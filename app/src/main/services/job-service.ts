import fs from "node:fs/promises";
import path from "node:path";
import Database from "better-sqlite3";
import { getSettings } from "./settings-service.js";
import { listSources } from "./catalog-service.js";
import { loadSourceToDb, toTableName } from "./sqlite-loader.js";
import { buildSystemPrompt } from "./system-prompt.js";
import type { SourceTableMap, TableInfo, ColumnInfo } from "./system-prompt.js";
import type { Job } from "../../shared/types.js";

let jobCache: Job[] = [];

/** TEXT-like 컬럼에 대해 고유값 샘플을 수집한다 (최대 8개). */
function collectColumnSamples(db: Database.Database, tableName: string, colName: string, colType: string): string[] | undefined {
  const t = (colType || "TEXT").toUpperCase();
  if (!t.includes("TEXT") && !t.includes("CHAR") && !t.includes("CLOB")) return undefined;
  try {
    const safe = tableName.replace(/"/g, '""');
    const safeCol = colName.replace(/"/g, '""');
    const rows = db.prepare(
      `SELECT DISTINCT "${safeCol}" FROM "${safe}" WHERE "${safeCol}" IS NOT NULL AND "${safeCol}" != '' LIMIT 8`
    ).all() as Record<string, unknown>[];
    const vals = rows.map((r) => String(Object.values(r)[0])).filter((v) => v.length <= 40);
    return vals.length > 0 ? vals : undefined;
  } catch {
    return undefined;
  }
}

/** PRAGMA table_info + 샘플값을 묶어 ColumnInfo[] 반환 */
function describeTable(db: Database.Database, tableName: string): ColumnInfo[] {
  type PragmaRow = { name: string; type: string };
  const safe = tableName.replace(/"/g, '""');
  const colRows = db.prepare(`PRAGMA table_info("${safe}")`).all() as PragmaRow[];
  return colRows.map((r) => ({
    name: r.name,
    type: r.type || "TEXT",
    samples: collectColumnSamples(db, tableName, r.name, r.type),
  }));
}

/** DB에서 모든 사용자 테이블 목록을 읽어 TableInfo[] 반환 */
function readAllTableInfos(db: Database.Database): { name: string; info: TableInfo }[] {
  const tables = db
    .prepare("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")
    .all() as { name: string }[];
  return tables.map((t) => {
    const safe = t.name.replace(/"/g, '""');
    const rowCount = (db.prepare(`SELECT COUNT(*) as cnt FROM "${safe}"`).get() as { cnt: number }).cnt;
    return { name: t.name, info: { tableName: t.name, rowCount, columns: describeTable(db, t.name) } };
  });
}

function jobsFilePath(workspaceRoot: string): string {
  return path.join(workspaceRoot, "jobs.json");
}

async function persistJobs(workspaceRoot: string): Promise<void> {
  await fs.mkdir(workspaceRoot, { recursive: true });
  await fs.writeFile(jobsFilePath(workspaceRoot), JSON.stringify(jobCache, null, 2), "utf-8");
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
): Promise<{ job: Job }> {
  const { workspaceRoot } = await getSettings();
  const jobId = crypto.randomUUID();
  const workspaceDir = path.join(workspaceRoot, `job_${jobId}`);
  await fs.mkdir(path.join(workspaceDir, "output"), { recursive: true });

  const allSources = await listSources();
  const selectedSources = sourceIds.length > 0
    ? allSources.filter((s) => sourceIds.includes(s.id))
    : allSources;

  const dbPath = path.join(workspaceDir, "data.db");
  const db = new Database(dbPath);
  const sourceMaps: SourceTableMap[] = [];

  for (const ds of selectedSources) {
    const result = await loadSourceToDb(db, ds);
    if (result.tables.length > 0) {
      const tablesWithCols = result.tables.map((t) => ({
        ...t,
        columns: describeTable(db, t.tableName),
      }));
      sourceMaps.push({ sourceName: result.sourceName, tables: tablesWithCols });
    }
  }
  db.close();

  await fs.writeFile(path.join(workspaceDir, "CLAUDE.md"), buildSystemPrompt(sourceMaps), "utf-8");

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
  return { job };
}

/**
 * 소스 목록을 기준으로 job의 data.db + CLAUDE.md를 재생성.
 * 등록된 소스 테이블뿐 아니라 쿼리로 생성된 추가 테이블(result 등)도 포함.
 */
export async function refreshJobSources(jobId: string): Promise<void> {
  const job = jobCache.find((j) => j.id === jobId);
  if (!job) return;

  const allSources = await listSources();
  const dbPath = path.join(job.workspaceDir, "data.db");
  const db = new Database(dbPath);
  const sourceMaps: SourceTableMap[] = [];

  try {
    // 1. 등록된 소스를 data.db에 로드 (소스 테이블 DROP 후 재생성)
    for (const ds of allSources) {
      const result = await loadSourceToDb(db, ds);
      if (result.tables.length > 0) {
        const tablesWithCols = result.tables.map((t) => ({
          ...t,
          columns: describeTable(db, t.tableName),
        }));
        sourceMaps.push({ sourceName: result.sourceName, tables: tablesWithCols });
      }
    }

    // 2. 소스 테이블 이외의 테이블(쿼리 결과 등)도 CLAUDE.md에 포함
    const sourcedNames = new Set(sourceMaps.flatMap((m) => m.tables.map((t) => t.tableName)));
    const allTableInfos = readAllTableInfos(db);
    const extraInfos = allTableInfos
      .filter((t) => !sourcedNames.has(t.name))
      .map((t) => t.info);

    if (extraInfos.length > 0) {
      sourceMaps.push({ sourceName: "쿼리 결과", tables: extraInfos });
    }
  } finally {
    db.close();
  }

  await fs.writeFile(
    path.join(job.workspaceDir, "CLAUDE.md"),
    buildSystemPrompt(sourceMaps),
    "utf-8"
  );
}

/**
 * 소스 파일을 재로드하지 않고 현재 data.db 상태만으로 CLAUDE.md를 갱신.
 * SQL 실행 직후 새 테이블이 생겼을 때 호출.
 */
export async function updateClaudeMd(jobId: string): Promise<void> {
  const job = jobCache.find((j) => j.id === jobId);
  if (!job) return;

  const dbPath = path.join(job.workspaceDir, "data.db");
  let db: Database.Database;
  try {
    db = new Database(dbPath, { readonly: true });
  } catch {
    return; // DB 파일 없으면 skip
  }

  const sourceMaps: SourceTableMap[] = [];
  try {
    // 등록된 소스의 테이블명 패턴 파악 (소스 vs 결과 테이블 구분용)
    const allSources = await listSources();
    const sourceBaseNames = new Set(allSources.map((ds) => toTableName(ds.name)));

    const allTableInfos = readAllTableInfos(db);

    // 소스 테이블 그룹화 (sourceName별)
    const sourceGroupMap = new Map<string, TableInfo[]>();
    const resultTables: TableInfo[] = [];

    for (const { name, info } of allTableInfos) {
      // CSV/JSON/JSONL: tableName = toTableName(ds.name)
      // MariaDB: tableName = toTableName(ds.name)_TABLE_NAME
      const matchedSource = allSources.find((ds) => {
        const base = toTableName(ds.name);
        return name === base || name.startsWith(base + "_");
      });

      if (matchedSource) {
        const group = sourceGroupMap.get(matchedSource.name) ?? [];
        group.push(info);
        sourceGroupMap.set(matchedSource.name, group);
      } else {
        resultTables.push(info);
      }
    }

    for (const [sourceName, tables] of sourceGroupMap) {
      sourceMaps.push({ sourceName, tables });
    }
    if (resultTables.length > 0) {
      sourceMaps.push({ sourceName: "쿼리 결과", tables: resultTables });
    }
  } finally {
    db.close();
  }

  await fs.writeFile(
    path.join(job.workspaceDir, "CLAUDE.md"),
    buildSystemPrompt(sourceMaps),
    "utf-8"
  );
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
