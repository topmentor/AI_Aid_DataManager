import path from "node:path";
import fs from "node:fs/promises";
import chokidar from "chokidar";
import Database from "better-sqlite3";
import type { BrowserWindow } from "electron";
import { queryClaude } from "./claude-bridge.js";
import { getSettings } from "./settings-service.js";
import { getJob, updateJob, refreshJobSources, updateClaudeMd } from "./job-service.js";

// One AbortController per active job
const abortControllers = new Map<string, AbortController>();

export function parseSqlOptions(sql: string): { title: string; sql: string }[] | null {
  const lines = sql.split("\n");
  const options: { title: string; sql: string }[] = [];
  let currentTitle = "";
  let currentLines: string[] = [];
  for (const line of lines) {
    const match = line.match(/^--\s*\[옵션\s*\d+\]\s*(.*)/);
    if (match) {
      if (currentTitle) {
        const s = currentLines.join("\n").trim();
        if (s) options.push({ title: currentTitle, sql: s });
      }
      currentTitle = match[1].trim() || `옵션 ${options.length + 1}`;
      currentLines = [];
    } else if (currentTitle) {
      currentLines.push(line);
    }
  }
  if (currentTitle) {
    const s = currentLines.join("\n").trim();
    if (s) options.push({ title: currentTitle, sql: s });
  }
  return options.length >= 2 ? options : null;
}

function pushJobUpdate(win: BrowserWindow, job: import("../../shared/types.js").Job | undefined): void {
  if (!job) return;
  win.webContents.send("job:update", job);
}

export async function sendMessage(
  win: BrowserWindow,
  jobId: string,
  message: string
): Promise<void> {
  const { claudeBin } = await getSettings();
  const job = getJob(jobId);
  if (!job) {
    throw new Error(`Job ${jobId} not found`);
  }

  if (abortControllers.has(jobId)) {
    throw new Error(`Job ${jobId} is already running`);
  }

  const ac = new AbortController();
  abortControllers.set(jobId, ac);

  await updateJob(jobId, { status: "planning" });

  // 매 메시지 전, 현재 카탈로그 소스를 data.db에 반영하고 CLAUDE.md를 최신 스키마로 재생성
  await refreshJobSources(jobId);

  // 사용자 메시지를 파일로 저장: Windows cmd.exe 한국어 인코딩 문제 방지
  // Claude는 파일에서 요청을 읽으므로 인코딩 손상 없이 전달됨
  await fs.writeFile(path.join(job.workspaceDir, "request.md"), message, "utf-8");

  // ── query.sql 파일 감시: 생성/변경 즉시 CodePanel에 전달 ──
  const analysisPath = path.join(job.workspaceDir, "query.sql");
  const watcher = chokidar.watch(analysisPath, {
    persistent: false,
    ignoreInitial: false,
    usePolling: true,      // Windows 파일 시스템에서 신뢰성 보장
    interval: 300,
    awaitWriteFinish: { stabilityThreshold: 150, pollInterval: 100 },
  });

  const sendCode = async () => {
    try {
      const code = await fs.readFile(analysisPath, "utf-8");
      win.webContents.send("job:analyze_code", { jobId, code });
    } catch {
      // 파일이 아직 없으면 무시
    }
  };

  watcher.on("add", sendCode);
  watcher.on("change", sendCode);

  try {
    // CLI 인수는 짧은 ASCII만 사용 (Windows cmd.exe 인코딩 안전)
    // 실제 사용자 요청은 request.md에서 읽음
    for await (const raw of queryClaude({
      prompt: "Read request.md and write query.sql for the user request.",
      cwd: job.workspaceDir,
      claudeBin,
      allowedTools: ["Read", "Edit", "Write"],
      permissionMode: "acceptEdits",
      abortSignal: ac.signal,
    })) {
      win.webContents.send("claude:stream", { jobId, event: raw });
    }

    win.webContents.send("claude:done", { jobId });

    // Claude 턴 완료 후 query.sql 최종본 확인 및 자동 실행
    try {
      const sql = await fs.readFile(analysisPath, "utf-8");

      // watcher가 마지막 변경을 놓쳤을 경우를 대비해 최종본 재전송
      win.webContents.send("job:analyze_code", { jobId, code: sql });

      // 옵션이 2개 이상이면 사용자가 선택하도록 자동 실행 건너뜀
      const options = parseSqlOptions(sql);
      if (options) {
        await updateJob(jobId, { status: "idle" });
        pushJobUpdate(win, getJob(jobId));
      } else {
        // 단일 쿼리 — 즉시 실행
        await updateJob(jobId, { status: "running" });
        pushJobUpdate(win, getJob(jobId));

        const dbPath = path.join(job.workspaceDir, "data.db");
        const db = new Database(dbPath);
        try {
          db.exec(sql);
          await updateJob(jobId, { status: "done" });
        } catch (sqlErr) {
          const errMsg = (sqlErr as Error).message;
          await updateJob(jobId, { status: "error", errorMsg: errMsg });
          win.webContents.send("claude:error", { jobId, error: errMsg });
        } finally {
          db.close();
        }
        // SQL 실행 후 새 테이블(result 등)을 CLAUDE.md에 반영
        await updateClaudeMd(jobId).catch(() => {});

        pushJobUpdate(win, getJob(jobId));
      }
    } catch (readErr) {
      const code = (readErr as NodeJS.ErrnoException).code;
      if (code !== "ENOENT") {
        const msg = (readErr as Error).message;
        win.webContents.send("claude:error", { jobId, error: `query.sql error: ${msg}` });
        await updateJob(jobId, { status: "error", errorMsg: msg });
        pushJobUpdate(win, getJob(jobId));
      }
      // ENOENT: query.sql 없음 — 정상 (텍스트만 응답한 경우)
    }
  } catch (err) {
    if ((err as Error).name === "AbortError") {
      await updateJob(jobId, { status: "idle" });
      pushJobUpdate(win, getJob(jobId));
      return;
    }
    const msg = (err as Error).message;
    win.webContents.send("claude:error", { jobId, error: msg });
    await updateJob(jobId, { status: "error", errorMsg: msg });
  } finally {
    await watcher.close();
    abortControllers.delete(jobId);
  }
}

export function abortJob(jobId: string): void {
  abortControllers.get(jobId)?.abort();
}
