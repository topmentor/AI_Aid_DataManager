import { DataSourcePanel } from "./DataSourcePanel";
import { ChatPanel } from "./ChatPanel";
import { CodePanel } from "./CodePanel";

// Temporary placeholder — will be replaced in Task 12
function ResultPanel() {
  return (
    <div style={{ padding: 12, color: "#555", fontSize: 12 }}>
      결과 패널 (Task 12에서 구현)
    </div>
  );
}

export function ProjectWindow() {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "240px 1fr 320px",
        height: "100vh",
        overflow: "hidden",
      }}
    >
      {/* Left: Data Source Panel */}
      <div style={{ borderRight: "1px solid #333", overflowY: "auto", background: "#252525" }}>
        <DataSourcePanel />
      </div>

      {/* Center: Chat (top) + Code (bottom) */}
      <div style={{ display: "grid", gridTemplateRows: "55% 45%", overflow: "hidden" }}>
        <ChatPanel />
        <CodePanel />
      </div>

      {/* Right: Results Panel */}
      <div style={{ borderLeft: "1px solid #333", overflowY: "auto" }}>
        <ResultPanel />
      </div>
    </div>
  );
}
