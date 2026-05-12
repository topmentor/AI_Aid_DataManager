import { create } from "zustand";
import type {
  DataSource,
  Job,
  DataSourceSchema,
  ClaudeEnvProbe,
  AppSettings,
  ClaudeStreamEvent,
} from "../../../shared/types";

export type ToolCallView = {
  id: string;
  name: string;
  summary: string;
  status: "running" | "done";
};

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  text: string;
  toolCalls: ToolCallView[];
  status: "pending" | "streaming" | "done" | "error";
  error?: string;
  timestamp: string;
}

interface AppState {
  // Navigation
  view: "start" | "main";
  // App config
  settings: AppSettings | null;
  probe: ClaudeEnvProbe | null;
  // Data sources
  sources: DataSource[];
  schemas: Map<string, DataSourceSchema>;
  // Jobs
  jobs: Job[];
  activeJobId: string | null;
  // Chat messages per job
  chatMessages: Map<string, ChatMessage[]>;
  // Code in Monaco
  activeAnalyzeCode: string;
  // Streaming state
  streaming: { jobId: string; assistantMessageId: string } | null;
  // Actions
  setView: (v: "start" | "main") => void;
  setSettings: (s: AppSettings) => void;
  setProbe: (p: ClaudeEnvProbe) => void;
  setSources: (s: DataSource[]) => void;
  setSchema: (id: string, schema: DataSourceSchema) => void;
  setJobs: (j: Job[]) => void;
  addJob: (j: Job) => void;
  updateJob: (j: Job) => void;
  setActiveJob: (id: string | null) => void;
  addChatMessage: (jobId: string, msg: Omit<ChatMessage, "id" | "toolCalls" | "status"> & Partial<Pick<ChatMessage, "id" | "toolCalls" | "status">>) => void;
  setActiveCode: (code: string) => void;
  // Streaming actions
  ensureAssistantMessage: (id: string) => void;
  appendAssistantText: (id: string, text: string) => void;
  upsertToolCall: (assistantId: string, call: ToolCallView) => void;
  markAssistantDone: (id: string) => void;
  markAssistantError: (id: string, error: string) => void;
  setStreaming: (s: { jobId: string; assistantMessageId: string } | null) => void;
}

export const useAppStore = create<AppState>((set) => ({
  view: "start",
  settings: null,
  probe: null,
  sources: [],
  schemas: new Map(),
  jobs: [],
  activeJobId: null,
  chatMessages: new Map(),
  activeAnalyzeCode: "",
  streaming: null,

  setView: (v) => set({ view: v }),
  setSettings: (s) => set({ settings: s }),
  setProbe: (p) => set({ probe: p }),
  setSources: (s) => set({ sources: s }),
  setSchema: (id, schema) =>
    set((state) => {
      const schemas = new Map(state.schemas);
      schemas.set(id, schema);
      return { schemas };
    }),
  setJobs: (j) => set({ jobs: j }),
  addJob: (j) => set((state) => ({ jobs: [...state.jobs, j] })),
  updateJob: (j) =>
    set((state) => ({ jobs: state.jobs.map((x) => (x.id === j.id ? j : x)) })),
  setActiveJob: (id) => set({ activeJobId: id }),
  addChatMessage: (jobId, msg) =>
    set((state) => {
      const chatMessages = new Map(state.chatMessages);
      const fullMsg: ChatMessage = {
        id: msg.id ?? crypto.randomUUID(),
        role: msg.role,
        text: msg.text,
        toolCalls: msg.toolCalls ?? [],
        status: msg.status ?? (msg.role === "user" ? "done" : "streaming"),
        error: undefined,
        timestamp: msg.timestamp,
      };
      chatMessages.set(jobId, [...(chatMessages.get(jobId) ?? []), fullMsg]);
      return { chatMessages };
    }),
  setActiveCode: (code) => set({ activeAnalyzeCode: code }),

  ensureAssistantMessage: (id) =>
    set((s) => {
      if (s.chatMessages.get(s.activeJobId ?? "")?.some((m) => m.id === id)) return s;
      const msg: ChatMessage = {
        id,
        role: "assistant",
        text: "",
        toolCalls: [],
        status: "streaming",
        timestamp: new Date().toISOString(),
      };
      const m = new Map(s.chatMessages);
      const jobId = s.activeJobId ?? "";
      m.set(jobId, [...(m.get(jobId) ?? []), msg]);
      return { chatMessages: m };
    }),

  appendAssistantText: (id, text) =>
    set((s) => {
      const m = new Map(s.chatMessages);
      for (const [jid, msgs] of m) {
        if (msgs.some((msg) => msg.id === id)) {
          m.set(
            jid,
            msgs.map((msg) =>
              msg.id === id
                ? { ...msg, text: msg.text + text, status: "streaming" as const }
                : msg
            )
          );
          break;
        }
      }
      return { chatMessages: m };
    }),

  upsertToolCall: (assistantId, call) =>
    set((s) => {
      const m = new Map(s.chatMessages);
      for (const [jid, msgs] of m) {
        if (msgs.some((msg) => msg.id === assistantId)) {
          m.set(
            jid,
            msgs.map((msg) => {
              if (msg.id !== assistantId) return msg;
              const existing = msg.toolCalls.find((c) => c.id === call.id);
              const toolCalls = existing
                ? msg.toolCalls.map((c) => (c.id === call.id ? { ...c, ...call } : c))
                : [...msg.toolCalls, call];
              return { ...msg, toolCalls };
            })
          );
          break;
        }
      }
      return { chatMessages: m };
    }),

  markAssistantDone: (id) =>
    set((s) => {
      const m = new Map(s.chatMessages);
      for (const [jid, msgs] of m) {
        if (msgs.some((msg) => msg.id === id)) {
          m.set(
            jid,
            msgs.map((msg) =>
              msg.id === id
                ? {
                    ...msg,
                    status: "done" as const,
                    toolCalls: msg.toolCalls.map((c) => ({ ...c, status: "done" as const })),
                  }
                : msg
            )
          );
          break;
        }
      }
      return { chatMessages: m, streaming: null };
    }),

  markAssistantError: (id, error) =>
    set((s) => {
      const m = new Map(s.chatMessages);
      for (const [jid, msgs] of m) {
        if (msgs.some((msg) => msg.id === id)) {
          m.set(
            jid,
            msgs.map((msg) =>
              msg.id === id ? { ...msg, status: "error" as const, error } : msg
            )
          );
          break;
        }
      }
      return { chatMessages: m, streaming: null };
    }),

  setStreaming: (s) => set({ streaming: s }),
}));
