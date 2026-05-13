import mysql from "mysql2/promise";

/** null → null, object/array → JSON string, else → String */
function toCell(v: unknown): string {
  if (v == null) return "";
  if (typeof v === "object") return JSON.stringify(v);
  return String(v);
}
import fs from "node:fs/promises";
import Papa from "papaparse";
import type {
  DataSource,
  DataSourceSchema,
  ColumnSchema,
  TableSchema,
} from "../../shared/types.js";

async function inspectMariaDb(ds: DataSource & { type: "mariadb" }): Promise<DataSourceSchema> {
  const cfg = ds.config;
  const conn = await mysql.createConnection({
    host: cfg.host,
    port: cfg.port,
    database: cfg.database,
    user: cfg.user,
    password: cfg.password,
    connectTimeout: 10_000,
  });

  try {
    const [rows] = await conn.query<mysql.RowDataPacket[]>(
      `SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
       FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = ?
       ORDER BY TABLE_NAME, ORDINAL_POSITION`,
      [cfg.database]
    );

    const tableMap = new Map<string, ColumnSchema[]>();
    for (const r of rows) {
      if (!tableMap.has(r.TABLE_NAME)) tableMap.set(r.TABLE_NAME, []);
      tableMap.get(r.TABLE_NAME)!.push({
        name: r.COLUMN_NAME,
        type: r.DATA_TYPE,
        nullable: r.IS_NULLABLE === "YES",
      });
    }

    const tables: TableSchema[] = [];
    for (const [tableName, columns] of tableMap) {
      tables.push({ tableName, columns });
    }

    return {
      sourceId: ds.id,
      sourceName: ds.name,
      type: "mariadb",
      tables,
    };
  } finally {
    await conn.end();
  }
}

async function inspectCsv(ds: DataSource & { type: "csv" }): Promise<DataSourceSchema> {
  const cfg = ds.config;
  const raw = await fs.readFile(cfg.filePath, "utf-8");

  const { data, meta } = Papa.parse<Record<string, string>>(raw, {
    header: true,
    preview: 5,
    skipEmptyLines: true,
    delimiter: cfg.delimiter,  // "" means auto-detect (PapaParse default)
  });

  const sample = data as Record<string, string>[];
  const columns: ColumnSchema[] = (meta.fields ?? []).map((name) => ({
    name,
    type: "string",
    sample: sample[0]?.[name] ?? "",
  }));

  return {
    sourceId: ds.id,
    sourceName: ds.name,
    type: "csv",
    columns,
  };
}

async function inspectJson(ds: DataSource & { type: "json" }): Promise<DataSourceSchema> {
  const cfg = ds.config;
  const raw = await fs.readFile(cfg.filePath, "utf-8");
  let parsed: unknown = JSON.parse(raw);

  // Navigate to rootPath if specified (e.g. "data.items")
  if (cfg.rootPath) {
    for (const key of cfg.rootPath.split(".")) {
      parsed = (parsed as Record<string, unknown>)[key];
    }
    if (parsed === undefined || parsed === null) {
      throw new Error(`rootPath "${cfg.rootPath}" not found in JSON`);
    }
  }

  // Get structure sample (first item if array, whole object if not)
  const sample = Array.isArray(parsed) ? parsed[0] : parsed;
  const structure = JSON.stringify(sample ?? null, null, 2).slice(0, 800);

  // If the root is an array of objects, extract flat column names
  let columns: ColumnSchema[] | undefined;
  if (Array.isArray(parsed) && parsed.length > 0 && typeof parsed[0] === "object" && parsed[0] !== null) {
    const first = parsed[0] as Record<string, unknown>;
    columns = Object.keys(first).map((name) => ({
      name,
      type: typeof first[name],
      sample: String(first[name] ?? ""),
    }));
  }

  return {
    sourceId: ds.id,
    sourceName: ds.name,
    type: "json",
    columns,
    structure,
  };
}

async function inspectJsonl(ds: DataSource & { type: "jsonl" }): Promise<DataSourceSchema> {
  const cfg = ds.config;
  const raw = await fs.readFile(cfg.filePath, "utf-8");
  const lines = raw.trim().split("\n").filter(Boolean).slice(0, 5);
  if (lines.length === 0) {
    return { sourceId: ds.id, sourceName: ds.name, type: "jsonl" };
  }
  const first = JSON.parse(lines[0]);
  const columns: ColumnSchema[] | undefined =
    typeof first === "object" && first !== null
      ? Object.keys(first as Record<string, unknown>).map((name) => ({
          name,
          type: typeof (first as Record<string, unknown>)[name],
          sample: String((first as Record<string, unknown>)[name] ?? ""),
        }))
      : undefined;
  return { sourceId: ds.id, sourceName: ds.name, type: "jsonl", columns };
}

export async function inspectSchema(ds: DataSource): Promise<DataSourceSchema> {
  switch (ds.type) {
    case "mariadb": return inspectMariaDb(ds);
    case "csv":     return inspectCsv(ds);
    case "json":    return inspectJson(ds);
    case "jsonl":   return inspectJsonl(ds);
  }
}

export async function testConnection(
  ds: DataSource
): Promise<{ ok: boolean; error?: string }> {
  try {
    await inspectSchema(ds);
    return { ok: true };
  } catch (e) {
    return { ok: false, error: (e as Error).message };
  }
}

export interface PreviewResult {
  title: string;
  headers: string[];
  rows: string[][];
}

export async function previewData(ds: DataSource, limit = 50): Promise<PreviewResult> {
  switch (ds.type) {
    case "csv":     return previewCsv(ds, limit);
    case "json":    return previewJson(ds, limit);
    case "jsonl":   return previewJsonl(ds, limit);
    case "mariadb": return previewMariaDb(ds, limit);
  }
}

async function previewCsv(ds: DataSource & { type: "csv" }, limit: number): Promise<PreviewResult> {
  const cfg = ds.config;
  const raw = await fs.readFile(cfg.filePath, "utf-8");
  const { data, meta } = Papa.parse<Record<string, string>>(raw, {
    header: true,
    preview: limit,
    skipEmptyLines: true,
    delimiter: cfg.delimiter,
  });
  const headers = meta.fields ?? [];
  const rows = (data as Record<string, string>[]).map((row) => headers.map((h) => String(row[h] ?? "")));
  return { title: ds.name, headers, rows };
}

async function previewJsonl(ds: DataSource & { type: "jsonl" }, limit: number): Promise<PreviewResult> {
  const cfg = ds.config;
  const raw = await fs.readFile(cfg.filePath, "utf-8");
  const lines = raw.trim().split("\n").filter(Boolean).slice(0, limit);
  if (lines.length === 0) return { title: ds.name, headers: [], rows: [] };
  const parsed = lines.map((l) => JSON.parse(l) as Record<string, unknown>);
  const headers = Object.keys(parsed[0]);
  const rows = parsed.map((obj) => headers.map((h) => toCell(obj[h])));
  return { title: ds.name, headers, rows };
}

async function previewJson(ds: DataSource & { type: "json" }, limit: number): Promise<PreviewResult> {
  const cfg = ds.config;
  const raw = await fs.readFile(cfg.filePath, "utf-8");
  let parsed: unknown = JSON.parse(raw);
  if (cfg.rootPath) {
    for (const key of cfg.rootPath.split(".")) {
      parsed = (parsed as Record<string, unknown>)[key];
    }
  }
  if (Array.isArray(parsed) && parsed.length > 0 && typeof parsed[0] === "object" && parsed[0] !== null) {
    const data = (parsed as Record<string, unknown>[]).slice(0, limit);
    const headers = Object.keys(data[0]);
    const rows = data.map((obj) => headers.map((h) => toCell(obj[h])));
    return { title: ds.name, headers, rows };
  }
  // Non-array: show top-level keys as key/value table
  const obj = parsed as Record<string, unknown>;
  const headers = ["키", "값"];
  const rows = Object.entries(obj)
    .slice(0, limit)
    .map(([k, v]) => [k, toCell(v)]);
  return { title: ds.name, headers, rows };
}

async function previewMariaDb(ds: DataSource & { type: "mariadb" }, limit: number): Promise<PreviewResult> {
  const cfg = ds.config;
  const conn = await mysql.createConnection({
    host: cfg.host,
    port: cfg.port,
    database: cfg.database,
    user: cfg.user,
    password: cfg.password,
    connectTimeout: 10_000,
  });
  try {
    // Pick the first table
    const [tableRows] = await conn.query<mysql.RowDataPacket[]>(
      `SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? LIMIT 1`,
      [cfg.database]
    );
    const tableName = tableRows[0]?.TABLE_NAME as string | undefined;
    if (!tableName) return { title: ds.name, headers: [], rows: [] };

    const [rows] = await conn.query<mysql.RowDataPacket[]>(
      `SELECT * FROM \`${tableName}\` LIMIT ?`,
      [limit]
    );
    if (rows.length === 0) return { title: `${ds.name} / ${tableName}`, headers: [], rows: [] };
    const headers = Object.keys(rows[0]);
    const dataRows = (rows as mysql.RowDataPacket[]).map((row) => headers.map((h) => String(row[h] ?? "")));
    return { title: `${ds.name} / ${tableName}`, headers, rows: dataRows };
  } finally {
    await conn.end();
  }
}
