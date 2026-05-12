import mysql from "mysql2/promise";
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
