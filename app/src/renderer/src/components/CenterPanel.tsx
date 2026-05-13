import { useEffect, useRef, useState } from "react";
import {
  Chart as ChartJS,
  CategoryScale, LinearScale,
  BarElement, LineElement, PointElement, ArcElement,
  Title, Tooltip, Legend,
} from "chart.js";
import { Bar, Line, Scatter, Pie } from "react-chartjs-2";
import { useAppStore } from "../store/appStore";
import { exportCSV, exportJSON, exportChartPNG } from "../utils/exportUtils";
import { MapView } from "./MapView";

ChartJS.register(
  CategoryScale, LinearScale,
  BarElement, LineElement, PointElement, ArcElement,
  Title, Tooltip, Legend
);

// ── TableView ────────────────────────────────────────────────────────────────

function TableView({
  headers, rows, hasMore, loadingMore, onLoadMore,
}: {
  headers: string[];
  rows: string[][];
  hasMore?: boolean;
  loadingMore?: boolean;
  onLoadMore?: () => void;
}) {
  return (
    <div className="tv-root">
      <div className="tv-scroll">
        <table className="tv-table">
          <thead>
            <tr>
              <th className="tv-th-rownum">#</th>
              {headers.map((h, i) => <th key={i} className="tv-th">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) => (
              <tr key={i} className={i % 2 === 0 ? "tv-row-even" : "tv-row-odd"}>
                <td className="tv-td-rownum">{i + 1}</td>
                {row.map((cell, j) => <td key={j} className="tv-td">{cell}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="tv-footer">
        <span className="tv-row-count">{rows.length.toLocaleString()}행</span>
        {hasMore && (
          <button
            type="button"
            className="tv-load-more-btn"
            onClick={onLoadMore}
            disabled={loadingMore}
          >
            {loadingMore ? "로딩 중…" : "+ 500행 더 불러오기"}
          </button>
        )}
        {!hasMore && rows.length > 0 && onLoadMore !== undefined && (
          <span className="tv-all-loaded">전체 로드 완료</span>
        )}
      </div>
    </div>
  );
}

// ── TableChart ────────────────────────────────────────────────────────────────

const CHART_COLORS = [
  "rgba(14,99,156,0.75)", "rgba(91,164,207,0.75)", "rgba(77,188,137,0.75)",
  "rgba(217,119,87,0.75)", "rgba(168,100,200,0.75)", "rgba(229,192,75,0.75)",
];

type ChartType = "bar" | "line" | "scatter" | "pie";

const PIE_COLORS = [
  "rgba(14,99,156,0.8)", "rgba(91,164,207,0.8)", "rgba(77,188,137,0.8)",
  "rgba(217,119,87,0.8)", "rgba(168,100,200,0.8)", "rgba(229,192,75,0.8)",
  "rgba(232,100,100,0.8)", "rgba(80,200,180,0.8)", "rgba(150,180,60,0.8)",
  "rgba(220,120,200,0.8)",
];

function TableChart({ headers, rows, tableName }: {
  headers: string[];
  rows: string[][];
  tableName: string;
}) {
  const [xCol, setXCol]       = useState(headers[0] ?? "");
  const [yCols, setYCols]     = useState<string[]>(
    headers.slice(1).filter((_, i) => i < 3)
  );
  const [pieLabel, setPieLabel] = useState(headers[0] ?? "");
  const [pieValue, setPieValue] = useState(headers.length > 1 ? headers[1] : headers[0] ?? "");
  const [chartType, setChartType] = useState<ChartType>("bar");
  const [maxRows, setMaxRows] = useState(100);
  const canvasWrapRef = useRef<HTMLDivElement>(null);

  const sample = rows.slice(0, maxRows);
  const xIdx   = headers.indexOf(xCol);

  const cartesianOptions = {
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

  const pieOptions = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: {
      legend: {
        position: "right" as const,
        labels: { color: "#c8c8c8", boxWidth: 12, font: { size: 11 } },
      },
      title: { display: true, text: tableName, color: "#aaa", font: { size: 12 } },
      tooltip: {
        callbacks: {
          label: (ctx: { label?: string; formattedValue: string; parsed: number; dataset: { data: number[] } }) => {
            const total = (ctx.dataset.data as number[]).reduce((a, b) => a + b, 0);
            const pct = total > 0 ? ((ctx.parsed / total) * 100).toFixed(1) : "0.0";
            return ` ${ctx.label}: ${ctx.formattedValue} (${pct}%)`;
          },
        },
      },
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

  const pieLabelIdx = headers.indexOf(pieLabel);
  const pieValueIdx = headers.indexOf(pieValue);
  const pieAggMap = new Map<string, number>();
  for (const row of sample) {
    const key = row[pieLabelIdx] ?? "";
    const val = parseFloat(row[pieValueIdx]) || 0;
    pieAggMap.set(key, (pieAggMap.get(key) ?? 0) + val);
  }
  const pieLabels = [...pieAggMap.keys()];
  const pieData = [...pieAggMap.values()];
  const pieDataset = {
    data: pieData,
    backgroundColor: pieLabels.map((_, i) => PIE_COLORS[i % PIE_COLORS.length]),
    borderColor: "#1e1e1e",
    borderWidth: 2,
  };

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
              <option value="pie">원그래프</option>
            </select>
          </label>

          {chartType === "pie" ? (
            <>
              <label className="rp-chart-cfg-label">레이블
                <select className="rp-chart-cfg-sel" value={pieLabel}
                  onChange={e => setPieLabel(e.target.value)}>
                  {headers.map(h => <option key={h} value={h}>{h}</option>)}
                </select>
              </label>
              <label className="rp-chart-cfg-label">값
                <select className="rp-chart-cfg-sel" value={pieValue}
                  onChange={e => setPieValue(e.target.value)}>
                  {headers.map(h => <option key={h} value={h}>{h}</option>)}
                </select>
              </label>
            </>
          ) : (
            <label className="rp-chart-cfg-label">X축
              <select className="rp-chart-cfg-sel" value={xCol}
                onChange={e => setXCol(e.target.value)}>
                {headers.map(h => <option key={h} value={h}>{h}</option>)}
              </select>
            </label>
          )}

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

        {chartType !== "pie" && (
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
        )}
      </div>

      <div className="rp-chart-canvas" ref={canvasWrapRef}>
        {chartType === "pie" ? (
          pieLabels.length === 0 ? (
            <p className="rp-empty">데이터가 없습니다.</p>
          ) : (
            <div className="rp-pie-wrap">
              <Pie data={{ labels: pieLabels, datasets: [pieDataset] }} options={pieOptions} />
            </div>
          )
        ) : yCols.length === 0 ? (
          <p className="rp-empty">Y축 컬럼을 하나 이상 선택하세요.</p>
        ) : chartType === "scatter" ? (
          <Scatter data={{ datasets }} options={cartesianOptions} />
        ) : chartType === "bar" ? (
          <Bar data={{ labels, datasets }} options={cartesianOptions} />
        ) : (
          <Line data={{ labels, datasets }} options={cartesianOptions} />
        )}
      </div>
    </div>
  );
}

// ── CenterPanel ───────────────────────────────────────────────────────────────

export function CenterPanel() {
  const {
    centerTabs, activeCenterTabId,
    openCenterTab, closeCenterTab, setActiveCenterTab, setCenterTabView,
    activeJobId, jobs, sources, setSources,
  } = useAppStore();

  const activeTab = centerTabs.find(t => t.id === activeCenterTabId) ?? null;
  const prevStatusRef = useRef<Record<string, string>>({});
  const [loadingMore, setLoadingMore] = useState(false);
  const [exportingFmt, setExportingFmt] = useState<"csv" | "json" | null>(null);

  // 소스로 저장 모달 상태
  const [modalOpen, setModalOpen] = useState(false);
  const [saveName,  setSaveName]  = useState("");
  const [saveBusy,  setSaveBusy]  = useState(false);
  const [saveMsg,   setSaveMsg]   = useState<string | null>(null);

  // 탭 변경 시 모달 닫기
  useEffect(() => {
    setModalOpen(false);
    setSaveMsg(null);
  }, [activeCenterTabId]);

  // Job 완료 시 result 테이블 자동 로드
  useEffect(() => {
    if (!activeJobId) return;
    const job = jobs.find(j => j.id === activeJobId);
    const status = job?.status;
    if (status === "done" && prevStatusRef.current[activeJobId] !== "done") {
      loadJobTables(activeJobId);
    }
    if (status) prevStatusRef.current[activeJobId] = status;
  }, [jobs, activeJobId]);

  async function loadJobTables(jobId: string) {
    try {
      const tables = await window.aidclaude.db.listTables(jobId);
      const limit = 500;
      for (const tableName of tables) {
        const tabId = `db:${jobId}:${tableName}`;
        const result = await window.aidclaude.db.previewTable(jobId, tableName, limit);
        useAppStore.getState().openCenterTab({
          id: tabId,
          title: tableName,
          headers: result.headers,
          rows: result.rows,
          sourceRef: { kind: "db", jobId, tableName },
          fullyLoaded: result.rows.length < limit,
        });
      }
    } catch {/* DB 없으면 무시 */}
  }

  function openSaveModal() {
    setSaveName(activeTab?.title ?? "");
    setSaveMsg(null);
    setModalOpen(true);
  }

  async function handleSaveAsSource() {
    if (!activeTab || !saveName.trim()) return;
    if (sources.some(s => s.name === saveName.trim())) {
      setSaveMsg(`✗ "${saveName.trim()}" 이름이 이미 존재합니다.`);
      return;
    }
    setSaveBusy(true);
    setSaveMsg(null);
    try {
      const ref = activeTab.sourceRef;
      let res: { ok: boolean; source?: import("../../../shared/types").DataSource; error?: string };

      if (ref?.kind === "db") {
        res = await window.aidclaude.db.saveAsSource(ref.jobId, ref.tableName, saveName.trim());
      } else {
        res = await window.aidclaude.data.saveAsSource(saveName.trim(), activeTab.headers, activeTab.rows);
      }

      if (res.ok && res.source) {
        setSources([...sources, res.source]);
        setSaveMsg(`✓ "${res.source.name}" 소스로 추가됨`);
        setTimeout(() => setModalOpen(false), 1200);
      } else {
        setSaveMsg(`✗ ${res.error ?? "저장 실패"}`);
      }
    } finally {
      setSaveBusy(false);
    }
  }

  /** 소스 전체 행을 가져옴 (미리보기 제한 없이) */
  async function fetchAllRows(): Promise<{ headers: string[]; rows: string[][] }> {
    const ref = activeTab?.sourceRef;
    if (!ref) return { headers: activeTab?.headers ?? [], rows: activeTab?.rows ?? [] };
    if (ref.kind === "catalog") {
      return window.aidclaude.catalog.previewData(ref.sourceId, 100_000_000);
    } else {
      return window.aidclaude.db.previewTable(ref.jobId, ref.tableName, 100_000_000);
    }
  }

  async function handleExportCSV() {
    if (!activeTab) return;
    setExportingFmt("csv");
    try {
      const { headers, rows } =
        activeTab.fullyLoaded || !activeTab.sourceRef
          ? { headers: activeTab.headers, rows: activeTab.rows }
          : await fetchAllRows();
      await exportCSV(activeTab.title, headers, rows);
    } catch (e) {
      alert(`CSV 내보내기 실패: ${(e as Error).message}`);
    } finally {
      setExportingFmt(null);
    }
  }

  async function handleExportJSON() {
    if (!activeTab) return;
    setExportingFmt("json");
    try {
      const { headers, rows } =
        activeTab.fullyLoaded || !activeTab.sourceRef
          ? { headers: activeTab.headers, rows: activeTab.rows }
          : await fetchAllRows();
      await exportJSON(activeTab.title, headers, rows);
    } catch (e) {
      alert(`JSON 내보내기 실패: ${(e as Error).message}`);
    } finally {
      setExportingFmt(null);
    }
  }

  async function handleLoadMore() {
    if (!activeTab?.sourceRef || loadingMore || activeTab.fullyLoaded) return;
    setLoadingMore(true);
    try {
      const nextLimit = activeTab.rows.length + 500;
      let result: { title: string; headers: string[]; rows: string[][] };
      if (activeTab.sourceRef.kind === "catalog") {
        result = await window.aidclaude.catalog.previewData(activeTab.sourceRef.sourceId, nextLimit);
      } else {
        result = await window.aidclaude.db.previewTable(
          activeTab.sourceRef.jobId, activeTab.sourceRef.tableName, nextLimit
        );
      }
      openCenterTab({
        id: activeTab.id,
        title: activeTab.title,
        headers: result.headers,
        rows: result.rows,
        sourceRef: activeTab.sourceRef,
        fullyLoaded: result.rows.length < nextLimit,
      });
    } catch (e) {
      alert(`불러오기 실패: ${(e as Error).message}`);
    } finally {
      setLoadingMore(false);
    }
  }

  return (
    <div className="cp-root">
      {/* ── Tab bar ── */}
      <div className="cp-tabbar">
        {centerTabs.length === 0 ? (
          <span className="cp-tabbar-hint">미리보기 또는 분석 결과가 여기에 표시됩니다</span>
        ) : (
          centerTabs.map(tab => (
            <div
              key={tab.id}
              className={`cp-tab${activeCenterTabId === tab.id ? " cp-tab-active" : ""}`}
              onClick={() => setActiveCenterTab(tab.id)}
            >
              <span className="cp-tab-title">{tab.title}</span>
              <button
                type="button"
                className="cp-tab-close"
                onClick={e => { e.stopPropagation(); closeCenterTab(tab.id); }}
              >
                ×
              </button>
            </div>
          ))
        )}
      </div>

      {/* ── Content ── */}
      <div className="cp-body">
        {!activeTab ? (
          <div className="cp-empty-state">
            <p>왼쪽 데이터 소스의 <strong>미리보기</strong> 버튼을 클릭하거나,</p>
            <p>AI에게 분석을 요청하면 결과가 여기에 표시됩니다.</p>
          </div>
        ) : (
          <>
            {/* Toolbar */}
            <div className="cp-toolbar">
              <span className="cp-toolbar-title">
                {activeTab.title} — {activeTab.rows.length.toLocaleString()}행 × {activeTab.headers.length}열
              </span>
              <div className="cp-toolbar-right">
                <div className="cp-view-btns">
                  <button
                    type="button"
                    className={`cp-view-btn${activeTab.view === "table" ? " cp-view-btn-active" : ""}`}
                    onClick={() => setCenterTabView(activeTab.id, "table")}
                  >표</button>
                  <button
                    type="button"
                    className={`cp-view-btn${activeTab.view === "chart" ? " cp-view-btn-active" : ""}`}
                    onClick={() => setCenterTabView(activeTab.id, "chart")}
                  >차트</button>
                  <button
                    type="button"
                    className={`cp-view-btn${activeTab.view === "map" ? " cp-view-btn-active" : ""}`}
                    onClick={() => setCenterTabView(activeTab.id, "map")}
                  >지도</button>
                </div>
                {activeTab.view === "table" && (
                  <div className="cp-export-btns">
                    <button type="button" className="cp-export-btn"
                      disabled={exportingFmt !== null}
                      onClick={handleExportCSV}>
                      {exportingFmt === "csv" ? "불러오는 중…" : "↓ CSV"}
                    </button>
                    <button type="button" className="cp-export-btn"
                      disabled={exportingFmt !== null}
                      onClick={handleExportJSON}>
                      {exportingFmt === "json" ? "불러오는 중…" : "↓ JSON"}
                    </button>
                  </div>
                )}
                {activeTab.sourceRef && (
                  <button
                    type="button"
                    className="cp-btn-save-source"
                    onClick={openSaveModal}
                  >
                    + 소스로 저장
                  </button>
                )}
              </div>
            </div>

            {/* View content */}
            <div className={`cp-content${activeTab.view === "map" ? " cp-content-map" : ""}`}>
              {activeTab.view === "table" ? (
                <TableView
                  headers={activeTab.headers}
                  rows={activeTab.rows}
                  hasMore={!!activeTab.sourceRef && !activeTab.fullyLoaded}
                  loadingMore={loadingMore}
                  onLoadMore={activeTab.sourceRef ? handleLoadMore : undefined}
                />
              ) : activeTab.view === "map" ? (
                activeTab.headers.length >= 2 ? (
                  <MapView headers={activeTab.headers} rows={activeTab.rows} />
                ) : (
                  <p className="rp-empty">지도를 표시하려면 컬럼이 2개 이상 필요합니다.</p>
                )
              ) : activeTab.headers.length >= 2 ? (
                <TableChart
                  headers={activeTab.headers}
                  rows={activeTab.rows}
                  tableName={activeTab.title}
                />
              ) : (
                <p className="rp-empty">차트를 그리려면 컬럼이 2개 이상 필요합니다.</p>
              )}
            </div>
          </>
        )}
      </div>

      {/* ── Export 진행 다이어로그 ── */}
      {exportingFmt !== null && (
        <div className="cp-modal-overlay">
          <div className="cp-modal cp-export-progress">
            <div className="cp-export-progress-spinner" />
            <p className="cp-modal-title">
              {exportingFmt === "csv" ? "CSV" : "JSON"} 내보내기 준비 중
            </p>
            <p className="cp-modal-hint">
              전체 데이터를 불러오고 있습니다. 잠시만 기다려주세요…
            </p>
          </div>
        </div>
      )}

      {/* ── 소스로 저장 모달 ── */}
      {modalOpen && (
        <div className="cp-modal-overlay" onClick={() => !saveBusy && setModalOpen(false)}>
          <div className="cp-modal" onClick={e => e.stopPropagation()}>
            <p className="cp-modal-title">데이터 소스로 저장</p>
            <p className="cp-modal-hint">
              {activeTab?.sourceRef?.kind === "db"
                ? "DB 테이블 전체를 CSV 소스로 저장합니다."
                : `현재 표시된 ${activeTab?.rows.length.toLocaleString()}행을 CSV 소스로 저장합니다.`}
            </p>
            <input
              type="text"
              className={`cp-modal-input${sources.some(s => s.name === saveName.trim()) ? " cp-modal-input-err" : ""}`}
              value={saveName}
              onChange={e => { setSaveName(e.target.value); setSaveMsg(null); }}
              onKeyDown={e => {
                if (e.key === "Enter" && !saveBusy) handleSaveAsSource();
                if (e.key === "Escape" && !saveBusy) setModalOpen(false);
              }}
              placeholder="소스 이름 입력"
              autoFocus
            />
            {sources.some(s => s.name === saveName.trim()) && (
              <p className="cp-save-msg-err">이미 사용 중인 이름입니다.</p>
            )}
            {saveMsg && !sources.some(s => s.name === saveName.trim()) && (
              <p className={saveMsg.startsWith("✓") ? "cp-save-msg-ok" : "cp-save-msg-err"}>
                {saveMsg}
              </p>
            )}
            <div className="cp-modal-btns">
              <button
                type="button"
                className="cp-btn-save-source cp-modal-confirm"
                disabled={!saveName.trim() || saveBusy || sources.some(s => s.name === saveName.trim())}
                onClick={handleSaveAsSource}
              >
                {saveBusy ? "저장 중…" : "저장"}
              </button>
              <button
                type="button"
                className="cp-export-btn cp-btn-cancel"
                disabled={saveBusy}
                onClick={() => setModalOpen(false)}
              >
                취소
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
