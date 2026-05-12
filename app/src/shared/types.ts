// 데이터 소스 종류
export interface MariaDbConfig {
  host: string; port: number; database: string;
  user: string; password: string;
}
export interface CsvConfig { filePath: string; delimiter?: string; }
export interface JsonConfig { filePath: string; rootPath?: string; }

interface DataSourceBase {
  id: string;
  name: string;
}

export type DataSource =
  | (DataSourceBase & { type: "mariadb"; config: MariaDbConfig })
  | (DataSourceBase & { type: "csv"; config: CsvConfig })
  | (DataSourceBase & { type: "json"; config: JsonConfig });

export type DataSourceType = DataSource["type"];

export interface ColumnSchema {
  name: string;
  type: string;
  nullable?: boolean;
  sample?: string;
}
export interface TableSchema { tableName: string; columns: ColumnSchema[]; }
export interface DataSourceSchema {
  sourceId: string;
  sourceName: string;
  type: DataSourceType;
  tables?: TableSchema[];      // mariadb
  columns?: ColumnSchema[];    // csv / json (flat)
  structure?: string;          // json (nested description)
}

export type JobStatus = "idle" | "planning" | "running" | "done" | "error";

export interface Job {
  id: string;
  createdAt: string;
  userRequest: string;
  status: JobStatus;
  workspaceDir: string;
  outputFiles: OutputFile[];
  errorMsg?: string;
}

export interface OutputFile {
  name: string;
  path: string;
  type: "csv" | "png" | "html" | "json" | "other";
  sizeBytes: number;
}

export interface ClaudeEnvProbe {
  binaryPath: string | null;
  version: string | null;
  authenticated: boolean;
  roundTripMs: number | null;
  error: string | null;
}

// Claude streaming 이벤트 (renderer로 relay)
export type ClaudeStreamEvent =
  | { type: "assistant"; text: string }
  | { type: "tool_use"; name: string; input: unknown }
  | { type: "tool_result"; content: string }
  | { type: "result"; sessionId: string; subtype: string; resultText: string }
  | { type: "error"; message: string };

export interface AppSettings {
  claudeBin: string;
  pythonBin: string;
  workspaceRoot: string;
}
