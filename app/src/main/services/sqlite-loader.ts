import Database from "better-sqlite3";
import mysql from "mysql2/promise";
import fs from "node:fs/promises";
import Papa from "papaparse";
import type { DataSource } from "../../shared/types.js";

/** 소스명 → SQL 테이블명 (특수문자를 _ 로 교체) */
export function toTableName(sourceName: string): string {
  return sourceName.replace(/[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]/g, "_").replace(/^_+|_+$/g, "") || "source";
}

export interface TableLoadResult {
  tableName: string;
  rowCount: number;
}

export interface SourceLoadResult {
  sourceName: string;
  tables: TableLoadResult[];
  error?: string;
}

function escName(n: string) {
  return `"${n.replace(/"/g, '""')}"`;
}

/** null → null, object/array → JSON string, else → String */
function toCell(v: unknown): string | null {
  if (v == null) return null;
  if (typeof v === "object") return JSON.stringify(v);
  return String(v);
}

function createAndInsert(
  db: Database.Database,
  tableName: string,
  fields: string[],
  rows: Record<string, unknown>[]
): number {
  const cols = fields.map((f) => `${escName(f)} TEXT`).join(", ");
  db.exec(`DROP TABLE IF EXISTS ${escName(tableName)}`);
  db.exec(fields.length > 0 ? `CREATE TABLE ${escName(tableName)} (${cols})` : `CREATE TABLE ${escName(tableName)} (_empty TEXT)`);
  if (fields.length === 0 || rows.length === 0) return 0;

  const ph = fields.map(() => "?").join(", ");
  const stmt = db.prepare(`INSERT INTO ${escName(tableName)} VALUES (${ph})`);
  const insertMany = db.transaction((data: Record<string, unknown>[]) => {
    for (const row of data) {
      stmt.run(fields.map((f) => toCell(row[f])));
    }
  });
  insertMany(rows);
  return rows.length;
}

async function fromCsv(db: Database.Database, ds: DataSource & { type: "csv" }): Promise<TableLoadResult[]> {
  const tableName = toTableName(ds.name);
  const raw = await fs.readFile(ds.config.filePath, "utf-8");
  const { data, meta } = Papa.parse<Record<string, string>>(raw, {
    header: true,
    skipEmptyLines: true,
    delimiter: ds.config.delimiter,
  });
  const rowCount = createAndInsert(db, tableName, meta.fields ?? [], data as Record<string, unknown>[]);
  return [{ tableName, rowCount }];
}

async function fromJsonl(db: Database.Database, ds: DataSource & { type: "jsonl" }): Promise<TableLoadResult[]> {
  const tableName = toTableName(ds.name);
  const raw = await fs.readFile(ds.config.filePath, "utf-8");
  const rows = raw.trim().split("\n").filter(Boolean).map((l) => JSON.parse(l) as Record<string, unknown>);
  const fields = rows.length > 0 ? Object.keys(rows[0]) : [];
  const rowCount = createAndInsert(db, tableName, fields, rows);
  return [{ tableName, rowCount }];
}

async function fromJson(db: Database.Database, ds: DataSource & { type: "json" }): Promise<TableLoadResult[]> {
  const tableName = toTableName(ds.name);
  const raw = await fs.readFile(ds.config.filePath, "utf-8");
  let parsed: unknown = JSON.parse(raw);
  if (ds.config.rootPath) {
    for (const key of ds.config.rootPath.split(".")) {
      parsed = (parsed as Record<string, unknown>)[key];
    }
  }
  const arr: Record<string, unknown>[] = Array.isArray(parsed)
    ? (parsed as Record<string, unknown>[])
    : [parsed as Record<string, unknown>];
  const fields = arr.length > 0 ? Object.keys(arr[0]) : [];
  const rowCount = createAndInsert(db, tableName, fields, arr);
  return [{ tableName, rowCount }];
}

async function fromMariaDb(db: Database.Database, ds: DataSource & { type: "mariadb" }): Promise<TableLoadResult[]> {
  const cfg = ds.config;
  const prefix = toTableName(ds.name);
  const conn = await mysql.createConnection({
    host: cfg.host, port: cfg.port, database: cfg.database,
    user: cfg.user, password: cfg.password, connectTimeout: 10_000,
  });
  const results: TableLoadResult[] = [];
  try {
    const [tableList] = await conn.query<mysql.RowDataPacket[]>(
      "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? ORDER BY TABLE_NAME",
      [cfg.database]
    );
    for (const { TABLE_NAME } of tableList) {
      const tableName = `${prefix}_${TABLE_NAME as string}`;
      const [rows] = await conn.query<mysql.RowDataPacket[]>(`SELECT * FROM \`${TABLE_NAME as string}\``);
      const dataRows = rows as Record<string, unknown>[];
      const fields = dataRows.length > 0 ? Object.keys(dataRows[0]) : [];
      const rowCount = createAndInsert(db, tableName, fields, dataRows);
      results.push({ tableName, rowCount });
    }
  } finally {
    await conn.end();
  }
  return results;
}

async function fromShapefile(db: Database.Database, ds: DataSource & { type: "shapefile" }): Promise<TableLoadResult[]> {
  const shp = await import("shapefile");
  const tableName = toTableName(ds.name);
  const shpPath = ds.config.shpPath;
  const dbfPath = shpPath.replace(/\.shp$/i, ".dbf");
  const source = await shp.open(shpPath, dbfPath);
  const rows: Record<string, unknown>[] = [];
  for (;;) {
    const result = await source.read();
    if (result.done) break;
    const f = result.value;
    const coords = f.geometry?.type === "Point"
      ? (f.geometry as { coordinates: number[] }).coordinates
      : [null, null];
    rows.push({ ...(f.properties ?? {}), x: coords[0], y: coords[1] });
  }
  const fields = rows.length > 0 ? Object.keys(rows[0]) : ["x", "y"];
  const rowCount = createAndInsert(db, tableName, fields, rows);
  return [{ tableName, rowCount }];
}

export async function loadSourceToDb(
  db: Database.Database,
  ds: DataSource
): Promise<SourceLoadResult> {
  try {
    let tables: TableLoadResult[] = [];
    switch (ds.type) {
      case "csv":       tables = await fromCsv(db, ds);       break;
      case "json":      tables = await fromJson(db, ds);      break;
      case "jsonl":     tables = await fromJsonl(db, ds);     break;
      case "mariadb":   tables = await fromMariaDb(db, ds);   break;
      case "shapefile": tables = await fromShapefile(db, ds); break;
    }
    return { sourceName: ds.name, tables };
  } catch (e) {
    return { sourceName: ds.name, tables: [], error: (e as Error).message };
  }
}
