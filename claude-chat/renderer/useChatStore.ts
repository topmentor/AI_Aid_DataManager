import { create } from "zustand";
import type { ChatSession, ClaudeProbe } from "../shared/chat-types";

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  text: string;
  status: "done" | "streaming" | "error";
  toolCalls: ToolCall[];
  error?: string;
  timestamp: string;
}

export interface ToolCall {
  id: string;
  name: string;
  summary: string;
  status: "running" | "done";
}

interface StreamingState {
  sessionId: string;
  assistantMessageId: string;
}

interface ChatStore {
  probe: ClaudeProbe | null;
  sessions: ChatSession[];
  activeSessionId: string | null;
  messages: Map<string, ChatMessage[]>; // sessionId → messages
  streaming: StreamingState | null;

  setProbe: (p: ClaudeProbe) => void;
  setSessions: (s: ChatSession[]) => void;
  addSession: (s: ChatSession) => void;
  setActiveSession: (id: string | null) => void;

  addMessage: (sessionId: string, msg: ChatMessage) => void;
  ensureAssistantMessage: (msgId: string) => void;
  appendAssistantText: (msgId: string, text: string) => void;
  addToolCall: (msgId: string, tool: ToolCall) => void;
  updateToolCall: (msgId: string, toolId: string, patch: Partial<ToolCall>) => void;
  finalizeAssistantMessage: (msgId: string, status: "done" | "error", error?: string) => void;

  setStreaming: (s: StreamingState | null) => void;
}

function updateMessageInSessions(
  messages: Map<string, ChatMessage[]>,
  msgId: string,
  updater: (m: ChatMessage) => ChatMessage
): Map<string, ChatMessage[]> {
  const next = new Map(messages);
  for (const [sid, msgs] of next) {
    const idx = msgs.findIndex((m) => m.id === msgId);
    if (idx !== -1) {
      const updated = [...msgs];
      updated[idx] = updater(updated[idx]);
      next.set(sid, updated);
      break;
    }
  }
  return next;
}

export const useChatStore = create<ChatStore>((set) => ({
  probe: null,
  sessions: [],
  activeSessionId: null,
  messages: new Map(),
  streaming: null,

  setProbe: (p) => set({ probe: p }),
  setSessions: (s) => set({ sessions: s }),
  addSession: (s) => set((st) => ({ sessions: [...st.sessions, s] })),
  setActiveSession: (id) => set({ activeSessionId: id }),

  addMessage: (sessionId, msg) =>
    set((st) => {
      const next = new Map(st.messages);
      next.set(sessionId, [...(next.get(sessionId) ?? []), msg]);
      return { messages: next };
    }),

  ensureAssistantMessage: (msgId) =>
    set((st) => {
      const sessionId = st.activeSessionId;
      if (!sessionId) return {};
      const msg: ChatMessage = {
        id: msgId,
        role: "assistant",
        text: "",
        status: "streaming",
        toolCalls: [],
        timestamp: new Date().toISOString(),
      };
      const next = new Map(st.messages);
      next.set(sessionId, [...(next.get(sessionId) ?? []), msg]);
      return { messages: next };
    }),

  appendAssistantText: (msgId, text) =>
    set((st) => ({
      messages: updateMessageInSessions(st.messages, msgId, (m) => ({
        ...m,
        text: m.text + text,
      })),
    })),

  addToolCall: (msgId, tool) =>
    set((st) => ({
      messages: updateMessageInSessions(st.messages, msgId, (m) => ({
        ...m,
        toolCalls: [...m.toolCalls, tool],
      })),
    })),

  updateToolCall: (msgId, toolId, patch) =>
    set((st) => ({
      messages: updateMessageInSessions(st.messages, msgId, (m) => ({
        ...m,
        toolCalls: m.toolCalls.map((c) =>
          c.id === toolId ? { ...c, ...patch } : c
        ),
      })),
    })),

  finalizeAssistantMessage: (msgId, status, error) =>
    set((st) => ({
      messages: updateMessageInSessions(st.messages, msgId, (m) => ({
        ...m,
        status,
        error,
        toolCalls: m.toolCalls.map((c) =>
          c.status === "running" ? { ...c, status: "done" } : c
        ),
      })),
    })),

  setStreaming: (s) => set({ streaming: s }),
}));
