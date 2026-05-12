import fs from "node:fs/promises";
import path from "node:path";
import { app } from "electron";
import type { AppSettings } from "../../shared/types.js";

const DEFAULT_SETTINGS: AppSettings = {
  claudeBin: "claude",
  pythonBin: "python",
  workspaceRoot: "", // set dynamically below
};

function getDefault(): AppSettings {
  return {
    ...DEFAULT_SETTINGS,
    workspaceRoot: path.join(app.getPath("userData"), "workspace"),
  };
}

function settingsPath(): string {
  return path.join(app.getPath("userData"), "settings.json");
}

export async function getSettings(): Promise<AppSettings> {
  try {
    const raw = await fs.readFile(settingsPath(), "utf-8");
    return { ...getDefault(), ...JSON.parse(raw) };
  } catch {
    return getDefault();
  }
}

export async function setSettings(
  partial: Partial<AppSettings>
): Promise<AppSettings> {
  const current = await getSettings();
  const next = { ...current, ...partial };
  await fs.writeFile(settingsPath(), JSON.stringify(next, null, 2), "utf-8");
  return next;
}
