import { useState, useEffect } from "react";
import { useAppStore } from "../store/appStore";
import type { OutputFile, Job } from "../../../shared/types";

function fmtSize(b: number): string {
  return b < 1024 ? `${b} B` : `${Math.round(b / 1024)} KB`;
}

export function ResultPanel() {
  const { activeJobId, jobs, updateJob } = useAppStore();
  const job = jobs.find(j => j.id === activeJobId);
  const [activeTab, setActiveTab] = useState<"files" | "table" | "chart">("files");
  const [tableData, setTableData] = useState<{ headers: string[]; rows: string[][] } | null>(null);
  const [chartSrc, setChartSrc] = useState<string | null>(null);

  useEffect(() => {
    setTableData(null);
    setChartSrc(null);
    setActiveTab("files");
  }, [activeJobId]);

  useEffect(() => {
    if (!activeJobId) return;
    const handler = (...args: unknown[]) => {
      const updated = args[0] as Job;
      if (updated.id === activeJobId) updateJob(updated);
    };
    window.aidclaude.on(`job:update`, handler);
    return () => window.aidclaude.off(`job:update`, handler);
  }, [activeJobId, updateJob]);

  async function handleOpenFile(f: OutputFile) {
    if (f.type === "csv") {
      const text = await window.aidclaude.files.readText(f.path);
      const lines = text.trim().split("\n");
      if (lines.length === 0) return;
      const headers = lines[0].split(",").map(h => h.trim().replace(/^"|"$/g, ""));
      const rows = lines.slice(1).map(l => l.split(",").map(c => c.trim().replace(/^"|"$/g, "")));
      setTableData({ headers, rows });
      setActiveTab("table");
    } else if (f.type === "png") {
      const b64 = await window.aidclaude.files.readBase64(f.path);
      setChartSrc(`data:image/png;base64,${b64}`);
      setActiveTab("chart");
    } else {
      window.aidclaude.files.open(f.path);
    }
  }

  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column", fontSize: 13 }}>
      <div style={{ padding: "6px 12px", borderBottom: "1px solid #333", display: "flex", gap: 8 }}>
        {(["files", "table", "chart"] as const).map(tab => (
          <button key={tab} style={{ background: activeTab === tab ? "#0e639c" : "#333", padding: "2px 10px", fontSize: 12 }}
            onClick={() => setActiveTab(tab)}>
            {tab === "files" ? "파일" : tab === "table" ? "표" : "차트"}
          </button>
        ))}
        {job && <span style={{ marginLeft: "auto", color: "#888", fontSize: 11 }}>{job.status}</span>}
      </div>

      <div style={{ flex: 1, overflowY: "auto", padding: 12 }}>
        {activeTab === "files" && (
          <div>
            {(!job?.outputFiles?.length) && <p style={{ color: "#666" }}>결과 파일이 없습니다.</p>}
            {job?.outputFiles?.map(f => (
              <div key={f.name} style={{ display: "flex", justifyContent: "space-between", padding: "8px 0", borderBottom: "1px solid #333" }}>
                <span>{f.name} <span style={{ color: "#666" }}>({fmtSize(f.sizeBytes)})</span></span>
                <button style={{ fontSize: 11, padding: "2px 8px" }} onClick={() => handleOpenFile(f)}>열기</button>
              </div>
            ))}
          </div>
        )}

        {activeTab === "table" && tableData && (
          <div style={{ overflowX: "auto" }}>
            <table style={{ borderCollapse: "collapse", fontSize: 12, width: "100%" }}>
              <thead>
                <tr>{tableData.headers.map((h, i) => <th key={i} style={{ padding: "4px 8px", background: "#333", border: "1px solid #555" }}>{h}</th>)}</tr>
              </thead>
              <tbody>
                {tableData.rows.slice(0, 200).map((row, i) => (
                  <tr key={i} style={{ background: i % 2 === 0 ? "#1e1e1e" : "#252525" }}>
                    {row.map((cell, j) => <td key={j} style={{ padding: "3px 8px", border: "1px solid #444" }}>{cell}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
            {tableData.rows.length > 200 && <p style={{ color: "#888", marginTop: 8 }}>200행 이상은 파일로 다운로드하세요.</p>}
          </div>
        )}

        {activeTab === "table" && !tableData && (
          <p style={{ color: "#666" }}>CSV 파일을 선택하면 표가 표시됩니다.</p>
        )}

        {activeTab === "chart" && chartSrc && (
          <img src={chartSrc} alt="chart" style={{ maxWidth: "100%", borderRadius: 4 }} />
        )}

        {activeTab === "chart" && !chartSrc && (
          <p style={{ color: "#666" }}>PNG 파일을 선택하면 차트가 표시됩니다.</p>
        )}
      </div>
    </div>
  );
}
