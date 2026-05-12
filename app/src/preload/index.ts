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
    readBase64: (fp: string) => ipcRenderer.invoke("files:readBase64", fp),
    copyToData: (srcPath: string) => ipcRenderer.invoke("files:copyToData", srcPath),
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
