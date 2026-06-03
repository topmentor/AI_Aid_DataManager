import { contextBridge, ipcRenderer } from "electron";
import { createApiClient } from "./api-client.js";

// SSF 서버 URL 해석: (1) main에 동기 IPC 질의(sandbox 안전) → (2) additionalArguments → (3) 기본값
function readServerUrl(): string {
  try {
    const url = ipcRenderer.sendSync("aidclaude:server-url");
    if (typeof url === "string" && url) return url;
  } catch {
    /* fall through */
  }
  const arg = process.argv.find((a) => a.startsWith("--server-url="));
  return arg ? arg.slice("--server-url=".length) : "http://localhost:8765/AidClaude";
}
const api = createApiClient(readServerUrl());

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
  // ── 데이터 (HTTP → SSF) ──
  settings: api.settings,
  catalog: api.catalog,
  jobs: api.jobs,
  data: api.data,
  db: api.db,
  files: {
    // 네이티브 열기는 IPC, 나머지는 HTTP
    open: (fp: string) => ipcRenderer.invoke("files:open", fp),
    readText: api.filesHttp.readText,
    writeText: api.filesHttp.writeText,
    readLines: api.filesHttp.readLines,
    readBase64: api.filesHttp.readBase64,
    copyToData: api.filesHttp.copyToData,
    copyShapefile: api.filesHttp.copyShapefile,
  },

  // ── Claude (IPC, Electron 잔류) ──
  claude: {
    probe: () => ipcRenderer.invoke("claude:probe"),
    sendMessage: (jobId: string, message: string) =>
      ipcRenderer.invoke("claude:sendMessage", jobId, message),
    abort: (jobId: string) => ipcRenderer.invoke("claude:abort", jobId),
  },

  // ── 네이티브 다이얼로그/내보내기 (IPC) ──
  export: {
    saveText: (defaultName: string, filters: { name: string; extensions: string[] }[], content: string) =>
      ipcRenderer.invoke("export:saveText", defaultName, filters, content),
    saveBinary: (defaultName: string, filters: { name: string; extensions: string[] }[], base64: string) =>
      ipcRenderer.invoke("export:saveBinary", defaultName, filters, base64),
  },
  dialog: {
    openFile: (filters: { name: string; extensions: string[] }[]) =>
      ipcRenderer.invoke("dialog:openFile", filters),
  },

  // ── 이벤트 (main → renderer push) ──
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
