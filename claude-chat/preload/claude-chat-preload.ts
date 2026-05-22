import { contextBridge, ipcRenderer } from "electron";

// Dynamic per-session channels are allowed via prefix matching
function isAllowed(channel: string): boolean {
  return (
    channel.startsWith("claude-chat:stream:") ||
    channel.startsWith("claude-chat:done:") ||
    channel.startsWith("claude-chat:error:")
  );
}

// Map<channel, Map<originalFn, wrapperFn>> — prevents memory leaks on cleanup
const registry = new Map<string, Map<Function, Function>>();

function getChannelMap(channel: string): Map<Function, Function> {
  if (!registry.has(channel)) registry.set(channel, new Map());
  return registry.get(channel)!;
}

contextBridge.exposeInMainWorld("claudeChat", {
  probe: () => ipcRenderer.invoke("claude-chat:probe"),

  createSession: (opts?: { cwd?: string; label?: string }) =>
    ipcRenderer.invoke("claude-chat:createSession", opts),

  listSessions: () => ipcRenderer.invoke("claude-chat:listSessions"),

  sendMessage: (sessionId: string, message: string) =>
    ipcRenderer.invoke("claude-chat:sendMessage", sessionId, message),

  abort: (sessionId: string) => ipcRenderer.invoke("claude-chat:abort", sessionId),

  // Returns a cleanup function — call it to unsubscribe (no manual off() needed)
  onStream: (sessionId: string, fn: (event: unknown) => void): (() => void) => {
    const channel = `claude-chat:stream:${sessionId}`;
    if (!isAllowed(channel)) return () => {};
    const wrapper = (_e: unknown, event: unknown) => fn(event);
    getChannelMap(channel).set(fn, wrapper);
    ipcRenderer.on(channel, wrapper as Parameters<typeof ipcRenderer.on>[1]);
    return () => {
      ipcRenderer.removeListener(channel, wrapper as Parameters<typeof ipcRenderer.removeListener>[1]);
      getChannelMap(channel).delete(fn);
    };
  },

  onDone: (sessionId: string, fn: () => void): (() => void) => {
    const channel = `claude-chat:done:${sessionId}`;
    if (!isAllowed(channel)) return () => {};
    const wrapper = () => fn();
    getChannelMap(channel).set(fn, wrapper);
    ipcRenderer.on(channel, wrapper as Parameters<typeof ipcRenderer.on>[1]);
    return () => {
      ipcRenderer.removeListener(channel, wrapper as Parameters<typeof ipcRenderer.removeListener>[1]);
      getChannelMap(channel).delete(fn);
    };
  },

  onError: (sessionId: string, fn: (err: { message: string }) => void): (() => void) => {
    const channel = `claude-chat:error:${sessionId}`;
    if (!isAllowed(channel)) return () => {};
    const wrapper = (_e: unknown, err: { message: string }) => fn(err);
    getChannelMap(channel).set(fn, wrapper);
    ipcRenderer.on(channel, wrapper as Parameters<typeof ipcRenderer.on>[1]);
    return () => {
      ipcRenderer.removeListener(channel, wrapper as Parameters<typeof ipcRenderer.removeListener>[1]);
      getChannelMap(channel).delete(fn);
    };
  },
});
