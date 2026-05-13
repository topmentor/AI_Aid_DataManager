import type {
  DataSource,
  DataSourceSchema,
  Job,
  ClaudeEnvProbe,
  AppSettings,
} from "../../shared/types";

declare global {
  interface Window {
    aidclaude: {
      settings: {
        get(): Promise<AppSettings>;
        set(s: Partial<AppSettings>): Promise<AppSettings>;
      };
      catalog: {
        list(): Promise<DataSource[]>;
        add(ds: Omit<DataSource, "id">): Promise<DataSource>;
        update(ds: DataSource): Promise<void>;
        remove(id: string): Promise<void>;
        testConnection(id: string): Promise<{ ok: boolean; error?: string }>;
        getSchema(id: string): Promise<DataSourceSchema>;
        previewData(id: string, limit?: number): Promise<{ title: string; headers: string[]; rows: string[][] }>;
      };
      claude: {
        probe(): Promise<ClaudeEnvProbe>;
        sendMessage(jobId: string, message: string): Promise<void>;
        abort(jobId: string): Promise<void>;
      };
      jobs: {
        create(userRequest: string, sourceIds: string[]): Promise<Job>;
        list(): Promise<Job[]>;
        runAnalysis(jobId: string): Promise<{ ok: boolean; error?: string }>;
        runSql(jobId: string, sql: string): Promise<{ ok: boolean; error?: string }>;
        refreshSources(jobId: string): Promise<void>;
        getSqlOptions(jobId: string): Promise<{ title: string; sql: string }[]>;
      };
      data: {
        saveAsSource(sourceName: string, headers: string[], rows: string[][]): Promise<{ ok: boolean; source?: DataSource; error?: string }>;
      };
      db: {
        listTables(jobId: string): Promise<string[]>;
        previewTable(jobId: string, tableName: string, limit?: number): Promise<{ title: string; headers: string[]; rows: string[][] }>;
        saveAsSource(jobId: string, tableName: string, sourceName: string): Promise<{ ok: boolean; source?: DataSource; error?: string }>;
      };
      files: {
        open(fp: string): Promise<void>;
        readText(fp: string): Promise<string | null>;
        writeText(fp: string, content: string): Promise<void>;
        readLines(fp: string, count: number): Promise<string[]>;
        readBase64(path: string): Promise<string>;
        copyToData(srcPath: string): Promise<string>;
        copyShapefile(srcShpPath: string): Promise<{ shpPath: string; encoding: string }>;
      };
      export: {
        saveText(defaultName: string, filters: { name: string; extensions: string[] }[], content: string): Promise<string | null>;
        saveBinary(defaultName: string, filters: { name: string; extensions: string[] }[], base64: string): Promise<string | null>;
      };
      dialog: {
        openFile(filters: { name: string; extensions: string[] }[]): Promise<string | null>;
      };
      on(channel: string, fn: (...args: unknown[]) => void): void;
      off(channel: string, fn: (...args: unknown[]) => void): void;
    };
  }
}
