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

  // Read system prompt from context.md
  let systemPrompt = "";
  try {
    const contextMd = await fs.readFile(
      path.join(job.workspaceDir, "context.md"),
      "utf-8"
    );
    // Extract just the system prompt portion (after "# System Prompt\n")
    const marker = "# System Prompt\n";
    const idx = contextMd.indexOf(marker);
    systemPrompt = idx >= 0 ? contextMd.slice(idx + marker.length) : contextMd;
  } catch {
    // context.md missing — proceed without system prompt
  }

  const ac = new AbortController();
  abortControllers.set(jobId, ac);

  await updateJob(jobId, { status: "planning" });

  try {
    let assistantBuffer = "";

    for await (const raw of queryClaude({
      prompt: message,
      cwd: job.workspaceDir,
      claudeBin,
      systemPromptAppend: systemPrompt || undefined,
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
      win.webContents.send("job:update", getJob(jobId));
    } catch {
      // analyze.py does not exist yet — Claude may still be writing it
      // Status stays at "planning", no error
    }
  } catch (err) {
    if ((err as Error).name === "AbortError") {
      await updateJob(jobId, { status: "idle" });
      return;
    }
    const msg = (err as Error).message;
    pushEvent(win, jobId, { type: "error", message: msg });
    await updateJob(jobId, { status: "error", errorMsg: msg });
  } finally {
    abortControllers.delete(jobId);
  }
}

export function abortJob(jobId: string): void {
  abortControllers.get(jobId)?.abort();
}
