import path from "node:path";
import fs from "node:fs/promises";
import chokidar from "chokidar";
import type { BrowserWindow } from "electron";
import { queryClaude } from "./claude-bridge.js";
import { createSsfClient, type SsfJob } from "./ssf-client.js";
import type { Job, JobStatus } from "../../shared/types.js";

// 활성 job당 AbortController 하나
const abortControllers = new Map<string, AbortController>();

/** SSF job + 상태를 renderer Job 형태로 push. */
function pushJobUpdate(win: BrowserWindow, job: SsfJob, status: JobStatus, errorMsg?: string): void {
  const mapped: Job = {
    id: job.id,
    createdAt: String(job.createdAt ?? ""),
    userRequest: job.userRequest ?? "",
    status,
    workspaceDir: job.workspaceDir,
    outputFiles: [],
    errorMsg,
  };
  win.webContents.send("job:update", mapped);
}

export async function sendMessage(
  win: BrowserWindow,
  serverUrl: string,
  jobId: string,
  message: string
): Promise<void> {
  const ssf = createSsfClient(serverUrl);
  const settings = await ssf.getSettings();
  const claudeBin = settings.claudeBin || "claude";

  const job = await ssf.getJob(jobId);
  if (!job) throw new Error(`Job ${jobId} not found`);

  if (abortControllers.has(jobId)) {
    throw new Error(`Job ${jobId} is already running`);
  }
  const ac = new AbortController();
  abortControllers.set(jobId, ac);

  pushJobUpdate(win, job, "planning");

  // 턴 전: 현재 카탈로그 소스를 data.db에 반영 + CLAUDE.md 갱신
  await ssf.refreshJobSources(jobId).catch(() => {});

  // 사용자 메시지를 파일로 저장(Windows 인코딩 문제 방지) — Claude가 request.md에서 읽음
  await fs.writeFile(path.join(job.workspaceDir, "request.md"), message, "utf-8");

  // query.sql 파일 감시 → 생성/변경 즉시 CodePanel로 전달
  const analysisPath = path.join(job.workspaceDir, "query.sql");
  const watcher = chokidar.watch(analysisPath, {
    persistent: false,
    ignoreInitial: false,
    usePolling: true,
    interval: 300,
    awaitWriteFinish: { stabilityThreshold: 150, pollInterval: 100 },
  });
  const sendCode = async () => {
    try {
      const code = await fs.readFile(analysisPath, "utf-8");
      win.webContents.send("job:analyze_code", { jobId, code });
    } catch {
      /* 파일 미존재 무시 */
    }
  };
  watcher.on("add", sendCode);
  watcher.on("change", sendCode);

  try {
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

    // 최종 query.sql 재전송 + 옵션 판정
    let sql: string | null = null;
    try {
      sql = await fs.readFile(analysisPath, "utf-8");
      win.webContents.send("job:analyze_code", { jobId, code: sql });
    } catch (e) {
      if ((e as NodeJS.ErrnoException).code !== "ENOENT") throw e;
    }

    if (sql == null) {
      // query.sql 없음 — 텍스트만 응답
      pushJobUpdate(win, job, "idle");
      return;
    }

    const options = await ssf.getSqlOptions(jobId).catch(() => []);
    if (options.length >= 2) {
      // 옵션 2개+ → 사용자 선택 대기 (자동 실행 안 함)
      pushJobUpdate(win, job, "idle");
    } else {
      // 단일 쿼리 → SSF가 즉시 실행(백업 포함)
      pushJobUpdate(win, job, "running");
      const r = await ssf.runJobAnalysis(jobId);
      if (r.ok) {
        pushJobUpdate(win, job, "done");
      } else {
        win.webContents.send("claude:error", { jobId, error: r.error ?? "실행 실패" });
        pushJobUpdate(win, job, "error", r.error);
      }
    }
  } catch (err) {
    if ((err as Error).name === "AbortError") {
      pushJobUpdate(win, job, "idle");
      return;
    }
    const msg = (err as Error).message;
    win.webContents.send("claude:error", { jobId, error: msg });
    pushJobUpdate(win, job, "error", msg);
  } finally {
    await watcher.close();
    abortControllers.delete(jobId);
  }
}

export function abortJob(jobId: string): void {
  abortControllers.get(jobId)?.abort();
}
