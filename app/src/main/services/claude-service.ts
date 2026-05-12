import path from "node:path";
import fs from "node:fs/promises";
import type { BrowserWindow } from "electron";
import { queryClaude } from "./claude-bridge.js";
import { getSettings } from "./settings-service.js";
import { getJob, updateJob } from "./job-service.js";
import { validatePython } from "./ast-validator.js";
import { runAnalysis } from "./python-runner.js";
import type { ClaudeStreamEvent } from "../../shared/types.js";

// One AbortController per active job
const abortControllers = new Map<string, AbortController>();

function pushJobUpdate(win: BrowserWindow, job: import("../../shared/types.js").Job | undefined): void {
  if (!job) return;
  win.webContents.send("job:update", job);
}

function pushEvent(
  win: BrowserWindow,
  jobId: string,
  event: ClaudeStreamEvent
): void {
  // Channel name: "claude:stream" with jobId as payload field
  // Renderer listens on the allowed channel "claude:stream"
  win.webContents.send("claude:stream", { jobId, event });
}

export async function sendMessage(
  win: BrowserWindow,
  jobId: string,
  message: string
): Promise<void> {
  const { claudeBin, pythonBin } = await getSettings();
  const job = getJob(jobId);
  if (!job) {
    throw new Error(`Job ${jobId} not found`);
  }

  // Prevent double-send for same job
  if (abortControllers.has(jobId)) {
    throw new Error(`Job ${jobId} is already running`);
  }

  const ac = new AbortController();
  abortControllers.set(jobId, ac);

  await updateJob(jobId, { status: "planning" });

  try {
    let assistantBuffer = "";

    // CLAUDE.md in workspaceDir is read automatically by Claude Code — no --append-system-prompt needed
    for await (const raw of queryClaude({
      prompt: message,
      cwd: job.workspaceDir,
      claudeBin,
      allowedTools: ["Read", "Edit", "Write"],
      permissionMode: "acceptEdits",
      abortSignal: ac.signal,
    })) {
      const ev = raw as Record<string, unknown>;

      if (ev.type === "assistant") {
        // Extract text from assistant message content array
        const content = (ev.message as { content?: { type: string; text?: string }[] })?.content ?? [];
        for (const block of content) {
          if (block.type === "text" && block.text) {
            assistantBuffer += block.text;
            pushEvent(win, jobId, { type: "assistant", text: block.text });
          }
        }
      } else if (ev.type === "result") {
        // Session ID from Claude
        const sessionId = (ev.session_id as string) ?? "";
        const subtype = (ev.subtype as string) ?? "";
        const resultText = assistantBuffer;
        pushEvent(win, jobId, { type: "result", sessionId, subtype, resultText });
      }
    }

    // Notify renderer that the Claude turn is complete
    win.webContents.send("claude:done", { jobId });

    // Turn complete — check for analyze.py and auto-run if present
    const analysisPath = path.join(job.workspaceDir, "analyze.py");
    try {
      const code = await fs.readFile(analysisPath, "utf-8");

      // Read the code and push it to renderer for CodePanel
      win.webContents.send("job:analyze_code", { jobId, code });

      // AST validation
      const validation = await validatePython(code, pythonBin);
      if (!validation.ok) {
        const errMsg = `AST validation failed:\n${validation.errors.join("\n")}`;
        await updateJob(jobId, { status: "error", errorMsg: errMsg });
        pushEvent(win, jobId, { type: "error", message: errMsg });
        pushJobUpdate(win, getJob(jobId));
        return;
      }

      // Run analysis
      await updateJob(jobId, { status: "running" });
      const result = await runAnalysis(job.workspaceDir, pythonBin);

      if (result.exitCode === 0) {
        await updateJob(jobId, { status: "done", outputFiles: result.outputFiles });
      } else {
        const errMsg =
          result.stderr.slice(0, 1000) ||
          `Process exited with code ${result.exitCode}`;
        await updateJob(jobId, {
          status: "error",
          errorMsg: errMsg,
          outputFiles: result.outputFiles,
        });
        pushEvent(win, jobId, { type: "error", message: errMsg });
      }

      // Push updated job to renderer
      pushJobUpdate(win, getJob(jobId));
    } catch (readErr) {
      const code = (readErr as NodeJS.ErrnoException).code;
      if (code !== "ENOENT") {
        const msg = (readErr as Error).message;
        pushEvent(win, jobId, { type: "error", message: `analyze.py error: ${msg}` });
        await updateJob(jobId, { status: "error", errorMsg: msg });
        pushJobUpdate(win, getJob(jobId));
      }
      // ENOENT = analyze.py doesn't exist yet, that's OK
    }
  } catch (err) {
    if ((err as Error).name === "AbortError") {
      await updateJob(jobId, { status: "idle" });
      pushJobUpdate(win, getJob(jobId));
      return;
    }
    const msg = (err as Error).message;
    pushEvent(win, jobId, { type: "error", message: msg });
    win.webContents.send("claude:error", { jobId, error: msg });
    await updateJob(jobId, { status: "error", errorMsg: msg });
  } finally {
    abortControllers.delete(jobId);
  }
}

export function abortJob(jobId: string): void {
  abortControllers.get(jobId)?.abort();
}
