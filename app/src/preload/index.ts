import { contextBridge, ipcRenderer } from "electron";

const ALLOWED_PUSH_CHANNELS = [
  "claude:stream",
  "claude:done",
  "claude:error",
  "job:update",
  "job:analyze_code",
] as const;

// Map<channel, Map<originalFn, wrapperFn>>
const wrapperRegistry = new Map<string, Map<Function, Function>>();

function getOrCreateChannelMap(channel: string): Map<Function, Function> {
  if (!wrapperRegistry.has(channel)) wrapperRegistry.set(channel, new Map());
  return wrapperRegistry.get(channel)!;
}

contextBridge.exposeInMainWorld("aidclaude", {
  // Settings
  settings: {
    get: () => ipcRenderer.invoke("settings:get"),
    set: (s: unknown) => ipcRenderer.invoke("settings:set", s),
  },
  // Data Source Catalog
  catalog: {
    list: () => ipcRenderer.invoke("catalog:list"),
    add: (ds: unknown) => ipcRenderer.invoke("catalog:add", ds),
    update: (ds: unknown) => ipcRenderer.invoke("catalog:update", ds),
    remove: (id: string) => ipcRenderer.invoke("catalog:remove", id),
    testConnection: (id: string) => ipcRenderer.invoke("catalog:testConnection", id),
    getSchema: (id: string) => ipcRenderer.invoke("catalog:getSchema", id),
    previewData: (id: string, limit?: number) => ipcRenderer.invoke("catalog:previewData", id, limit),
  },
  // Claude
  claude: {
    probe: () => ipcRenderer.invoke("claude:probe"),
    sendMessage: (jobId: string, message: string) =>
      ipcRenderer.invoke("claude:sendMessage", jobId, message),
    abort: (jobId: string) => ipcRenderer.invoke("claude:abort", jobId),
  },
  // Jobs
  jobs: {
    create: (userRequest: string, sourceIds: string[]) =>
      ipcRenderer.invoke("jobs:create", userRequest, sourceIds),
    list: () => ipcRenderer.invoke("jobs:list"),
    runAnalysis: (jobId: string) => ipcRenderer.invoke("jobs:runAnalysis", jobId),
    runSql: (jobId: string, sql: string) => ipcRenderer.invoke("jobs:runSql", jobId, sql),
    refreshSources: (jobId: string) => ipcRenderer.invoke("jobs:refreshSources", jobId),
  },
  // 임의 데이터를 CSV 소스로 저장
  data: {
    saveAsSource: (sourceName: string, headers: string[], rows: string[][]) =>
      ipcRenderer.invoke("data:saveAsSource", sourceName, headers, rows),
  },
  // SQLite DB browser
  db: {
    listTables: (jobId: string) => ipcRenderer.invoke("db:listTables", jobId),
    previewTable: (jobId: string, tableName: string, limit?: number) =>
      ipcRenderer.invoke("db:previewTable", jobId, tableName, limit),
    saveAsSource: (jobId: string, tableName: string, sourceName: string) =>
      ipcRenderer.invoke("db:saveAsSource", jobId, tableName, sourceName),
  },
  // File utilities
  files: {
    open: (fp: string) => ipcRenderer.invoke("files:open", fp),
    readText: (fp: string) => ipcRenderer.invoke("files:readText", fp),
    writeText: (fp: string, content: string) => ipcRenderer.invoke("files:writeText", fp, content),
    readLines: (fp: string, count: number) => ipcRenderer.invoke("files:readLines", fp, count),
    readBase64: (fp: string) => ipcRenderer.invoke("files:readBase64", fp),
    copyToData: (srcPath: string) => ipcRenderer.invoke("files:copyToData", srcPath),
  },
  // Export (native save dialog + write)
  export: {
    saveText: (defaultName: string, filters: { name: string; extensions: string[] }[], content: string) =>
      ipcRenderer.invoke("export:saveText", defaultName, filters, content),
    saveBinary: (defaultName: string, filters: { name: string; extensions: string[] }[], base64: string) =>
      ipcRenderer.invoke("export:saveBinary", defaultName, filters, base64),
  },
  // Native dialogs
  dialog: {
    openFile: (filters: { name: string; extensions: string[] }[]) =>
      ipcRenderer.invoke("dialog:openFile", filters),
  },
  // Events (main → renderer push) — allowlisted channels only, with leak-free wrapper registry
  on: (channel: string, fn: (...args: unknown[]) => void) => {
    if (!(ALLOWED_PUSH_CHANNELS as readonly string[]).includes(channel)) return;
    const wrapper = (_e: unknown, ...args: unknown[]) => fn(...args);
    getOrCreateChannelMap(channel).set(fn, wrapper);
    ipcRenderer.on(channel, wrapper as Parameters<typeof ipcRenderer.on>[1]);
  },
  off: (channel: string, fn: (...args: unknown[]) => void) => {
    if (!(ALLOWED_PUSH_CHANNELS as readonly string[]).includes(channel)) return;
    const channelMap = wrapperRegistry.get(channel);
    const wrapper = channelMap?.get(fn);
    if (wrapper) {
      ipcRenderer.removeListener(channel, wrapper as Parameters<typeof ipcRenderer.removeListener>[1]);
      channelMap!.delete(fn);
    }
  },
});
