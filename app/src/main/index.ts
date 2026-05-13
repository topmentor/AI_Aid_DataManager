import { app, BrowserWindow, ipcMain, shell, dialog } from "electron";
import path from "node:path";
import fs from "node:fs/promises";
import os from "node:os";
import { probeClaude } from "./services/claude-detector.js";
import { getSettings, setSettings } from "./services/settings-service.js";
import { listSources, addSource, updateSource, removeSource } from "./services/catalog-service.js";
import { inspectSchema, testConnection, previewData } from "./services/schema-inspector.js";
import type { AppSettings, DataSource } from "../shared/types.js";
import { loadJobs, createJob, getJob, updateJob, refreshJobSources, updateClaudeMd } from "./services/job-service.js";
import Database from "better-sqlite3";
import { sendMessage as claudeSendMessage, abortJob } from "./services/claude-service.js";

// Suppress harmless "Request Autofill.enable failed" DevTools Protocol noise
app.commandLine.appendSwitch("disable-features", "AutofillServerCommunication");

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
  } else {
    win.loadFile(path.join(__dirname, "../renderer/index.html"));
  }
}

app.whenReady().then(async () => {
  createWindow();
  // 앱 시작 시 기존 job들의 CLAUDE.md를 현재 DB 상태로 갱신 (백그라운드)
  loadJobs().then((jobs) => {
    for (const j of jobs) {
      updateClaudeMd(j.id).catch(() => {});
    }
  }).catch(() => {});
});
app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});

// ── IPC handlers (stubs — full implementations added in later tasks) ──

// Settings — real implementations (Task 4)
ipcMain.handle("settings:get", getSettings);
ipcMain.handle("settings:set", (_e, s: Partial<AppSettings>) => setSettings(s));

// Catalog — real implementations (Task 4)
ipcMain.handle("catalog:list", listSources);
ipcMain.handle("catalog:add", async (_e, ds: Omit<DataSource, "id">) => {
  const added = await addSource(ds);
  // 기존 모든 job의 data.db와 CLAUDE.md를 백그라운드로 갱신
  loadJobs().then((jobs) => {
    for (const j of jobs) {
      refreshJobSources(j.id).catch(() => {});
    }
  }).catch(() => {});
  return added;
});
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

ipcMain.handle("catalog:previewData", async (_e, id: string, limit?: number) => {
  const sources = await listSources();
  const ds = sources.find((s) => s.id === id);
  if (!ds) throw new Error(`Source not found: ${id}`);
  return previewData(ds, limit ?? 500);
});

// Claude (Task 3 + 8)
ipcMain.handle("claude:probe", () =>
  probeClaude({ cwd: app.getPath("userData") })
);
ipcMain.handle("claude:sendMessage", async (_e, jobId: string, msg: string) => {
  if (!win) throw new Error("Window not initialized");
  await claudeSendMessage(win, jobId, msg);
});
ipcMain.handle("claude:abort", (_e, jobId: string) => abortJob(jobId));

// Jobs (Task 7)
ipcMain.handle("jobs:list", loadJobs);
ipcMain.handle("jobs:create", async (_e, userRequest: string, sourceIds: string[]) => {
  const { job } = await createJob(userRequest, sourceIds);
  return job;
});
ipcMain.handle("jobs:refreshSources", (_e, jobId: string) => refreshJobSources(jobId));

// Manual SQL execution
ipcMain.handle("jobs:runAnalysis", async (_e, jobId: string) => {
  const job = getJob(jobId);
  if (!job) return { ok: false, error: `Job ${jobId} not found` };

  let sql: string;
  try {
    sql = await fs.readFile(path.join(job.workspaceDir, "query.sql"), "utf-8");
  } catch {
    return { ok: false, error: "query.sql 파일이 없습니다. AI에게 분석을 요청해 주세요." };
  }

  await updateJob(jobId, { status: "running" });
  win?.webContents.send("job:update", getJob(jobId));

  const dbPath = path.join(job.workspaceDir, "data.db");
  const db = new Database(dbPath);
  try {
    db.exec(sql);
    await updateJob(jobId, { status: "done" });
    win?.webContents.send("job:update", getJob(jobId));
    await updateClaudeMd(jobId).catch(() => {});
    return { ok: true };
  } catch (err) {
    const errMsg = (err as Error).message;
    await updateJob(jobId, { status: "error", errorMsg: errMsg });
    win?.webContents.send("job:update", getJob(jobId));
    return { ok: false, error: errMsg };
  } finally {
    db.close();
  }
});

// Run arbitrary SQL on a job's data.db (used for option selection)
ipcMain.handle("jobs:runSql", async (_e, jobId: string, sql: string) => {
  const job = getJob(jobId);
  if (!job) return { ok: false, error: `Job ${jobId} not found` };

  await updateJob(jobId, { status: "running" });
  win?.webContents.send("job:update", getJob(jobId));

  const dbPath = path.join(job.workspaceDir, "data.db");
  const db = new Database(dbPath);
  try {
    db.exec(sql);
    await updateJob(jobId, { status: "done" });
    win?.webContents.send("job:update", getJob(jobId));
    await updateClaudeMd(jobId).catch(() => {});
    return { ok: true };
  } catch (err) {
    const errMsg = (err as Error).message;
    await updateJob(jobId, { status: "error", errorMsg: errMsg });
    win?.webContents.send("job:update", getJob(jobId));
    return { ok: false, error: errMsg };
  } finally {
    db.close();
  }
});

// Export a DB table as CSV and register it as a new data source
ipcMain.handle(
  "db:saveAsSource",
  async (_e, jobId: string, tableName: string, sourceName: string) => {
    const job = getJob(jobId);
    if (!job) return { ok: false, error: "Job not found" };

    const dbPath = path.join(job.workspaceDir, "data.db");
    const db = new Database(dbPath, { readonly: true });
    try {
      const safe = tableName.replace(/"/g, '""');
      const rows = db.prepare(`SELECT * FROM "${safe}"`).all() as Record<string, unknown>[];
      if (rows.length === 0) return { ok: false, error: "테이블이 비어 있습니다" };

      const headers = Object.keys(rows[0]);

      // CSV 직렬화 (RFC 4180 준수)
      const escape = (v: unknown): string => {
        if (v == null) return "";
        const s = String(v);
        return /[,"\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
      };
      const csvContent =
        [headers.join(","), ...rows.map((r) => headers.map((h) => escape(r[h])).join(","))].join("\r\n");

      // workspaceRoot/data/ 에 CSV 저장
      const { workspaceRoot } = await getSettings();
      const dataDir = path.join(workspaceRoot, "data");
      await fs.mkdir(dataDir, { recursive: true });
      const safeName = sourceName.replace(/[^\w가-힣ㄱ-ㅎㅏ-ㅣ_-]/g, "_");
      const csvPath = path.join(dataDir, `${safeName}_${Date.now()}.csv`);
      // UTF-8 BOM 포함 → Excel에서 한글 깨짐 방지
      await fs.writeFile(csvPath, "﻿" + csvContent, "utf-8");

      // 카탈로그에 새 데이터 소스 등록
      const ds = await addSource({ name: sourceName, type: "csv", config: { filePath: csvPath } });
      return { ok: true, source: ds };
    } catch (e) {
      return { ok: false, error: (e as Error).message };
    } finally {
      db.close();
    }
  }
);

// 임의 데이터(headers+rows)를 CSV로 저장하고 카탈로그에 등록
ipcMain.handle(
  "data:saveAsSource",
  async (_e, sourceName: string, headers: string[], rows: string[][]) => {
    try {
      const escape = (v: string): string => {
        if (v == null) return "";
        return /[,"\n\r]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v;
      };
      const csvContent = [
        headers.join(","),
        ...rows.map((r) => r.map(escape).join(",")),
      ].join("\r\n");

      const { workspaceRoot } = await getSettings();
      const dataDir = path.join(workspaceRoot, "data");
      await fs.mkdir(dataDir, { recursive: true });
      const safeName = sourceName.replace(/[^\w가-힣ㄱ-ㅎㅏ-ㅣ_-]/g, "_");
      const csvPath = path.join(dataDir, `${safeName}_${Date.now()}.csv`);
      await fs.writeFile(csvPath, "﻿" + csvContent, "utf-8");

      const ds = await addSource({ name: sourceName, type: "csv", config: { filePath: csvPath } });
      return { ok: true, source: ds };
    } catch (e) {
      return { ok: false, error: (e as Error).message };
    }
  }
);

// SQLite DB browser
ipcMain.handle("db:listTables", async (_e, jobId: string) => {
  const job = getJob(jobId);
  if (!job) throw new Error(`Job ${jobId} not found`);
  const dbPath = path.join(job.workspaceDir, "data.db");
  const db = new Database(dbPath, { readonly: true });
  try {
    const rows = db.prepare(
      "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
    ).all() as { name: string }[];
    return rows.map((r) => r.name);
  } finally {
    db.close();
  }
});

ipcMain.handle("db:previewTable", async (_e, jobId: string, tableName: string, limit = 500) => {
  const job = getJob(jobId);
  if (!job) throw new Error(`Job ${jobId} not found`);
  const dbPath = path.join(job.workspaceDir, "data.db");
  const db = new Database(dbPath, { readonly: true });
  try {
    const safe = tableName.replace(/"/g, '""');
    const rows = db.prepare(`SELECT * FROM "${safe}" LIMIT ?`).all(limit) as Record<string, unknown>[];
    if (rows.length === 0) return { title: tableName, headers: [], rows: [] };
    const headers = Object.keys(rows[0]);
    const dataRows = rows.map((r) =>
      headers.map((h) => (r[h] == null ? "" : String(r[h])))
    );
    return { title: tableName, headers, rows: dataRows };
  } finally {
    db.close();
  }
});

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
  try {
    return await fs.readFile(resolved, "utf-8");
  } catch (e) {
    if ((e as NodeJS.ErrnoException).code === "ENOENT") return null;
    throw e;
  }
});
ipcMain.handle("files:writeText", async (_e, fp: string, content: string) => {
  const resolved = path.resolve(fp);
  const userDataPath = path.resolve(app.getPath("userData"));
  const isAllowed = resolved.startsWith(userDataPath + path.sep) || resolved === userDataPath;
  if (!isAllowed) throw new Error("Access denied: path outside userData");
  await fs.mkdir(path.dirname(resolved), { recursive: true });
  await fs.writeFile(resolved, content, "utf-8");
});
ipcMain.handle("files:readLines", async (_e, fp: string, count: number) => {
  const resolved = path.resolve(fp);
  const userDataPath = path.resolve(app.getPath("userData"));
  if (!resolved.startsWith(userDataPath + path.sep)) throw new Error("Access denied");
  const handle = await fs.open(resolved, "r");
  try {
    const buf = Buffer.alloc(8192);
    const { bytesRead } = await handle.read(buf, 0, 8192, 0);
    const content = buf.subarray(0, bytesRead).toString("utf-8");
    return content.split(/\r?\n/).slice(0, count);
  } finally {
    await handle.close();
  }
});
ipcMain.handle("files:readBase64", async (_e, fp: string) => {
  const userData = app.getPath("userData");
  const tmpDir = os.tmpdir();
  const normalized = path.normalize(fp);
  if (!normalized.startsWith(userData) && !normalized.startsWith(tmpDir)) {
    throw new Error("Access denied");
  }
  const buf = await fs.readFile(normalized);
  return buf.toString("base64");
});

// Export helpers — path chosen by native save dialog, no userData restriction needed
ipcMain.handle("export:saveText", async (
  _e,
  defaultName: string,
  filters: { name: string; extensions: string[] }[],
  content: string
) => {
  if (!win) return null;
  const result = await dialog.showSaveDialog(win, {
    defaultPath: defaultName,
    filters: [...filters, { name: "모든 파일", extensions: ["*"] }],
  });
  if (result.canceled || !result.filePath) return null;
  await fs.writeFile(result.filePath, content, "utf-8");
  return result.filePath;
});

ipcMain.handle("export:saveBinary", async (
  _e,
  defaultName: string,
  filters: { name: string; extensions: string[] }[],
  base64: string
) => {
  if (!win) return null;
  const result = await dialog.showSaveDialog(win, {
    defaultPath: defaultName,
    filters: [...filters, { name: "모든 파일", extensions: ["*"] }],
  });
  if (result.canceled || !result.filePath) return null;
  await fs.writeFile(result.filePath, Buffer.from(base64, "base64"));
  return result.filePath;
});

// File picker dialog — returns selected file path or null if cancelled
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

// Copy a file into workspaceRoot/data/ and return the destination path
ipcMain.handle("files:copyToData", async (_e, srcPath: string) => {
  const { workspaceRoot } = await getSettings();
  const dataDir = path.join(workspaceRoot, "data");
  await fs.mkdir(dataDir, { recursive: true });
  const ext = path.extname(srcPath);
  const base = path.basename(srcPath, ext).replace(/[^a-zA-Z0-9_\-]/g, "_");
  const destName = `${base}_${Date.now()}${ext}`;
  const destPath = path.join(dataDir, destName);
  await fs.copyFile(srcPath, destPath);
  return destPath;
});

// Copy shapefile set (.shp + .dbf + .shx + .prj + .cpg) to workspaceRoot/data/
// Returns the destination .shp path
ipcMain.handle("files:copyShapefile", async (_e, srcShpPath: string) => {
  const { workspaceRoot } = await getSettings();
  const dataDir = path.join(workspaceRoot, "data");
  await fs.mkdir(dataDir, { recursive: true });
  const srcDir = path.dirname(srcShpPath);
  const srcBase = path.basename(srcShpPath, ".shp");
  const timestamp = Date.now();
  const destBase = `${srcBase.replace(/[^a-zA-Z0-9_\-]/g, "_")}_${timestamp}`;
  const sidecarExts = [".shp", ".dbf", ".shx", ".prj", ".cpg"];
  for (const ext of sidecarExts) {
    const srcFile = path.join(srcDir, srcBase + ext);
    try {
      await fs.access(srcFile);
      await fs.copyFile(srcFile, path.join(dataDir, destBase + ext));
    } catch {
      // optional sidecar — skip if missing
    }
  }
  return path.join(dataDir, destBase + ".shp");
});
