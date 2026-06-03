// main 프로세스에서 SSF 백엔드(/api/*.do)를 호출하는 경량 클라이언트.
// Claude 턴 전후 오케스트레이션(소스 갱신, SQL 옵션 조회, 분석 실행)에 사용.

interface Envelope {
  result: string;
  msg?: string;
  resultMap?: Record<string, unknown>;
  resultList?: unknown[];
}

async function call(serverUrl: string, path: string, body?: unknown): Promise<Envelope> {
  const init: RequestInit = body === undefined
    ? { method: "GET" }
    : { method: "POST", headers: { "Content-Type": "application/json; charset=utf-8" }, body: JSON.stringify(body) };
  const res = await fetch(`${serverUrl}/api/${path}`, init);
  if (!res.ok) throw new Error(`HTTP ${res.status} (${path})`);
  return (await res.json()) as Envelope;
}

export interface SsfJob {
  id: string;
  userRequest: string;
  status: string;
  errorMsg?: string;
  workspaceDir: string;
  createdAt: number;
}

export function createSsfClient(serverUrl: string) {
  return {
    async getSettings(): Promise<Record<string, string>> {
      const e = await call(serverUrl, "getSettings.do");
      return (e.resultMap ?? {}) as Record<string, string>;
    },
    async getJob(jobId: string): Promise<SsfJob | null> {
      const e = await call(serverUrl, "listJobs.do");
      const list = (e.resultList ?? []) as SsfJob[];
      return list.find((j) => j.id === jobId) ?? null;
    },
    async refreshJobSources(jobId: string): Promise<void> {
      await call(serverUrl, "refreshJobSources.do", { jobId });
    },
    async getSqlOptions(jobId: string): Promise<{ title: string; sql: string }[]> {
      const e = await call(serverUrl, "getSqlOptions.do", { jobId });
      return (e.resultList ?? []) as { title: string; sql: string }[];
    },
    async runJobAnalysis(jobId: string): Promise<{ ok: boolean; error?: string }> {
      const e = await call(serverUrl, "runJobAnalysis.do", { jobId });
      const m = (e.resultMap ?? {}) as { ok?: boolean; error?: string };
      return { ok: !!m.ok, error: m.error };
    },
  };
}

export type SsfClient = ReturnType<typeof createSsfClient>;
