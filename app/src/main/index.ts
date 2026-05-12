import { app, BrowserWindow, ipcMain, shell } from "electron";
import path from "node:path";
import fs from "node:fs/promises";

let win: BrowserWindow | null = null;

function createWindow() {
  win = new BrowserWindow({
    width: 1400,
    height: 900,
    webPreferences: {
      preload: path.join(__dirname, "../preload/index.js"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  if (process.env.ELECTRON_RENDERER_URL) {
    win.loadURL(process.env.ELECTRON_RENDERER_URL);
    win.webContents.openDevTools();
  } else {
    win.loadFile(path.join(__dirname, "../renderer/index.html"));
  }
}

app.whenReady().then(createWindow);
app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});

// ── IPC handlers (stubs — full implementations added in later tasks) ──

// Settings (stub — implemented in Task 4)
ipcMain.handle("settings:get", () => ({
  claudeBin: "claude",
  pythonBin: "python",
  workspaceRoot: path.join(app.getPath("userData"), "workspace"),
}));
ipcMain.handle("settings:set", () => {});

// Catalog (stub — implemented in Task 4)
ipcMain.handle("catalog:list", () => []);
ipcMain.handle("catalog:add", () => { throw new Error("Not yet implemented"); });
ipcMain.handle("catalog:update", () => { throw new Error("Not yet implemented"); });
ipcMain.handle("catalog:remove", () => { throw new Error("Not yet implemented"); });
ipcMain.handle("catalog:testConnection", () => ({ ok: false, error: "Not yet implemented" }));
ipcMain.handle("catalog:getSchema", () => { throw new Error("Not yet implemented"); });

// Claude (stub — implemented in Task 3 + 8)
ipcMain.handle("claude:probe", () => ({
  binaryPath: null, version: null, authenticated: false,
  roundTripMs: null, error: "Claude service not yet initialized",
}));
ipcMain.handle("claude:sendMessage", () => {});
ipcMain.handle("claude:abort", () => {});

// Jobs (stub — implemented in Task 7)
ipcMain.handle("jobs:list", () => []);
ipcMain.handle("jobs:create", () => { throw new Error("Not yet implemented"); });

// Files
ipcMain.handle("files:open", (_e, fp: string) => shell.openPath(fp));
ipcMain.handle("files:readText", (_e, fp: string) => fs.readFile(fp, "utf-8"));
