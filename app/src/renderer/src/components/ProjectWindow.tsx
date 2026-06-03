import { useEffect, useRef, useState } from "react";
import { DataSourcePanel } from "./DataSourcePanel";
import { TerminalPanel } from "./TerminalPanel";
import { SqlPanel } from "./SqlPanel";
import { CenterPanel } from "./CenterPanel";
import { SettingsModal } from "./SettingsModal";
import { useAppStore } from "../store/appStore";

// ── Drag utilities ──────────────────────────────────────────────────────────

function colDrag(
  e: React.MouseEvent, startSize: number, direction: 1 | -1,
  min: number, max: number, setSize: (n: number) => void
) {
  e.preventDefault();
  const startX = e.clientX;
  document.body.style.cursor = "col-resize";
  document.body.style.userSelect = "none";
  const onMove = (ev: MouseEvent) => setSize(Math.max(min, Math.min(max, startSize + (ev.clientX - startX) * direction)));
  const onUp = () => {
    document.body.style.cursor = ""; document.body.style.userSelect = "";
    window.removeEventListener("mousemove", onMove); window.removeEventListener("mouseup", onUp);
  };
  window.addEventListener("mousemove", onMove); window.addEventListener("mouseup", onUp);
}

function rowDrag(
  e: React.MouseEvent, containerEl: HTMLElement | null,
  startRatio: number, min: number, max: number, setRatio: (r: number) => void
) {
  e.preventDefault();
  const startY = e.clientY;
  const h = containerEl?.offsetHeight ?? 600;
  document.body.style.cursor = "row-resize";
  document.body.style.userSelect = "none";
  const onMove = (ev: MouseEvent) => setRatio(Math.max(min, Math.min(max, startRatio + (ev.clientY - startY) / h)));
  const onUp = () => {
    document.body.style.cursor = ""; document.body.style.userSelect = "";
    window.removeEventListener("mousemove", onMove); window.removeEventListener("mouseup", onUp);
  };
  window.addEventListener("mousemove", onMove); window.addEventListener("mouseup", onUp);
}

function VHandle({ onMouseDown }: { onMouseDown: (e: React.MouseEvent) => void }) {
  return <div className="pw-vhandle" onMouseDown={onMouseDown}><span className="pw-grip pw-vgrip" /></div>;
}
function HHandle({ onMouseDown }: { onMouseDown: (e: React.MouseEvent) => void }) {
  return <div className="pw-hhandle" onMouseDown={onMouseDown}><span className="pw-grip pw-hgrip" /></div>;
}

// ── ProjectWindow ────────────────────────────────────────────────────────────

export function ProjectWindow() {
  const [leftW, setLeftW] = useState(240);
  const [rightW, setRightW] = useState(420);
  const [termRatio, setTermRatio] = useState(0.6);
  const rightRef = useRef<HTMLDivElement>(null);
  const settingsOpen = useAppStore((s) => s.settingsOpen);
  const setSettings = useAppStore((s) => s.setSettings);
  const setSettingsOpen = useAppStore((s) => s.setSettingsOpen);

  // 설정을 store에 로드(폰트 등 사용)
  useEffect(() => {
    window.aida.settings.get().then(setSettings).catch(() => {});
  }, [setSettings]);

  return (
    <div
      className="pw-root"
      style={{ "--pw-left-w": `${leftW}px`, "--pw-right-w": `${rightW}px` } as React.CSSProperties}
    >
      {/* ── Left: DataSource + 설정 ── */}
      <div className="pw-left">
        <DataSourcePanel />
        <button className="pw-settings-btn" onClick={() => setSettingsOpen(true)} title="설정">
          ⚙ 설정
        </button>
      </div>

      <VHandle onMouseDown={(e) => colDrag(e, leftW, 1, 160, 480, setLeftW)} />

      {/* ── Center: Content tabs ── */}
      <div className="pw-center">
        <CenterPanel />
      </div>

      <VHandle onMouseDown={(e) => colDrag(e, rightW, -1, 280, 720, setRightW)} />

      {/* ── Right: Agent terminal (top) + SQL editor (bottom) ── */}
      <div
        className="pw-right"
        ref={rightRef}
        style={{ "--pw-term-ratio": `${termRatio * 100}%` } as React.CSSProperties}
      >
        <TerminalPanel />
        <HHandle onMouseDown={(e) => rowDrag(e, rightRef.current, termRatio, 0.2, 0.85, setTermRatio)} />
        <SqlPanel />
      </div>

      {settingsOpen && <SettingsModal />}
    </div>
  );
}
