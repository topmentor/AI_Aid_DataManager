import { app, BrowserWindow, ipcMain, shell } from "electron";
import path from "node:path";
import fs from "node:fs/promises";
import { probeClaude } from "./services/claude-detector.js";
import { getSettings, setSettings } from "./services/settings-service.js";
import { listSources, addSource, updateSource, removeSource } from "./services/catalog-service.js";
import { inspectSchema, testConnection } from "./services/schema-inspector.js";
import type { AppSettings, DataSource } from "../shared/types.js";

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

// Settings — real implementations (Task 4)
ipcMain.handle("settings:get", getSettings);
ipcMain.handle("settings:set", (_e, s: Partial<AppSettings>) => setSettings(s));

// Catalog — real implementations (Task 4)
ipcMain.handle("catalog:list", listSources);
ipcMain.handle("catalog:add", (_e, ds: Omit<DataSource, "id">) => addSource(ds));
ipcMain.handle("catalog:update", (_e, ds: DataSource) => updateSource(ds));
ipcMain.handle("catalog:remove", (_e, id: string) => removeSource(id));
ipcMain.handle("catalog:testConnection", async (_e, id: string) => {
  const sources = await listSources();
  const ds = sources.find((s) => s.id === id);
  if (!ds) return { ok: false, error: "Source not found" };
  return testConnection(ds);
});

ipcMain.handle("catalog:getSchema", async (_e, id: string) => {
  const sources = await listSources();
  const ds = sources.find((s) => s.id === id);
  if (!ds) throw new Error(`Source not found: ${id}`);
  return inspectSchema(ds);
});

// Claude (stub — implemented in Task 3 + 8)
ipcMain.handle("claude:probe", () =>
  probeClaude({ cwd: app.getPath("userData") })
);
ipcMain.handle("claude:sendMessage", () => {});
ipcMain.handle("claude:abort", () => {});

// Jobs (stub — implemented in Task 7)
ipcMain.handle("jobs:list", () => []);
ipcMain.handle("jobs:create", () => { throw new Error("Not yet implemented"); });

// Files
ipcMain.handle("files:open", (_e, fp: string) => shell.openPath(fp));
ipcMain.handle("files:readText", async (_e, fp: string) => {
  const resolved = path.resolve(fp);
  const userDataPath = path.resolve(app.getPath("userData"));
  const tempPath = path.resolve(app.getPath("temp"));
  const allowed = [userDataPath, tempPath];
  const isAllowed = allowed.some(dir => resolved.startsWith(dir + path.sep) || resolved === dir);
  if (!isAllowed) {
    throw new Error("Access denied: path outside allowed directories");
  }
  return fs.readFile(resolved, "utf-8");
});
