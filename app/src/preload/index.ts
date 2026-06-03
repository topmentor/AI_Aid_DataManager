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

contextBridge.exposeInMainWorld("aidclaude", {
  // ── 데이터 (HTTP → SSF) ──
  settings: api.settings,
  catalog: api.catalog,
  jobs: api.jobs,
  data: api.data,
  db: api.db,
  files: {
    open: (fp: string) => ipcRenderer.invoke("files:open", fp), // 네이티브 열기는 IPC
    readText: api.filesHttp.readText,
    writeText: api.filesHttp.writeText,
    readLines: api.filesHttp.readLines,
    readBase64: api.filesHttp.readBase64,
    copyToData: api.filesHttp.copyToData,
    copyShapefile: api.filesHttp.copyShapefile,
  },
  // ── Agent 터미널 (claude/codex; HTTP+WS → SSF) ──
  agent: api.agent,

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
});
