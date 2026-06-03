import { useState } from "react";
import { DataSourcePanel } from "./DataSourcePanel";
import { TerminalPanel } from "./TerminalPanel";
import { CenterPanel } from "./CenterPanel";

// ── Drag utilities ──────────────────────────────────────────────────────────

function colDrag(
  e: React.MouseEvent,
  startSize: number,
  direction: 1 | -1,
  min: number,
  max: number,
  setSize: (n: number) => void
) {
  e.preventDefault();
  const startX = e.clientX;
  document.body.style.cursor = "col-resize";
  document.body.style.userSelect = "none";

  const onMove = (ev: MouseEvent) => {
    const next = startSize + (ev.clientX - startX) * direction;
    setSize(Math.max(min, Math.min(max, next)));
  };
  const onUp = () => {
    document.body.style.cursor = "";
    document.body.style.userSelect = "";
    window.removeEventListener("mousemove", onMove);
    window.removeEventListener("mouseup", onUp);
  };
  window.addEventListener("mousemove", onMove);
  window.addEventListener("mouseup", onUp);
}

function VHandle({ onMouseDown }: { onMouseDown: (e: React.MouseEvent) => void }) {
  return (
    <div className="pw-vhandle" onMouseDown={onMouseDown}>
      <span className="pw-grip pw-vgrip" />
    </div>
  );
}

// ── ProjectWindow ────────────────────────────────────────────────────────────

export function ProjectWindow() {
  const [leftW, setLeftW] = useState(240);
  const [rightW, setRightW] = useState(420);

  return (
    <div
      className="pw-root"
      style={{ "--pw-left-w": `${leftW}px`, "--pw-right-w": `${rightW}px` } as React.CSSProperties}
    >
      {/* ── Left: DataSource ── */}
      <div className="pw-left">
        <DataSourcePanel />
      </div>

      <VHandle onMouseDown={(e) => colDrag(e, leftW, 1, 160, 480, setLeftW)} />

      {/* ── Center: Content tabs ── */}
      <div className="pw-center">
        <CenterPanel />
      </div>

      <VHandle onMouseDown={(e) => colDrag(e, rightW, -1, 280, 720, setRightW)} />

      {/* ── Right: Agent terminal ── */}
      <div className="pw-right">
        <TerminalPanel />
      </div>
    </div>
  );
}
