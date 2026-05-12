import fs from "node:fs/promises";
import path from "node:path";
import { app } from "electron";
import { getSettings } from "./settings-service.js";
import { listSources } from "./catalog-service.js";
import { inspectSchema } from "./schema-inspector.js";
import { buildDataHelpers } from "./python-runner.js";
import { buildSystemPrompt } from "./system-prompt.js";
import type { Job, JobStatus, DataSourceSchema } from "../../shared/types.js";

// In-memory job cache (also persisted to jobs.json)
let jobCache: Job[] = [];

function jobsFilePath(workspaceRoot: string): string {
  return path.join(workspaceRoot, "jobs.json");
}

async function persistJobs(workspaceRoot: string): Promise<void> {
  await fs.mkdir(workspaceRoot, { recursive: true });
  await fs.writeFile(
    jobsFilePath(workspaceRoot),
    JSON.stringify(jobCache, null, 2),
    "utf-8"
  );
}

export async function loadJobs(): Promise<Job[]> {
  const { workspaceRoot } = await getSettings();
  try {
    const raw = await fs.readFile(jobsFilePath(workspaceRoot), "utf-8");
    jobCache = JSON.parse(raw) as Job[];
  } catch {
    jobCache = [];
  }
  return jobCache;
}

export async function createJob(
  userRequest: string,
  sourceIds: string[]
): Promise<{ job: Job; systemPrompt: string }> {
  const { workspaceRoot } = await getSettings();
  const jobId = crypto.randomUUID();
  const workspaceDir = path.join(workspaceRoot, `job_${jobId}`);
  const outputDir = path.join(workspaceDir, "output");
  await fs.mkdir(outputDir, { recursive: true });

  // Select sources
  const allSources = await listSources();
  const selectedSources =
    sourceIds.length > 0
      ? allSources.filter((s) => sourceIds.includes(s.id))
      : allSources;

  // Generate data_helpers.py (credentials injected here — never seen by Claude)
  const helpersCode = buildDataHelpers(selectedSources);
  await fs.writeFile(
    path.join(workspaceDir, "data_helpers.py"),
    helpersCode,
    "utf-8"
  );

  // Build schema context for system prompt
  const schemas: DataSourceSchema[] = [];
  for (const ds of selectedSources) {
    try {
      schemas.push(await inspectSchema(ds));
    } catch {
      // Skip sources that fail to connect during job creation
    }
  }

  const systemPrompt = buildSystemPrompt(schemas);

  // Write context.md for reference
  await fs.writeFile(
    path.join(workspaceDir, "context.md"),
    `# User Request\n${userRequest}\n\n# System Prompt\n${systemPrompt}`,
    "utf-8"
  );

  const job: Job = {
    id: jobId,
    createdAt: new Date().toISOString(),
    userRequest,
    status: "idle",
    workspaceDir,
    outputFiles: [],
  };

  jobCache.push(job);
  await persistJobs(workspaceRoot);

  return { job, systemPrompt };
}

export function getJob(jobId: string): Job | undefined {
  return jobCache.find((j) => j.id === jobId);
}

export async function updateJob(
  jobId: string,
  updates: Partial<Pick<Job, "status" | "outputFiles" | "errorMsg">>
): Promise<void> {
  const job = jobCache.find((j) => j.id === jobId);
  if (!job) return;
  Object.assign(job, updates);
  const { workspaceRoot } = await getSettings();
  await persistJobs(workspaceRoot);
}
