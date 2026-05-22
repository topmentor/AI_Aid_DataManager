import os from "node:os";
import path from "node:path";
import fs from "node:fs/promises";
import type { BrowserWindow } from "electron";
import { queryClaude } from "./claude-bridge.js";
import type { ChatSession } from "../shared/chat-types.js";

// Active sessions in memory
const sessions = new Map<string, ChatSession>();

// One AbortController per active session
const abortControllers = new Map<string, AbortController>();

function defaultCwd(): string {
  return path.join(os.tmpdir(), "claude-chat-sessions");
}

export async function createSession(
  options: { cwd?: string; label?: string } = {}
): Promise<ChatSession> {
  const id = crypto.randomUUID();
  const baseDir = options.cwd ?? defaultCwd();
  const sessionDir = path.join(baseDir, `session_${id}`);
  await fs.mkdir(sessionDir, { recursive: true });

  const session: ChatSession = {
    id,
    cwd: sessionDir,
    label: options.label ?? `세션 ${new Date().toLocaleTimeString("ko-KR")}`,
    createdAt: new Date().toISOString(),
  };
  sessions.set(id, session);
  return session;
}

export function listSessions(): ChatSession[] {
  return [...sessions.values()];
}

export async function sendMessage(
  win: BrowserWindow,
  sessionId: string,
  message: string,
  claudeBin?: string
): Promise<void> {
  const session = sessions.get(sessionId);
  if (!session) throw new Error(`Session ${sessionId} not found`);

  if (abortControllers.has(sessionId)) {
    throw new Error(`Session ${sessionId} is already running`);
  }

  const ac = new AbortController();
  abortControllers.set(sessionId, ac);

  // Write message to request.md for Windows encoding safety
  await fs.writeFile(path.join(session.cwd, "request.md"), message, "utf-8");

  const bin = claudeBin ?? process.env.CLAUDE_BIN ?? "claude";

  try {
    for await (const raw of queryClaude({
      prompt: "Read request.md and respond to the user request.",
      cwd: session.cwd,
      claudeBin: bin,
      allowedTools: ["Read", "Edit", "Write"],
      permissionMode: "acceptEdits",
      abortSignal: ac.signal,
    })) {
      win.webContents.send(`claude-chat:stream:${sessionId}`, raw);
    }
    win.webContents.send(`claude-chat:done:${sessionId}`);
  } catch (err) {
    if ((err as Error).name === "AbortError") {
      win.webContents.send(`claude-chat:done:${sessionId}`);
      return;
    }
    win.webContents.send(`claude-chat:error:${sessionId}`, {
      message: (err as Error).message,
    });
  } finally {
    abortControllers.delete(sessionId);
  }
}

export function abortSession(sessionId: string): void {
  abortControllers.get(sessionId)?.abort();
}
