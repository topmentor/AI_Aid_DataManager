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
      };
      claude: {
        probe(): Promise<ClaudeEnvProbe>;
        sendMessage(jobId: string, message: string): Promise<void>;
        abort(jobId: string): Promise<void>;
      };
      jobs: {
        create(userRequest: string, sourceIds: string[]): Promise<Job>;
        list(): Promise<Job[]>;
      };
      files: {
        open(fp: string): Promise<void>;
        readText(fp: string): Promise<string>;
      };
      on(channel: string, fn: (...args: unknown[]) => void): void;
      off(channel: string, fn: (...args: unknown[]) => void): void;
    };
  }
}
