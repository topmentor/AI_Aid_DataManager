import { create } from "zustand";
import type {
  DataSource,
  Job,
  DataSourceSchema,
  ClaudeEnvProbe,
  AppSettings,
  ClaudeStreamEvent,
} from "../../../shared/types";

export interface ChatMessage {
  role: "user" | "assistant";
  text: string;
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
  addChatMessage: (jobId: string, msg: ChatMessage) => void;
  setActiveCode: (code: string) => void;
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
      chatMessages.set(jobId, [...(chatMessages.get(jobId) ?? []), msg]);
      return { chatMessages };
    }),
  setActiveCode: (code) => set({ activeAnalyzeCode: code }),
}));
