import { useState, useEffect, useRef } from "react";
import {
  Chart as ChartJS,
  CategoryScale, LinearScale,
  BarElement, LineElement, PointElement,
  Title, Tooltip, Legend,
} from "chart.js";
import { Bar, Line, Scatter } from "react-chartjs-2";
import { useAppStore } from "../store/appStore";
import type { Job } from "../../../shared/types";
import { exportCSV, exportJSON, exportChartPNG } from "../utils/exportUtils";

ChartJS.register(
  CategoryScale, LinearScale,
  BarElement, LineElement, PointElement,
  Title, Tooltip, Legend
);

// ── TableView grid ──────────────────────────────────────────────────────────

function TableView({
  headers, rows, truncateAt = 500,
}: {
  headers: string[];
  rows: string[][];
  truncateAt?: number;
}) {
  const visible = rows.slice(0, truncateAt);
  return (
    <div>
      <div className="tv-scroll">
        <table className="tv-table">
          <thead>
            <tr>
              <th className="tv-th-rownum">#</th>
              {headers.map((h, i) => <th key={i} className="tv-th">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {visible.map((row, i) => (
              <tr key={i} className={i % 2 === 0 ? "tv-row-even" : "tv-row-odd"}>
                <td className="tv-td-rownum">{i + 1}</td>
                {row.map((cell, j) => <td key={j} className="tv-td">{cell}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {rows.length > truncateAt && (
        <p className="tv-truncated">{rows.length.toLocaleString()}행 중 {truncateAt}행 표시</p>
      )}
    </div>
  );
}

// ── TableChart (Chart.js) ───────────────────────────────────────────────────

const CHART_COLORS = [
  "rgba(14,99,156,0.75)", "rgba(91,164,207,0.75)", "rgba(77,188,137,0.75)",
  "rgba(217,119,87,0.75)", "rgba(168,100,200,0.75)", "rgba(229,192,75,0.75)",
];

type ChartType = "bar" | "line" | "scatter";

function TableChart({ headers, rows, tableName }: {
  headers: string[];
  rows: string[][];
  tableName: string;
}) {
  const [xCol, setXCol]   = useState(headers[0] ?? "");
  const [yCols, setYCols] = useState<string[]>(headers.slice(1).filter((_, i) => i < 3));
  const [chartType, setChartType] = useState<ChartType>("bar");
  const [maxRows, setMaxRows]     = useState(100);
  const canvasWrapRef = useRef<HTMLDivElement>(null);

  const sample = rows.slice(0, maxRows);
  const xIdx   = headers.indexOf(xCol);

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: {
      legend: { labels: { color: "#c8c8c8", boxWidth: 12 } },
      title:  { display: true, text: tableName, color: "#aaa", font: { size: 12 } },
    },
    scales: {
      x: { ticks: { color: "#888", maxRotation: 45 }, grid: { color: "#2a2a2a" } },
      y: { ticks: { color: "#888" }, grid: { color: "#2a2a2a" } },
    },
  };

  function toggleY(col: string) {
    setYCols(yCols.includes(col) ? yCols.filter(c => c !== col) : [...yCols, col]);
  }

  async function handleExportPNG() {
    const canvas = canvasWrapRef.current?.querySelector("canvas") as HTMLCanvasElement | null;
    if (!canvas) return;
    await exportChartPNG(tableName, canvas);
  }

  const labels = sample.map(r => r[xIdx] ?? "");
  const datasets = yCols.map((yc, ci) => {
    const yi = headers.indexOf(yc);
    return {
      label: yc,
      data: chartType === "scatter"
        ? sample.map(r => ({ x: parseFloat(r[xIdx]) || 0, y: parseFloat(r[yi]) || 0 }))
        : sample.map(r => parseFloat(r[yi]) || 0),
      backgroundColor: CHART_COLORS[ci % CHART_COLORS.length],
      borderColor:     CHART_COLORS[ci % CHART_COLORS.length].replace("0.75", "1"),
      borderWidth: 1,
    };
  });

  return (
    <div>
      <div className="rp-chart-cfg">
        <div className="rp-chart-cfg-row">
          <label className="rp-chart-cfg-label">차트
            <select className="rp-chart-cfg-sel" value={chartType}
              onChange={e => setChartType(e.target.value as ChartType)}>
              <option value="bar">막대</option>
              <option value="line">선</option>
              <option value="scatter">산점도</option>
            </select>
          </label>
          <label className="rp-chart-cfg-label">X축
            <select className="rp-chart-cfg-sel" value={xCol}
              onChange={e => setXCol(e.target.value)}>
              {headers.map(h => <option key={h} value={h}>{h}</option>)}
            </select>
          </label>
          <label className="rp-chart-cfg-label">최대행
            <select className="rp-chart-cfg-sel" value={maxRows}
              onChange={e => setMaxRows(Number(e.target.value))}>
              {[50, 100, 200, 500].map(n => (
                <option key={n} value={n}>{n}행</option>
              ))}
            </select>
          </label>
          <button type="button" className="cp-export-btn" onClick={handleExportPNG}
            title="차트를 PNG로 저장">
            ↓ PNG
          </button>
        </div>
        <div className="rp-chart-cfg-row">
          <span className="rp-chart-cfg-label">Y축</span>
          <div className="rp-chart-ycols">
            {headers.map(h => (
              <button
                type="button" key={h}
                className={`rp-chart-ycol-btn${yCols.includes(h) ? " rp-chart-ycol-btn-on" : ""}`}
                onClick={() => toggleY(h)}
              >
                {h}
              </button>
            ))}
          </div>
        </div>
      </div>
      <div className="rp-chart-canvas" ref={canvasWrapRef}>
        {yCols.length > 0 ? (
          chartType === "scatter" ? (
            <Scatter data={{ datasets }} options={chartOptions} />
          ) : chartType === "bar" ? (
            <Bar data={{ labels, datasets }} options={chartOptions} />
          ) : (
            <Line data={{ labels, datasets }} options={chartOptions} />
          )
        ) : (
          <p className="rp-empty">Y축 컬럼을 하나 이상 선택하세요.</p>
        )}
      </div>
    </div>
  );
}

// ── ResultPanel ─────────────────────────────────────────────────────────────

export function ResultPanel() {
  const { activeJobId, jobs, updateJob, sources, setSources } = useAppStore();
  const job = jobs.find(j => j.id === activeJobId);

  const [activeTab, setActiveTab] = useState<"db" | "table" | "chart">("db");
  const [dbTables,  setDbTables]  = useState<string[]>([]);
  const [dbPreview, setDbPreview] = useState<{
    title: string; headers: string[]; rows: string[][];
  } | null>(null);
  const [dbLoading, setDbLoading] = useState(false);
  const [selectedTable, setSelectedTable] = useState<string | null>(null);

  // 데이터 소스로 저장
  const [savingAs, setSavingAs]   = useState(false);
  const [saveName, setSaveName]   = useState("");
  const [saveBusy, setSaveBusy]   = useState(false);
  const [saveMsg,  setSaveMsg]    = useState<string | null>(null);

  async function loadDbTables(jobId: string) {
    try {
      const tables = await window.aidclaude.db.listTables(jobId);
      setDbTables(tables);
    } catch {
      setDbTables([]);
    }
  }

  useEffect(() => {
    setDbPreview(null);
    setDbTables([]);
    setSelectedTable(null);
    setActiveTab("db");
    if (activeJobId) loadDbTables(activeJobId);
  }, [activeJobId]);

  useEffect(() => {
    if ((job?.status === "done" || job?.status === "error") && activeJobId) {
      loadDbTables(activeJobId);
    }
  }, [job?.status]);

  useEffect(() => {
    if (!activeJobId) return;
    const handler = (...args: unknown[]) => {
      const updated = args[0] as Job;
      if (updated.id === activeJobId) updateJob(updated);
    };
    window.aidclaude.on("job:update", handler);
    return () => window.aidclaude.off("job:update", handler);
  }, [activeJobId, updateJob]);

  async function handleTableClick(tableName: string) {
    if (!activeJobId) return;
    setDbLoading(true);
    setSelectedTable(tableName);
    setDbPreview(null);
    setSavingAs(false);
    setSaveName("");
    setSaveMsg(null);
    try {
      const result = await window.aidclaude.db.previewTable(activeJobId, tableName);
      setDbPreview(result);
    } finally {
      setDbLoading(false);
    }
  }

  async function handleSaveAsSource() {
    if (!activeJobId || !selectedTable || !saveName.trim()) return;
    setSaveBusy(true);
    setSaveMsg(null);
    try {
      const res = await window.aidclaude.db.saveAsSource(activeJobId, selectedTable, saveName.trim());
      if (res.ok && res.source) {
        setSources([...sources, res.source]);
        setSaveMsg(`✓ "${res.source.name}" 소스로 추가됨`);
        setSavingAs(false);
        setSaveName("");
      } else {
        setSaveMsg(`✗ ${res.error ?? "저장 실패"}`);
      }
    } finally {
      setSaveBusy(false);
    }
  }

  const TABS = [
    { id: "db"    as const, label: "DB 테이블" },
    { id: "table" as const, label: "표" },
    { id: "chart" as const, label: "차트" },
  ];

  return (
    <div className="rp-panel">
      {/* ── Tabs ── */}
      <div className="rp-tabs">
        {TABS.map(t => (
          <button
            type="button" key={t.id}
            className={`rp-tab-btn${activeTab === t.id ? " rp-tab-btn-active" : ""}`}
            onClick={() => setActiveTab(t.id)}
          >
            {t.label}
          </button>
        ))}
        {job && <span className="rp-status">{job.status}</span>}
      </div>

      <div className="rp-content">
        {/* ── DB 테이블 탭 ── */}
        {activeTab === "db" && (
          <div>
            {!activeJobId ? (
              <p className="rp-empty">작업을 시작하면 소스 테이블이 표시됩니다.</p>
            ) : (
              <>
                <div className="rp-db-header">
                  <span className="rp-db-title">data.db 테이블 목록</span>
                  <button type="button" className="rp-btn-sm"
                    onClick={() => loadDbTables(activeJobId)}>새로고침</button>
                </div>

                {dbTables.length === 0
                  ? <p className="rp-empty">테이블이 없습니다.</p>
                  : (
                    <div className="rp-db-tables">
                      {dbTables.map(t => (
                        <button
                          type="button" key={t}
                          className={`rp-db-table-btn${selectedTable === t ? " rp-db-table-btn-active" : ""}`}
                          onClick={() => handleTableClick(t)}
                        >
                          {t}
                        </button>
                      ))}
                    </div>
                  )
                }

                {dbLoading && <p className="rp-db-loading rp-empty">로딩 중…</p>}

                {dbPreview && !dbLoading && (
                  <div className="rp-db-preview">
                    {/* 행 1: 제목 + 표/차트 버튼 */}
                    <div className="rp-db-preview-header">
                      <span className="rp-source-title">
                        {dbPreview.title} — {dbPreview.rows.length.toLocaleString()}행
                      </span>
                      <div className="rp-db-preview-actions">
                        <button type="button" className="rp-btn-sm"
                          onClick={() => setActiveTab("table")}>표로 보기</button>
                        <button type="button" className="rp-btn-sm"
                          onClick={() => setActiveTab("chart")}>차트 생성</button>
                      </div>
                    </div>

                    {/* 행 2: 소스로 저장 영역 */}
                    <div className="rp-save-source-row">
                      {!savingAs ? (
                        <button
                          type="button"
                          className="rp-btn-save-source"
                          onClick={() => { setSavingAs(true); setSaveName(selectedTable ?? ""); setSaveMsg(null); }}
                        >
                          + 소스로 저장
                        </button>
                      ) : (
                        <div className="rp-save-source-form">
                          <input
                            type="text"
                            className="rp-save-source-input"
                            value={saveName}
                            onChange={e => setSaveName(e.target.value)}
                            onKeyDown={e => {
                              if (e.key === "Enter") handleSaveAsSource();
                              if (e.key === "Escape") setSavingAs(false);
                            }}
                            placeholder="새 데이터 소스 이름"
                            autoFocus
                          />
                          <button type="button" className="rp-btn-sm"
                            disabled={!saveName.trim() || saveBusy}
                            onClick={handleSaveAsSource}>
                            {saveBusy ? "저장 중…" : "저장"}
                          </button>
                          <button type="button" className="rp-btn-sm rp-btn-cancel"
                            onClick={() => { setSavingAs(false); setSaveName(""); setSaveMsg(null); }}>
                            취소
                          </button>
                        </div>
                      )}
                      {saveMsg && (
                        <span className={`rp-save-source-msg${saveMsg.startsWith("✓") ? " rp-save-source-msg-ok" : " rp-save-source-msg-err"}`}>
                          {saveMsg}
                        </span>
                      )}
                    </div>

                    <TableView headers={dbPreview.headers} rows={dbPreview.rows} truncateAt={50} />
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* ── 표 탭 ── */}
        {activeTab === "table" && (
          dbPreview ? (
            <div>
              <div className="rp-export-bar">
                <span className="rp-export-bar-title">
                  {dbPreview.title} — {dbPreview.rows.length.toLocaleString()}행
                </span>
                <div className="rp-export-bar-btns">
                  <button type="button" className="cp-export-btn"
                    onClick={() => exportCSV(dbPreview.title, dbPreview.headers, dbPreview.rows)}>
                    ↓ CSV
                  </button>
                  <button type="button" className="cp-export-btn"
                    onClick={() => exportJSON(dbPreview.title, dbPreview.headers, dbPreview.rows)}>
                    ↓ JSON
                  </button>
                </div>
              </div>
              <TableView headers={dbPreview.headers} rows={dbPreview.rows} />
            </div>
          ) : (
            <p className="rp-empty">DB 테이블 탭에서 테이블을 선택하세요.</p>
          )
        )}

        {/* ── 차트 탭 ── */}
        {activeTab === "chart" && (
          dbPreview && dbPreview.headers.length >= 2
            ? <TableChart
                headers={dbPreview.headers}
                rows={dbPreview.rows}
                tableName={dbPreview.title}
              />
            : <p className="rp-empty">DB 테이블 탭에서 컬럼이 2개 이상인 테이블을 선택하세요.</p>
        )}
      </div>
    </div>
  );
}
