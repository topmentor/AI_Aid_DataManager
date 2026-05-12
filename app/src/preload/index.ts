import { contextBridge, ipcRenderer } from "electron";

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
  },
  // File utilities
  files: {
    open: (fp: string) => ipcRenderer.invoke("files:open", fp),
    readText: (fp: string) => ipcRenderer.invoke("files:readText", fp),
  },
  // Events (main → renderer push)
  on: (channel: string, fn: (...args: unknown[]) => void) => {
    ipcRenderer.on(channel, (_e, ...args) => fn(...args));
  },
  off: (channel: string, fn: (...args: unknown[]) => void) => {
    ipcRenderer.removeListener(channel, fn);
  },
});
