import fs from "node:fs/promises";
import path from "node:path";
import { app } from "electron";
import type { DataSource } from "../../shared/types.js";

function catalogPath(): string {
  return path.join(app.getPath("userData"), "catalog.json");
}

export async function listSources(): Promise<DataSource[]> {
  try {
    const raw = await fs.readFile(catalogPath(), "utf-8");
    return JSON.parse(raw) as DataSource[];
  } catch {
    return [];
  }
}

async function saveSources(sources: DataSource[]): Promise<void> {
  await fs.writeFile(catalogPath(), JSON.stringify(sources, null, 2), "utf-8");
}

export async function addSource(
  ds: Omit<DataSource, "id">
): Promise<DataSource> {
  const sources = await listSources();
  const newDs: DataSource = { ...ds, id: crypto.randomUUID() } as DataSource;
  await saveSources([...sources, newDs]);
  return newDs;
}

export async function updateSource(ds: DataSource): Promise<void> {
  const sources = await listSources();
  const idx = sources.findIndex((s) => s.id === ds.id);
  if (idx === -1) throw new Error(`Source ${ds.id} not found`);
  sources[idx] = ds;
  await saveSources(sources);
}

export async function removeSource(id: string): Promise<void> {
  const sources = await listSources();
  await saveSources(sources.filter((s) => s.id !== id));
}
