// SSF 백엔드(/api/*.do) HTTP 클라이언트 — preload의 데이터 네임스페이스 구현.
// electron 비의존(노드 fetch). SSF 봉투 {result,resultMap,resultList,...}를
// 기존 window.aidclaude.* 반환 형태로 언랩한다.

interface SsfEnvelope {
  result: string;
  msg?: string;
  resultMap?: Record<string, unknown>;
  resultList?: unknown[];
  count?: number;
}

async function call(serverUrl: string, path: string, body?: unknown): Promise<SsfEnvelope> {
  const init: RequestInit = body === undefined
    ? { method: "GET" }
    : { method: "POST", headers: { "Content-Type": "application/json; charset=utf-8" }, body: JSON.stringify(body) };
  const res = await fetch(`${serverUrl}/api/${path}`, init);
  if (!res.ok) throw new Error(`HTTP ${res.status} (${path})`);
  return (await res.json()) as SsfEnvelope;
}

function okMap(e: SsfEnvelope): Record<string, unknown> {
  if (e.result !== "OK") throw new Error(e.msg || "요청 실패");
  return e.resultMap ?? {};
}
function okList(e: SsfEnvelope): unknown[] {
  if (e.result !== "OK") throw new Error(e.msg || "요청 실패");
  return e.resultList ?? [];
}

function mapJob(j: Record<string, unknown>): Record<string, unknown> {
  return {
    id: j.id,
    createdAt: j.createdAt == null ? "" : String(j.createdAt),
    userRequest: j.userRequest ?? "",
    status: j.status ?? "idle",
    workspaceDir: j.workspaceDir ?? "",
    outputFiles: [],
    errorMsg: j.errorMsg == null ? undefined : String(j.errorMsg),
  };
}

export function createApiClient(serverUrl: string) {
  return {
    settings: {
      get: async () => okMap(await call(serverUrl, "getSettings.do")),
      set: async (s: unknown) => okMap(await call(serverUrl, "setSettings.do", s)),
    },
    catalog: {
      list: async () => okList(await call(serverUrl, "listSources.do")),
      add: async (ds: { name: string; type: string; config: unknown }) =>
        okMap(await call(serverUrl, "addSource.do", ds)),
      update: async (ds: { id: string; name: string; type: string; config: unknown }) => {
        await call(serverUrl, "updateSource.do", ds);
      },
      remove: async (id: string) => {
        await call(serverUrl, "removeSource.do", { id });
      },
      testConnection: async (id: string) => okMap(await call(serverUrl, "testConnection.do", { id })),
      getSchema: async (id: string) => {
        const m = okMap(await call(serverUrl, "getSchema.do", { id }));
        if (m.sourceId == null) m.sourceId = id;
        return m;
      },
      previewData: async (id: string, limit?: number) =>
        okMap(await call(serverUrl, "previewData.do", { id, limit: limit ?? 50 })),
    },
    jobs: {
      create: async (userRequest: string, sourceIds: string[]) =>
        mapJob(okMap(await call(serverUrl, "createJob.do", { userRequest, sourceIds }))),
      list: async () => (okList(await call(serverUrl, "listJobs.do")) as Record<string, unknown>[]).map(mapJob),
      runAnalysis: async (jobId: string) => okMap(await call(serverUrl, "runJobAnalysis.do", { jobId })),
      runSql: async (jobId: string, sql: string) => okMap(await call(serverUrl, "runJobSql.do", { jobId, sql })),
      refreshSources: async (jobId: string) => {
        await call(serverUrl, "refreshJobSources.do", { jobId });
      },
      getSqlOptions: async (jobId: string) => okList(await call(serverUrl, "getSqlOptions.do", { jobId })),
      listQueryHistory: async (jobId: string) => okList(await call(serverUrl, "listQueryHistory.do", { jobId })),
      listAllOrphanTables: async () => okList(await call(serverUrl, "listOrphanTables.do")),
      dropAllOrphanTables: async () => {
        const m = okMap(await call(serverUrl, "dropOrphanTables.do", {}));
        return { ok: true, dropped: Number(m.dropped ?? 0) };
      },
    },
    data: {
      saveAsSource: async (sourceName: string, headers: string[], rows: string[][]) => {
        const e = await call(serverUrl, "saveDataAsSource.do", { sourceName, headers, rows });
        if (e.result !== "OK") return { ok: false, error: e.msg };
        return { ok: true, source: e.resultMap };
      },
    },
    db: {
      listTables: async (jobId: string) => okList(await call(serverUrl, "listTables.do", { jobId })),
      previewTable: async (jobId: string, tableName: string, limit?: number) =>
        okMap(await call(serverUrl, "previewTable.do", { jobId, tableName, limit: limit ?? 500 })),
      saveAsSource: async (jobId: string, tableName: string, sourceName: string) => {
        const e = await call(serverUrl, "saveTableAsSource.do", { jobId, tableName, sourceName });
        if (e.result !== "OK") return { ok: false, error: e.msg };
        return { ok: true, source: e.resultMap };
      },
    },
    // files: read*/copy* 만 HTTP (open/export/dialog은 IPC 유지)
    filesHttp: {
      readText: async (fp: string): Promise<string | null> => {
        const m = okMap(await call(serverUrl, "readText.do", { path: fp }));
        return m.content == null ? null : String(m.content);
      },
      writeText: async (fp: string, content: string) => {
        await call(serverUrl, "writeText.do", { path: fp, content });
      },
      readLines: async (fp: string, count: number) =>
        okList(await call(serverUrl, "readLines.do", { path: fp, count })) as string[],
      readBase64: async (fp: string): Promise<string> => {
        const m = okMap(await call(serverUrl, "readBase64.do", { path: fp }));
        return String(m.base64 ?? "");
      },
      copyToData: async (srcPath: string): Promise<string> => {
        const m = okMap(await call(serverUrl, "copyToData.do", { srcPath }));
        return String(m.path ?? "");
      },
      copyShapefile: async (srcShpPath: string) =>
        okMap(await call(serverUrl, "copyShapefile.do", { srcShpPath })),
    },
    // Agent PTY 터미널 (claude/codex) — SSF의 /api/agent-pty + /ws/agent-pty
    agent: {
      startLocal: async (opts: { agent: string; workingDirectory: string }) => {
        const body = new URLSearchParams({
          agent: opts.agent,
          workingDirectory: opts.workingDirectory,
        }).toString();
        const res = await fetch(`${serverUrl}/api/agent-pty/local`, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded;charset=utf-8" },
          body,
        });
        const json = (await res.json()) as {
          sessionId?: string; agent?: string; command?: string; workingDirectory?: string; error?: string;
        };
        if (!res.ok) throw new Error(json.error || `HTTP ${res.status}`);
        return json as { sessionId: string; agent: string; command: string; workingDirectory: string };
      },
      killLocal: async (sessionId: string) => {
        await fetch(`${serverUrl}/api/agent-pty/local/${encodeURIComponent(sessionId)}/kill`, {
          method: "POST",
        }).catch(() => undefined);
      },
      wsUrl: (sessionId: string, cols: number, rows: number) => {
        const wsBase = serverUrl.replace(/^http/i, "ws");
        return `${wsBase}/ws/agent-pty/local/${encodeURIComponent(sessionId)}?cols=${cols}&rows=${rows}`;
      },
    },
  };
}

export type ApiClient = ReturnType<typeof createApiClient>;
