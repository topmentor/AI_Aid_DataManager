import mysql from "mysql2/promise";
import fs from "node:fs/promises";
import Papa from "papaparse";
import type {
  DataSource,
  DataSourceSchema,
  ColumnSchema,
  TableSchema,
  MariaDbConfig,
  CsvConfig,
  JsonConfig,
} from "../../shared/types.js";

async function inspectMariaDb(ds: DataSource & { type: "mariadb" }): Promise<DataSourceSchema> {
  const cfg = ds.config as MariaDbConfig;
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
  const cfg = ds.config as CsvConfig;
  const raw = await fs.readFile(cfg.filePath, "utf-8");

  const { data, meta } = Papa.parse<Record<string, string>>(raw, {
    header: true,
    preview: 5,
    skipEmptyLines: true,
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
  const cfg = ds.config as JsonConfig;
  const raw = await fs.readFile(cfg.filePath, "utf-8");
  let parsed: unknown = JSON.parse(raw);

  // Navigate to rootPath if specified (e.g. "data.items")
  if (cfg.rootPath) {
    for (const key of cfg.rootPath.split(".")) {
      parsed = (parsed as Record<string, unknown>)[key];
    }
  }

  // Get structure sample (first item if array, whole object if not)
  const sample = Array.isArray(parsed) ? parsed[0] : parsed;
  const structure = JSON.stringify(sample, null, 2).slice(0, 800);

  // If the root is an array of objects, extract flat column names
  let columns: ColumnSchema[] | undefined;
  if (Array.isArray(parsed) && parsed.length > 0 && typeof parsed[0] === "object" && parsed[0] !== null) {
    columns = Object.keys(parsed[0] as object).map((name) => ({
      name,
      type: typeof (parsed as Record<string, unknown>[])[0][name],
      sample: String((parsed as Record<string, unknown>[])[0][name] ?? ""),
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

export async function inspectSchema(ds: DataSource): Promise<DataSourceSchema> {
  switch (ds.type) {
    case "mariadb": return inspectMariaDb(ds);
    case "csv":     return inspectCsv(ds);
    case "json":    return inspectJson(ds);
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
