import { spawn } from "node:child_process";
import { queryClaude } from "./claude-bridge.js";
import type { ClaudeEnvProbe } from "../../shared/types.js";

function getVersion(bin: string): Promise<string | null> {
  return new Promise((resolve) => {
    try {
      const child = spawn(bin, ["--version"], {
        stdio: ["ignore", "pipe", "pipe"],
        shell: process.platform === "win32",
      });
      let out = "";
      child.stdout.on("data", (b: Buffer) => (out += b.toString()));
      child.on("error", () => resolve(null));
      child.on("exit", (code) => resolve(code === 0 ? out.trim() : null));
    } catch {
      resolve(null);
    }
  });
}

function resolveBinaryPath(bin: string): Promise<string | null> {
  return new Promise((resolve) => {
    const cmd = process.platform === "win32" ? "where" : "which";
    const args = [bin];
    try {
      const child = spawn(cmd, args, {
        stdio: ["ignore", "pipe", "ignore"],
        shell: true,
      });
      let out = "";
      child.stdout.on("data", (b: Buffer) => (out += b.toString()));
      child.on("error", () => resolve(null));
      child.on("exit", (code) => {
        const first = out.split(/\r?\n/).find((l) => l.trim().length > 0);
        resolve(code === 0 && first ? first.trim() : null);
      });
    } catch {
      resolve(null);
    }
  });
}

export async function probeClaude(
  options: { bin?: string; cwd: string } = { cwd: process.cwd() }
): Promise<ClaudeEnvProbe> {
  const bin = options.bin ?? process.env.CLAUDE_BIN ?? "claude";

  const [binaryPath, version] = await Promise.all([
    resolveBinaryPath(bin),
    getVersion(bin),
  ]);

  if (!version) {
    return {
      binaryPath,
      version: null,
      authenticated: false,
      roundTripMs: null,
      error:
        "Claude Code CLI를 찾지 못했습니다. https://docs.claude.com/claude-code 에서 설치 후 `claude login`을 완료해주세요.",
    };
  }

  // Round-trip probe: minimal prompt to verify authentication
  const startedAt = Date.now();
  const ac = new AbortController();
  const timer = setTimeout(() => ac.abort(), 30_000);
  timer.unref();

  try {
    let sawResult = false;
    for await (const event of queryClaude({
      prompt: "Reply with just: ok",
      cwd: options.cwd,
      claudeBin: bin,
      allowedTools: [],
      permissionMode: "default",
      abortSignal: ac.signal,
    })) {
      const ev = event as { type?: string };
      if (ev.type === "result") {
        sawResult = true;
        break;
      }
    }
    clearTimeout(timer);
    return {
      binaryPath,
      version,
      authenticated: sawResult,
      roundTripMs: sawResult ? Date.now() - startedAt : null,
      error: sawResult ? null : "Claude Code로부터 응답을 받지 못했습니다.",
    };
  } catch (err) {
    clearTimeout(timer);
    const message = err instanceof Error ? err.message : String(err);
    return {
      binaryPath,
      version,
      authenticated: false,
      roundTripMs: null,
      error: ac.signal.aborted
        ? "Claude Code 응답이 30초 내에 오지 않았습니다."
        : `Claude Code 호출 실패: ${message}`,
    };
  }
}
