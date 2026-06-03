import { create } from "zustand";
import type {
  DataSource,
  Job,
  DataSourceSchema,
  AppSettings,
} from "../../../shared/types";

export type CenterTabSourceRef =
  | { kind: "catalog"; sourceId: string }
  | { kind: "db"; jobId: string; tableName: string };

export interface CenterTab {
  id: string;
  title: string;
  headers: string[];
  rows: string[][];
  view: "table" | "chart" | "map";
  sourceRef?: CenterTabSourceRef;
  fullyLoaded?: boolean;
}

interface AppState {
  // Navigation
  view: "start" | "main";
  // App config
  settings: AppSettings | null;
  // Data sources
  sources: DataSource[];
  schemas: Map<string, DataSourceSchema>;
  // Jobs
  jobs: Job[];
  activeJobId: string | null;
  // Center panel tabs (VSCode-style)
  centerTabs: CenterTab[];
  activeCenterTabId: string | null;
  // Settings modal
  settingsOpen: boolean;
  // Actions
  setView: (v: "start" | "main") => void;
  setSettings: (s: AppSettings) => void;
  setSettingsOpen: (open: boolean) => void;
  setSources: (s: DataSource[]) => void;
  setSchema: (id: string, schema: DataSourceSchema) => void;
  setJobs: (j: Job[]) => void;
  addJob: (j: Job) => void;
  updateJob: (j: Job) => void;
  setActiveJob: (id: string | null) => void;
  // Center tab actions
  openCenterTab: (tab: Omit<CenterTab, "view">) => void;
  closeCenterTab: (id: string) => void;
  setActiveCenterTab: (id: string) => void;
  setCenterTabView: (id: string, view: "table" | "chart" | "map") => void;
}

export const useAppStore = create<AppState>((set) => ({
  view: "start",
  settings: null,
  sources: [],
  schemas: new Map(),
  jobs: [],
  activeJobId: null,
  centerTabs: [],
  activeCenterTabId: null,
  settingsOpen: false,

  setView: (v) => set({ view: v }),
  setSettings: (s) => set({ settings: s }),
  setSettingsOpen: (open) => set({ settingsOpen: open }),
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

  // Center tab actions
  openCenterTab: (tab) =>
    set((state) => {
      const existing = state.centerTabs.find((t) => t.id === tab.id);
      if (existing) {
        const centerTabs = state.centerTabs.map((t) =>
          t.id === tab.id
            ? {
                ...t,
                title: tab.title,
                headers: tab.headers,
                rows: tab.rows,
                sourceRef: tab.sourceRef ?? t.sourceRef,
                fullyLoaded: tab.fullyLoaded,
              }
            : t
        );
        return { centerTabs, activeCenterTabId: tab.id };
      }
      const newTab: CenterTab = { ...tab, view: "table" };
      return {
        centerTabs: [...state.centerTabs, newTab],
        activeCenterTabId: tab.id,
      };
    }),
  closeCenterTab: (id) =>
    set((state) => {
      const centerTabs = state.centerTabs.filter((t) => t.id !== id);
      let activeCenterTabId = state.activeCenterTabId;
      if (activeCenterTabId === id) {
        const idx = state.centerTabs.findIndex((t) => t.id === id);
        activeCenterTabId = centerTabs[Math.max(0, idx - 1)]?.id ?? null;
      }
      return { centerTabs, activeCenterTabId };
    }),
  setActiveCenterTab: (id) => set({ activeCenterTabId: id }),
  setCenterTabView: (id, view) =>
    set((state) => ({
      centerTabs: state.centerTabs.map((t) =>
        t.id === id ? { ...t, view } : t
      ),
    })),
}));
