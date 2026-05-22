import fs from "node:fs/promises";
import path from "node:path";
import Database from "better-sqlite3";

const MAX_QUERY_HISTORY = 20;
const MAX_RESULT_HISTORY = 10;

/** query.sql이 존재하면 workspaceDir/history/query_NNN.sql 로 백업 */
export async function backupQuerySql(workspaceDir: string): Promise<void> {
  const src = path.join(workspaceDir, "query.sql");
  try {
    const content = await fs.readFile(src, "utf-8");
    if (!content.trim()) return;
  } catch {
    return; // query.sql 아직 없음
  }

  const histDir = path.join(workspaceDir, "history");
  await fs.mkdir(histDir, { recursive: true });

  const files = await fs.readdir(histDir).catch(() => [] as string[]);
  const nums = files
    .map((f) => { const m = f.match(/^query_(\d+)\.sql$/); return m ? parseInt(m[1], 10) : null; })
    .filter((n): n is number => n !== null)
    .sort((a, b) => a - b);

  const nextNum = nums.length > 0 ? nums[nums.length - 1] + 1 : 1;
  await fs.copyFile(src, path.join(histDir, `query_${String(nextNum).padStart(3, "0")}.sql`));

  // MAX 초과분 삭제 (오래된 것 먼저)
  if (nums.length >= MAX_QUERY_HISTORY) {
    const toDelete = nums.slice(0, nums.length - MAX_QUERY_HISTORY + 1);
    for (const n of toDelete) {
      await fs.rm(path.join(histDir, `query_${String(n).padStart(3, "0")}.sql`)).catch(() => {});
    }
  }
}

/** DB에 result 테이블이 있으면 result_bak_NNN 으로 복사 */
export function backupResultTable(db: Database.Database): void {
  const exists = db.prepare(
    "SELECT name FROM sqlite_master WHERE type='table' AND name='result'"
  ).get() as { name: string } | undefined;
  if (!exists) return;

  const rows = db.prepare(
    "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'result_bak_%'"
  ).all() as { name: string }[];
  const nums = rows
    .map((r) => { const m = r.name.match(/^result_bak_(\d+)$/); return m ? parseInt(m[1], 10) : null; })
    .filter((n): n is number => n !== null)
    .sort((a, b) => a - b);

  const nextNum = nums.length > 0 ? nums[nums.length - 1] + 1 : 1;
  const bakName = `result_bak_${String(nextNum).padStart(3, "0")}`;
  db.exec(`CREATE TABLE "${bakName}" AS SELECT * FROM result`);

  if (nums.length >= MAX_RESULT_HISTORY) {
    const toDelete = nums.slice(0, nums.length - MAX_RESULT_HISTORY + 1);
    for (const n of toDelete) {
      db.exec(`DROP TABLE IF EXISTS "result_bak_${String(n).padStart(3, "0")}"`);
    }
  }
}

/** workspaceDir/history/ 안의 query_NNN.sql 파일 목록을 최신순으로 반환 */
export async function listQueryHistory(workspaceDir: string): Promise<string[]> {
  const histDir = path.join(workspaceDir, "history");
  const files = await fs.readdir(histDir).catch(() => [] as string[]);
  return files
    .filter((f) => /^query_\d+\.sql$/.test(f))
    .sort()
    .reverse();
}
