import { useEffect, useRef, useState } from "react";
import { Terminal } from "@xterm/xterm";
import { FitAddon } from "@xterm/addon-fit";
import "@xterm/xterm/css/xterm.css";
import { useAppStore } from "../store/appStore";

/**
 * Agent 터미널 패널 — SSF가 PTY로 실행하는 claude/codex CLI를 xterm.js로 연결.
 * "시작" 시 job을 생성(선택 소스를 data.db로 적재)하고 그 워크스페이스에서 에이전트를 띄운다.
 */
export function TerminalPanel() {
  const sources = useAppStore((s) => s.sources);
  const [agentKind, setAgentKind] = useState<"claude" | "codex">("claude");
  const [running, setRunning] = useState(false);
  const [status, setStatus] = useState("");

  const containerRef = useRef<HTMLDivElement>(null);
  const termRef = useRef<Terminal | null>(null);
  const fitRef = useRef<FitAddon | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const sessionRef = useRef<string | null>(null);
  const onResizeRef = useRef<(() => void) | null>(null);

  const cleanup = () => {
    if (onResizeRef.current) window.removeEventListener("resize", onResizeRef.current);
    onResizeRef.current = null;
    try { wsRef.current?.close(); } catch { /* noop */ }
    try { termRef.current?.dispose(); } catch { /* noop */ }
    wsRef.current = null;
    termRef.current = null;
    fitRef.current = null;
    sessionRef.current = null;
    setRunning(false);
  };

  // 언마운트 시 정리
  useEffect(() => cleanup, []);

  const start = async () => {
    setStatus("작업 준비 중...");
    try {
      const sourceIds = sources.map((s) => s.id);
      const job = await window.aidclaude.jobs.create("terminal session", sourceIds);
      useAppStore.getState().addJob(job);
      useAppStore.getState().setActiveJob(job.id);

      setRunning(true);
      setStatus("에이전트 시작 중...");

      // 컨테이너가 DOM에 보인 다음 터미널을 연다
      requestAnimationFrame(async () => {
        const el = containerRef.current;
        if (!el) return;
        const term = new Terminal({
          cursorBlink: true,
          fontFamily: "Consolas, 'D2Coding', monospace",
          fontSize: 13,
          theme: { background: "#1e1e1e", foreground: "#d4d4d4" },
        });
        const fit = new FitAddon();
        term.loadAddon(fit);
        term.open(el);
        fit.fit();
        termRef.current = term;
        fitRef.current = fit;

        try {
          const res = await window.aidclaude.agent.startLocal({
            agent: agentKind,
            workingDirectory: job.workspaceDir,
          });
          sessionRef.current = res.sessionId;

          const ws = new WebSocket(window.aidclaude.agent.wsUrl(res.sessionId, term.cols, term.rows));
          ws.binaryType = "arraybuffer";
          wsRef.current = ws;

          ws.onopen = () => { setStatus(""); term.focus(); };
          ws.onmessage = (e) => {
            if (typeof e.data === "string") term.write(e.data);
            else term.write(new Uint8Array(e.data as ArrayBuffer));
          };
          ws.onclose = () => { setStatus("세션이 종료되었습니다"); cleanup(); };
          ws.onerror = () => setStatus("WebSocket 오류");

          term.onData((d) => {
            if (ws.readyState === WebSocket.OPEN) ws.send(new TextEncoder().encode(d));
          });

          const onResize = () => {
            fit.fit();
            if (ws.readyState === WebSocket.OPEN) {
              ws.send(JSON.stringify({ type: "resize", cols: term.cols, rows: term.rows }));
            }
          };
          onResizeRef.current = onResize;
          window.addEventListener("resize", onResize);
        } catch (e) {
          setStatus("에이전트 시작 실패: " + (e as Error).message);
          cleanup();
        }
      });
    } catch (e) {
      setStatus("작업 생성 실패: " + (e as Error).message);
      setRunning(false);
    }
  };

  const stop = () => {
    if (sessionRef.current) void window.aidclaude.agent.killLocal(sessionRef.current);
    cleanup();
    setStatus("중지됨");
  };

  return (
    <div className="term-panel">
      <div className="term-header">
        <strong>{agentKind === "codex" ? "Codex" : "Claude Code"}</strong>
        {running && (
          <button className="term-btn term-btn-ghost" onClick={stop}>중지</button>
        )}
      </div>

      {!running && (
        <div className="term-form">
          <div className="term-row">
            <label>
              <input type="radio" name="agent-kind" value="claude"
                checked={agentKind === "claude"} onChange={() => setAgentKind("claude")} /> Claude Code
            </label>
            <label>
              <input type="radio" name="agent-kind" value="codex"
                checked={agentKind === "codex"} onChange={() => setAgentKind("codex")} /> Codex
            </label>
          </div>
          <div className="term-hint">
            선택한 데이터 소스({sources.length}개)를 작업 공간에 적재한 뒤 그 폴더에서 에이전트를 실행합니다.
          </div>
          <div className="term-actions">
            <button className="term-btn term-btn-primary" onClick={start}>터미널 시작</button>
            <span className="term-status">{status}</span>
          </div>
        </div>
      )}

      <div ref={containerRef} className="term-xterm" style={{ display: running ? "block" : "none" }} />
      {running && status && <div className="term-status term-status-bar">{status}</div>}
    </div>
  );
}
