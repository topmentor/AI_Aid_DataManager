import { app, ipcMain } from "electron";
import type { BrowserWindow } from "electron";
import { probeClaude } from "./claude-detector.js";
import {
  createSession,
  listSessions,
  sendMessage,
  abortSession,
} from "./claude-chat-service.js";

export interface RegisterClaudeChatOptions {
  claudeBin?: string;
  probeDir?: string;
}

export function registerClaudeChatIpc(
  win: BrowserWindow,
  options: RegisterClaudeChatOptions = {}
): void {
  const { claudeBin, probeDir } = options;

  ipcMain.handle("claude-chat:probe", () =>
    probeClaude({
      bin: claudeBin,
      cwd: probeDir ?? app.getPath("userData"),
    })
  );

  ipcMain.handle("claude-chat:createSession", (_e, opts?: { cwd?: string; label?: string }) =>
    createSession(opts)
  );

  ipcMain.handle("claude-chat:listSessions", () => listSessions());

  ipcMain.handle(
    "claude-chat:sendMessage",
    (_e, sessionId: string, message: string) =>
      sendMessage(win, sessionId, message, claudeBin)
  );

  ipcMain.handle("claude-chat:abort", (_e, sessionId: string) =>
    abortSession(sessionId)
  );
}
