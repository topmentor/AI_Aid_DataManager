import type {
  DataSource,
  DataSourceSchema,
  Job,
  AppSettings,
} from "../../shared/types";

declare global {
  interface Window {
    aida: {
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
      agent: {
        startLocal(opts: { agent: string; workingDirectory: string }): Promise<{
          sessionId: string; agent: string; command: string; workingDirectory: string;
        }>;
        killLocal(sessionId: string): Promise<void>;
        wsUrl(sessionId: string, cols: number, rows: number): string;
        check(agent: string): Promise<{ installed: boolean; binary: string; installCommand: string }>;
        installLocal(opts: { agent: string; command?: string; workingDirectory?: string }): Promise<{ sessionId: string }>;
      };
      jobs: {
        create(userRequest: string, sourceIds: string[]): Promise<Job>;
        list(): Promise<Job[]>;
        runAnalysis(jobId: string): Promise<{ ok: boolean; error?: string }>;
        runSql(jobId: string, sql: string): Promise<{ ok: boolean; error?: string }>;
        refreshSources(jobId: string): Promise<void>;
        getSqlOptions(jobId: string): Promise<{ title: string; sql: string }[]>;
        listQueryHistory(jobId: string): Promise<string[]>;
        listAllOrphanTables(): Promise<{ jobId: string; jobLabel: string; tables: string[] }[]>;
        dropAllOrphanTables(): Promise<{ ok: boolean; dropped: number }>;
      };
      data: {
        saveAsSource(sourceName: string, headers: string[], rows: string[][]): Promise<{ ok: boolean; source?: DataSource; error?: string }>;
      };
      sql: {
        listHistory(limit?: number): Promise<{ id: number; sql: string; createdAt: number }[]>;
        addHistory(sql: string): Promise<void>;
        clearHistory(): Promise<void>;
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
        openDirectory(): Promise<string | null>;
      };
      fonts: {
        list(): Promise<string[]>;
      };
    };
  }
}
