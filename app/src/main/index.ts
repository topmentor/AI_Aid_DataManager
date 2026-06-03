import { app, BrowserWindow, ipcMain, shell, dialog } from "electron";
import path from "node:path";
import fs from "node:fs/promises";
import { probeClaude } from "./services/claude-detector.js";
import { sendMessage as claudeSendMessage, abortJob } from "./services/claude-service.js";
import { launchServer, resolvePaths, type ServerHandle } from "./services/server-launcher.js";

// Suppress harmless "Request Autofill.enable failed" DevTools Protocol noise
app.commandLine.appendSwitch("disable-features", "AutofillServerCommunication");

let win: BrowserWindow | null = null;
let server: ServerHandle | null = null;

function isDev(): boolean {
  return !!process.env.ELECTRON_RENDERER_URL;
}

function dataHome(): string {
  return path.join(app.getPath("userData"), "aidclaude");
}

async function startBackend(): Promise<ServerHandle> {
  const projectRoot = path.resolve(__dirname, "../../.."); // app/out/main → repo root
  const { jar, web } = resolvePaths({
    isDev: isDev(),
    resourcesPath: process.resourcesPath,
    projectRoot,
  });
  return launchServer({ jar, web, home: dataHome() });
}

function createWindow(serverUrl: string) {
  win = new BrowserWindow({
    width: 1400,
    height: 900,
    webPreferences: {
      preload: path.join(__dirname, "../preload/index.js"),
      contextIsolation: true,
      nodeIntegration: false,
      additionalArguments: [`--server-url=${serverUrl}`],
    },
  });

  if (process.env.ELECTRON_RENDERER_URL) {
    win.loadURL(process.env.ELECTRON_RENDERER_URL);
  } else {
    win.loadFile(path.join(__dirname, "../renderer/index.html"));
  }
}

app.whenReady().then(async () => {
  try {
    server = await startBackend();
    console.log(`[main] SSF backend ready: ${server.url}`);
  } catch (e) {
    console.error("[main] SSF 백엔드 기동 실패:", e);
    dialog.showErrorBox(
      "백엔드 기동 실패",
      `SSF 서버를 시작하지 못했습니다.\n\n${(e as Error).message}\n\n` +
        `Java 17+ 설치 및 server/build.ps1로 jar 빌드를 확인하세요.`
    );
    app.quit();
    return;
  }
  createWindow(server.url);
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});

app.on("before-quit", () => {
  if (server) {
    try { server.proc.kill(); } catch { /* noop */ }
    server = null;
  }
});

// ── Claude (Electron 잔류 — SSF에 HTTP 위임) ──
ipcMain.handle("claude:probe", () => probeClaude({ cwd: dataHome() }));
ipcMain.handle("claude:sendMessage", async (_e, jobId: string, msg: string) => {
  if (!win) throw new Error("Window not initialized");
  if (!server) throw new Error("Server not ready");
  await claudeSendMessage(win, server.url, jobId, msg);
});
ipcMain.handle("claude:abort", (_e, jobId: string) => abortJob(jobId));

// ── 네이티브 다이얼로그/내보내기 (Electron 전용) ──
ipcMain.handle("files:open", (_e, fp: string) => shell.openPath(fp));

ipcMain.handle(
  "export:saveText",
  async (_e, defaultName: string, filters: { name: string; extensions: string[] }[], content: string) => {
    if (!win) return null;
    const result = await dialog.showSaveDialog(win, {
      defaultPath: defaultName,
      filters: [...filters, { name: "모든 파일", extensions: ["*"] }],
    });
    if (result.canceled || !result.filePath) return null;
    await fs.writeFile(result.filePath, content, "utf-8");
    return result.filePath;
  }
);

ipcMain.handle(
  "export:saveBinary",
  async (_e, defaultName: string, filters: { name: string; extensions: string[] }[], base64: string) => {
    if (!win) return null;
    const result = await dialog.showSaveDialog(win, {
      defaultPath: defaultName,
      filters: [...filters, { name: "모든 파일", extensions: ["*"] }],
    });
    if (result.canceled || !result.filePath) return null;
    await fs.writeFile(result.filePath, Buffer.from(base64, "base64"));
    return result.filePath;
  }
);

ipcMain.handle(
  "dialog:openFile",
  async (_e, filters: { name: string; extensions: string[] }[]) => {
    if (!win) return null;
    const result = await dialog.showOpenDialog(win, {
      properties: ["openFile"],
      filters: [...filters, { name: "모든 파일", extensions: ["*"] }],
    });
    return result.canceled ? null : (result.filePaths[0] ?? null);
  }
);
