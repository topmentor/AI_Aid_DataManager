import { useState, useEffect, useRef, useMemo, useCallback } from "react";
import Editor from "@monaco-editor/react";
import { useAppStore } from "../store/appStore";

function parseSqlOptions(sql: string): { title: string; sql: string }[] | null {
  const lines = sql.split("\n");
  const options: { title: string; sql: string }[] = [];
  let currentTitle = "";
  let currentLines: string[] = [];
  for (const line of lines) {
    const match = line.match(/^--\s*\[옵션\s*\d+\]\s*(.*)/);
    if (match) {
      if (currentTitle) {
        const s = currentLines.join("\n").trim();
        if (s) options.push({ title: currentTitle, sql: s });
      }
      currentTitle = match[1].trim() || `옵션 ${options.length + 1}`;
      currentLines = [];
    } else if (currentTitle) {
      currentLines.push(line);
    }
  }
  if (currentTitle) {
    const s = currentLines.join("\n").trim();
    if (s) options.push({ title: currentTitle, sql: s });
  }
  return options.length >= 2 ? options : null;
}

export function CodePanel() {
  const activeAnalyzeCode = useAppStore((s) => s.activeAnalyzeCode);
  const activeJobId = useAppStore((s) => s.activeJobId);

  const [running, setRunning] = useState(false);
  const [runError, setRunError] = useState<string | null>(null);
  const [saved, setSaved] = useState(true);
  const [runningOptionIdx, setRunningOptionIdx] = useState<number | null>(null);
  const saveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [histFiles, setHistFiles] = useState<string[]>([]);
  const [histOpen, setHistOpen] = useState(false);
  const histRef = useRef<HTMLDivElement>(null);


  const sqlOptions = useMemo(
    () => parseSqlOptions(activeAnalyzeCode ?? ""),
    [activeAnalyzeCode]
  );

  // 히스토리 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    if (!histOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (!histRef.current?.contains(e.target as Node)) setHistOpen(false);
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [histOpen]);

  // 작업 전환 시 query.sql 로드 (디스크에서)
  useEffect(() => {
    setSaved(true);
    const job = useAppStore.getState().jobs.find((j) => j.id === activeJobId);
    if (!job) {
      useAppStore.getState().setActiveCode("");
      return;
    }
    const filePath = job.workspaceDir.replace(/\\/g, "/") + "/query.sql";
    window.aidclaude.files
      .readText(filePath)
      .then((code) => useAppStore.getState().setActiveCode(code ?? ""))
      .catch(() => undefined);
  }, [activeJobId]);

  function getFilePath(): string | null {
    const job = useAppStore.getState().jobs.find((j) => j.id === activeJobId);
    return job ? job.workspaceDir.replace(/\\/g, "/") + "/query.sql" : null;
  }

  async function saveCode(code: string): Promise<void> {
    const fp = getFilePath();
    if (!fp) return;
    await window.aidclaude.files.writeText(fp, code);
    setSaved(true);
  }

  function handleChange(value: string | undefined) {
    const code = value ?? "";
    useAppStore.getState().setActiveCode(code);
    setSaved(false);
    // 자동 저장: 1초 후 디바운스
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(() => saveCode(code), 1000);
  }

  const handleOpenHistory = useCallback(async () => {
    if (!activeJobId) return;
    const files = await window.aidclaude.jobs.listQueryHistory(activeJobId);
    setHistFiles(files);
    setHistOpen(true);
  }, [activeJobId]);

  async function handleLoadHistory(filename: string) {
    if (!activeJobId) return;
    const job = useAppStore.getState().jobs.find((j) => j.id === activeJobId);
    if (!job) return;
    const histPath = job.workspaceDir.replace(/\\/g, "/") + "/history/" + filename;
    const code = await window.aidclaude.files.readText(histPath);
    if (code !== null) {
      useAppStore.getState().setActiveCode(code);
      setSaved(false);
      if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
      saveTimerRef.current = setTimeout(() => saveCode(code), 1000);
    }
    setHistOpen(false);
  }

  async function handleRun() {
    if (!activeJobId || running) return;
    setRunning(true);
    setRunError(null);
    // 실행 전 최신 코드 즉시 저장
    if (saveTimerRef.current) {
      clearTimeout(saveTimerRef.current);
      saveTimerRef.current = null;
    }
    await saveCode(activeAnalyzeCode ?? "").catch(() => undefined);
    try {
      const result = await window.aidclaude.jobs.runAnalysis(activeJobId);
      if (!result.ok && result.error) setRunError(result.error);
    } catch (e) {
      setRunError((e as Error).message);
    } finally {
      setRunning(false);
    }
  }

  async function handleRunOption(idx: number, sql: string) {
    if (!activeJobId || runningOptionIdx !== null) return;
    setRunningOptionIdx(idx);
    setRunError(null);
    try {
      const result = await window.aidclaude.jobs.runSql(activeJobId, sql);
      if (!result.ok && result.error) setRunError(result.error);
    } catch (e) {
      setRunError((e as Error).message);
    } finally {
      setRunningOptionIdx(null);
    }
  }

  const canRun = !!activeJobId && !running;

  return (
    <div className="code-panel">
      <div className="code-panel-header">
        <span className="code-panel-filename">
          query.sql
          {!saved && <span className="code-panel-unsaved"> ●</span>}
        </span>
        <div className="code-panel-actions">
          {/* 이전 쿼리 히스토리 드롭다운 */}
          <div ref={histRef} className="code-panel-hist-wrap">
            <button
              type="button"
              className="code-panel-hist-btn"
              disabled={!activeJobId}
              onClick={handleOpenHistory}
            >
              이전 쿼리 ▾
            </button>
            {histOpen && (
              <div className="code-panel-hist-dropdown">
                {histFiles.length === 0 ? (
                  <span className="code-panel-hist-empty">이전 쿼리 없음</span>
                ) : (
                  histFiles.map((f) => (
                    <button
                      key={f}
                      type="button"
                      className="code-panel-hist-item"
                      onClick={() => handleLoadHistory(f)}
                    >
                      {f}
                    </button>
                  ))
                )}
              </div>
            )}
          </div>
          {runError && (
            <span className="code-panel-error" title={runError}>
              {runError.slice(0, 60)}{runError.length > 60 ? "…" : ""}
            </span>
          )}
          {!sqlOptions && (
            <>
              <button
                type="button"
                className="code-panel-save-btn"
                disabled={saved || !activeJobId}
                onClick={() => saveCode(activeAnalyzeCode ?? "")}
              >
                저장
              </button>
              <button
                type="button"
                className={`code-panel-run-btn${running ? " code-panel-run-btn-busy" : ""}`}
                disabled={!canRun}
                onClick={handleRun}
              >
                {running ? "⏳ 실행 중…" : "▶ 실행"}
              </button>
            </>
          )}
        </div>
      </div>

      {sqlOptions && (
        <div className="code-panel-options">
          <span className="code-panel-options-label">옵션 선택 후 실행</span>
          {sqlOptions.map((opt, idx) => (
            <button
              key={idx}
              type="button"
              className={`code-panel-option-btn${runningOptionIdx === idx ? " code-panel-option-btn-busy" : ""}`}
              disabled={runningOptionIdx !== null || !activeJobId}
              onClick={() => handleRunOption(idx, opt.sql)}
            >
              {runningOptionIdx === idx ? "⏳ " : `▶ 옵션 ${idx + 1}. `}
              {opt.title}
            </button>
          ))}
        </div>
      )}
      <div className="code-panel-editor">
        <Editor
          height="100%"
          language="sql"
          theme="vs-dark"
          value={activeAnalyzeCode ?? ""}
          onChange={handleChange}
          options={{
            readOnly: false,
            minimap: { enabled: false },
            fontSize: 13,
            lineNumbers: "on",
            scrollBeyondLastLine: false,
            wordWrap: "on",
            tabSize: 4,
            insertSpaces: true,
          }}
        />
      </div>

    </div>
  );
}
