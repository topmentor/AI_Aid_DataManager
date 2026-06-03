import { useEffect, useRef, useState } from "react";
import { Terminal } from "@xterm/xterm";
import { FitAddon } from "@xterm/addon-fit";
import "@xterm/xterm/css/xterm.css";
import { useAppStore } from "../store/appStore";

type AgentKind = "claude" | "codex";

/**
 * Agent 터미널 패널 — SSF가 PTY로 실행하는 claude/codex CLI를 xterm.js로 연결.
 * "시작" 시 job을 생성(선택 소스를 data.db로 적재)하고 그 워크스페이스에서 에이전트를 띄운다.
 * CLI 미설치 시 설치 명령(편집 가능)을 터미널에서 실행할 수 있다.
 */
export function TerminalPanel() {
  const sources = useAppStore((s) => s.sources);
  const [agentKind, setAgentKind] = useState<AgentKind>("claude");
  const [running, setRunning] = useState(false);
  const [mode, setMode] = useState<"agent" | "install">("agent");
  const [status, setStatus] = useState("");
  const [installed, setInstalled] = useState<boolean | null>(null);
  const [installCmd, setInstallCmd] = useState("");

  const containerRef = useRef<HTMLDivElement>(null);
  const termRef = useRef<Terminal | null>(null);
  const fitRef = useRef<FitAddon | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const sessionRef = useRef<string | null>(null);
  const onResizeRef = useRef<(() => void) | null>(null);

  // 에이전트 선택 시 설치 여부 확인
  useEffect(() => {
    let alive = true;
    setInstalled(null);
    window.aida.agent
      .check(agentKind)
      .then((r) => { if (alive) { setInstalled(r.installed); setInstallCmd(r.installCommand); } })
      .catch(() => { if (alive) setInstalled(null); });
    return () => { alive = false; };
  }, [agentKind]);

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

  /** sessionId에 xterm + WS를 연결. */
  const attachTerminal = (sessionId: string, kind: "agent" | "install") => {
    requestAnimationFrame(() => {
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
      sessionRef.current = sessionId;

      const ws = new WebSocket(window.aida.agent.wsUrl(sessionId, term.cols, term.rows));
      ws.binaryType = "arraybuffer";
      wsRef.current = ws;

      ws.onopen = () => { setStatus(""); term.focus(); };
      ws.onmessage = (e) => {
        if (typeof e.data === "string") term.write(e.data);
        else term.write(new Uint8Array(e.data as ArrayBuffer));
      };
      ws.onclose = () => {
        cleanup();
        if (kind === "install") {
          setStatus("설치 세션 종료 — 설치 여부를 다시 확인하세요");
          window.aida.agent.check(agentKind).then((r) => setInstalled(r.installed)).catch(() => {});
        } else {
          setStatus("세션이 종료되었습니다");
        }
      };
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
    });
  };

  const start = async () => {
    setStatus("작업 준비 중...");
    try {
      const sourceIds = sources.map((s) => s.id);
      const job = await window.aida.jobs.create("terminal session", sourceIds);
      useAppStore.getState().addJob(job);
      useAppStore.getState().setActiveJob(job.id);

      setMode("agent");
      setRunning(true);
      setStatus("에이전트 시작 중...");
      const res = await window.aida.agent.startLocal({
        agent: agentKind,
        workingDirectory: job.workspaceDir,
      });
      attachTerminal(res.sessionId, "agent");
    } catch (e) {
      setStatus("시작 실패: " + (e as Error).message);
      setRunning(false);
    }
  };

  const install = async () => {
    const cmd = installCmd.trim();
    if (!cmd) { setStatus("설치 명령을 입력하세요"); return; }
    setStatus("설치 시작 중...");
    try {
      setMode("install");
      setRunning(true);
      const res = await window.aida.agent.installLocal({ agent: agentKind, command: cmd });
      attachTerminal(res.sessionId, "install");
    } catch (e) {
      setStatus("설치 실패: " + (e as Error).message);
      setRunning(false);
    }
  };

  const stop = () => {
    if (sessionRef.current) void window.aida.agent.killLocal(sessionRef.current);
    cleanup();
    setStatus("중지됨");
  };

  const agentLabel = agentKind === "codex" ? "Codex" : "Claude Code";

  return (
    <div className="term-panel">
      <div className="term-header">
        <strong>{agentLabel}{running && mode === "install" ? " — 설치" : ""}</strong>
        {running && <button className="term-btn term-btn-ghost" onClick={stop}>중지</button>}
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
            {installed === true && <span className="term-badge term-badge-ok">설치됨</span>}
            {installed === false && <span className="term-badge term-badge-no">미설치</span>}
          </div>

          <div className="term-hint">
            선택한 데이터 소스({sources.length}개)를 작업 공간에 적재한 뒤 그 폴더에서 {agentLabel}를 실행합니다.
          </div>

          <div className="term-actions">
            <button className="term-btn term-btn-primary" onClick={start}>터미널 시작</button>
            <span className="term-status">{status}</span>
          </div>

          {/* CLI 설치 (미설치이거나 확인 실패 시 노출) */}
          {installed !== true && (
            <div className="term-install">
              <div className="term-hint">
                {agentLabel} CLI가 없으면 아래 명령으로 설치합니다(필요 시 수정 가능):
              </div>
              <input
                className="term-install-input"
                value={installCmd}
                onChange={(e) => setInstallCmd(e.target.value)}
                placeholder="예: npm install -g @anthropic-ai/claude-code"
              />
              <button className="term-btn" onClick={install}>설치 실행</button>
            </div>
          )}
        </div>
      )}

      <div ref={containerRef} className="term-xterm" style={{ display: running ? "block" : "none" }} />
      {running && status && <div className="term-status term-status-bar">{status}</div>}
    </div>
  );
}
