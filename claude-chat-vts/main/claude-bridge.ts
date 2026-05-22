import { spawn } from "node:child_process";
import readline from "node:readline";

export interface ClaudeQueryOpts {
  prompt: string;
  cwd: string;
  claudeBin?: string;
  resumeSessionId?: string;
  systemPromptAppend?: string;
  allowedTools?: string[];
  permissionMode?: "default" | "acceptEdits" | "bypassPermissions";
  abortSignal?: AbortSignal;
}

export async function* queryClaude(opts: ClaudeQueryOpts): AsyncGenerator<unknown> {
  const {
    prompt,
    cwd,
    resumeSessionId,
    systemPromptAppend,
    allowedTools,
    permissionMode = "acceptEdits",
    abortSignal,
    claudeBin = process.env.CLAUDE_BIN ?? "claude",
  } = opts;

  const args = [
    "-p",
    "--output-format", "stream-json",
    "--verbose",
    "--model", "opus",
    "--permission-mode", permissionMode,
  ];
  if (allowedTools?.length) args.push("--allowed-tools", allowedTools.join(","));
  if (systemPromptAppend) args.push("--append-system-prompt", systemPromptAppend);
  if (resumeSessionId) args.push("--resume", resumeSessionId);
  args.push("--", prompt);

  const child = spawn(claudeBin, args, {
    cwd,
    stdio: ["ignore", "pipe", "pipe"],
    shell: process.platform === "win32",
  });

  let killed = false;
  const kill = (sig: "SIGTERM" | "SIGKILL" = "SIGTERM") => {
    if (killed || child.exitCode !== null) return;
    killed = true;
    child.kill(sig);
    setTimeout(() => {
      if (child.exitCode === null) child.kill("SIGKILL");
    }, 1000).unref();
  };

  if (abortSignal) {
    if (abortSignal.aborted) { kill(); }
    else abortSignal.addEventListener("abort", () => kill(), { once: true });
  }

  child.stderr.on("data", (b: Buffer) => process.stderr.write(b));

  const rl = readline.createInterface({ input: child.stdout, crlfDelay: Infinity });

  const exitPromise = new Promise<{ code: number | null; signal: string | null }>(
    (resolve, reject) => {
      child.on("error", reject);
      child.on("exit", (code, signal) => resolve({ code, signal }));
    }
  );

  try {
    for await (const line of rl) {
      if (!line.trim()) continue;
      try {
        yield JSON.parse(line);
      } catch {
        process.stderr.write(`[claude-bridge] non-JSON stdout: ${line}\n`);
      }
    }
  } finally {
    if (!killed && child.exitCode === null) kill();
    const { code, signal } = await exitPromise.catch(() => ({ code: null, signal: null }));
    if (abortSignal?.aborted) {
      const err = new Error("aborted");
      err.name = "AbortError";
      throw err;
    }
    if (code !== 0 && code !== null && !killed) {
      throw new Error(`claude exited with code=${code} signal=${signal}`);
    }
  }
}
