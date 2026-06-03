import { useState } from "react";
import Editor from "@monaco-editor/react";
import { useAppStore } from "../store/appStore";

/**
 * SQL 에디터 패널 — 입력한 조회 SQL을 활성 작업의 data.db에 실행.
 * 결과는 `result` 임시 테이블로 생성하고 CenterPanel에 result 탭을 연다.
 */
export function SqlPanel() {
  const activeJobId = useAppStore((s) => s.activeJobId);
  const sources = useAppStore((s) => s.sources);
  const [sql, setSql] = useState("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;");
  const [status, setStatus] = useState("");
  const [running, setRunning] = useState(false);

  // 활성 작업이 없으면 전체 소스를 적재한 작업을 생성
  const ensureJob = async (): Promise<string> => {
    if (activeJobId) return activeJobId;
    const job = await window.aida.jobs.create("sql session", sources.map((s) => s.id));
    useAppStore.getState().addJob(job);
    useAppStore.getState().setActiveJob(job.id);
    return job.id;
  };

  const run = async () => {
    const q = sql.trim().replace(/;\s*$/, "");
    if (!q) return;
    setRunning(true);
    setStatus("실행 중...");
    try {
      const jobId = await ensureJob();
      // 입력 쿼리를 result 임시 테이블로 materialize
      const script = `DROP TABLE IF EXISTS result;\nCREATE TABLE result AS\n${q};`;
      const res = await window.aida.jobs.runSql(jobId, script);
      if (!res.ok) {
        setStatus("오류: " + (res.error ?? "실행 실패"));
        return;
      }
      const r = await window.aida.db.previewTable(jobId, "result", 500);
      if (r.headers.length > 0) {
        useAppStore.getState().openCenterTab({
          id: `db:${jobId}:result`,
          title: "result",
          headers: r.headers,
          rows: r.rows,
          sourceRef: { kind: "db", jobId, tableName: "result" },
          fullyLoaded: r.rows.length < 500,
        });
        setStatus(`완료 — result ${r.rows.length}행`);
      } else {
        setStatus("완료 — result 테이블(0행)");
      }
    } catch (e) {
      setStatus("실패: " + (e as Error).message);
    } finally {
      setRunning(false);
    }
  };

  const onKeyDown = (e: React.KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === "Enter") { e.preventDefault(); void run(); }
  };

  return (
    <div className="sql-panel" onKeyDown={onKeyDown}>
      <div className="sql-header">
        <strong>SQL</strong>
        <span className="sql-hint">입력한 조회 결과가 <code>result</code> 테이블로 생성됩니다 (Ctrl+Enter)</span>
        <button className="sql-run" onClick={run} disabled={running}>실행</button>
      </div>
      <div className="sql-editor">
        <Editor
          height="100%"
          defaultLanguage="sql"
          value={sql}
          onChange={(v) => setSql(v ?? "")}
          theme="vs-dark"
          options={{ minimap: { enabled: false }, fontSize: 13, lineNumbers: "on", scrollBeyondLastLine: false, automaticLayout: true }}
        />
      </div>
      {status && <div className="sql-status">{status}</div>}
    </div>
  );
}
