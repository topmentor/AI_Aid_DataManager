import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useAppStore } from "../store/appStore";
import type {
  DataSource, DataSourceType, DataSourceSchema,
  MariaDbConfig, CsvConfig, JsonConfig, JsonlConfig,
} from "../../../shared/types";

// ── Delimiter picker dialog (portal, non-modal) ──────────────────────────────

const PRESET_DELIMITERS = [
  { label: ",", value: "," },
  { label: ";", value: ";" },
  { label: "Tab", value: "\t" },
  { label: "|", value: "|" },
];

function DelimiterDialog({
  filePath,
  current,
  anchorRect,
  onClose,
  onConfirm,
}: {
  filePath: string | undefined;
  current: string | undefined;
  anchorRect: DOMRect;
  onClose: () => void;
  onConfirm: (delimiter: string | undefined) => void;
}) {
  const popupRef = useRef<HTMLDivElement>(null);
  const [lines, setLines] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [custom, setCustom] = useState(
    current && !PRESET_DELIMITERS.some((p) => p.value === current) ? current : ""
  );
  const [selected, setSelected] = useState<string | undefined>(current);

  useEffect(() => {
    if (!filePath) { setLoading(false); return; }
    window.aidclaude.files.readLines(filePath, 3).then((l) => {
      setLines(l);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, [filePath]);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (popupRef.current && !popupRef.current.contains(e.target as Node)) onClose();
    };
    const tid = window.setTimeout(() => document.addEventListener("mousedown", handler), 50);
    return () => { clearTimeout(tid); document.removeEventListener("mousedown", handler); };
  }, [onClose]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [onClose]);

  useLayoutEffect(() => {
    if (!popupRef.current) return;
    const left = anchorRect.right + 8;
    const top = Math.min(anchorRect.top, window.innerHeight - 320);
    popupRef.current.style.left = `${left}px`;
    popupRef.current.style.top = `${top}px`;
  }, [anchorRect]);

  function handlePreset(val: string) {
    setSelected(val);
    setCustom("");
  }

  function handleCustomChange(v: string) {
    setCustom(v);
    setSelected(v || undefined);
  }

  function handleAutoDetect() {
    onConfirm(undefined);
    onClose();
  }

  function handleConfirm() {
    onConfirm(selected || undefined);
    onClose();
  }

  const displayDelimiter = (v: string) => (v === "\t" ? "Tab" : v);

  return createPortal(
    <div ref={popupRef} className="delim-dialog">
      <div className="delim-dialog-header">
        <span className="delim-dialog-title">구분자 선택</span>
        <button type="button" className="schema-popup-close" onClick={onClose}>✕</button>
      </div>
      <div className="delim-dialog-body">
        {!filePath && (
          <p className="delim-dialog-hint">파일을 먼저 선택하세요.</p>
        )}
        {filePath && (
          <>
            <div className="delim-dialog-preview-label">파일 미리보기 (첫 3줄)</div>
            <div className="delim-dialog-preview">
              {loading ? "로딩 중…" : lines.length > 0 ? lines.join("\n") : "(내용 없음)"}
            </div>
          </>
        )}
        <div className="delim-dialog-presets-label">구분자</div>
        <div className="delim-dialog-presets">
          {PRESET_DELIMITERS.map((p) => (
            <button
              key={p.value}
              type="button"
              className={`delim-preset-btn${selected === p.value ? " delim-preset-btn-active" : ""}`}
              onClick={() => handlePreset(p.value)}
            >
              {p.label}
            </button>
          ))}
        </div>
        <div className="delim-dialog-custom-row">
          <span className="delim-dialog-presets-label delim-dialog-no-mb">직접 입력</span>
          <input
            className="dsp-input delim-custom-input"
            value={custom}
            maxLength={4}
            placeholder="기타"
            onChange={(e) => handleCustomChange(e.target.value)}
          />
        </div>
        {selected && (
          <div className="delim-dialog-current">
            선택됨: <code>{displayDelimiter(selected)}</code>
          </div>
        )}
        <div className="delim-dialog-actions">
          <button type="button" className="delim-auto-btn" onClick={handleAutoDetect}>자동 감지</button>
          <button type="button" className="delim-confirm-btn" onClick={handleConfirm}>확인</button>
        </div>
      </div>
    </div>,
    document.body
  );
}

// ── Type badge ───────────────────────────────────────────────────────────────

const TYPE_BADGE: Record<DataSourceType, { label: string; cls: string }> = {
  mariadb: { label: "DB",   cls: "ds-badge-db" },
  csv:     { label: "CSV",  cls: "ds-badge-csv" },
  json:    { label: "{ }",  cls: "ds-badge-json" },
  jsonl:   { label: "≡",    cls: "ds-badge-jsonl" },
};

// ── Schema popup (portal, non-modal) ─────────────────────────────────────────

function SchemaContent({ schema }: { schema: DataSourceSchema }) {
  if (schema.type === "mariadb" && schema.tables) {
    return (
      <>
        {schema.tables.map((t) => (
          <div key={t.tableName} className="sp-table">
            <div className="sp-table-name">{t.tableName}</div>
            {t.columns.map((c) => (
              <div key={c.name} className="sp-col">
                <span className="sp-col-name">{c.name}</span>
                <span className="sp-col-type">{c.type}</span>
              </div>
            ))}
          </div>
        ))}
      </>
    );
  }
  if (schema.columns) {
    return (
      <>
        {schema.columns.map((c) => (
          <div key={c.name} className="sp-col">
            <span className="sp-col-name">{c.name}</span>
            {c.sample != null && c.sample !== "" && (
              <span className="sp-col-sample">{String(c.sample).slice(0, 40)}</span>
            )}
          </div>
        ))}
      </>
    );
  }
  if (schema.structure) {
    return <pre className="sp-structure">{schema.structure}</pre>;
  }
  return <div className="sp-empty">스키마 없음</div>;
}

function SchemaPopup({
  schema, title, anchorRect, onClose,
}: {
  schema: DataSourceSchema;
  title: string;
  anchorRect: DOMRect;
  onClose: () => void;
}) {
  const popupRef = useRef<HTMLDivElement>(null);

  // Click-outside to close
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (popupRef.current && !popupRef.current.contains(e.target as Node)) {
        onClose();
      }
    };
    const tid = window.setTimeout(() => document.addEventListener("mousedown", handler), 50);
    return () => {
      clearTimeout(tid);
      document.removeEventListener("mousedown", handler);
    };
  }, [onClose]);

  // Escape to close
  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [onClose]);

  // Position via DOM ref — avoids inline style lint warning
  useLayoutEffect(() => {
    if (!popupRef.current) return;
    const left = anchorRect.right + 8;
    const top = Math.min(anchorRect.top, window.innerHeight - 420);
    popupRef.current.style.left = `${left}px`;
    popupRef.current.style.top = `${top}px`;
  }, [anchorRect]);

  return createPortal(
    <div
      ref={popupRef}
      className="schema-popup"
    >
      <div className="schema-popup-header">
        <span className="schema-popup-title">{title} 스키마</span>
        <button type="button" className="schema-popup-close" onClick={onClose}>✕</button>
      </div>
      <div className="schema-popup-body">
        <SchemaContent schema={schema} />
      </div>
    </div>,
    document.body
  );
}

// ── Form helpers ─────────────────────────────────────────────────────────────

type FormConfig = Partial<MariaDbConfig & CsvConfig & JsonConfig & JsonlConfig>;

interface AddForm {
  name: string;
  type: DataSourceType;
  config: FormConfig;
}

const DEFAULT_FORM: AddForm = {
  name: "",
  type: "mariadb",
  config: { host: "localhost", port: 3306, database: "", user: "", password: "" },
};

// ── DataSourcePanel ──────────────────────────────────────────────────────────

export function DataSourcePanel() {
  const { sources, schemas, setSources, setSchema, openCenterTab, activeJobId } = useAppStore();
  const [adding, setAdding] = useState(false);
  const [form, setForm] = useState<AddForm>(DEFAULT_FORM);
  const [schemaPopup, setSchemaPopup] = useState<{ sourceId: string; anchorRect: DOMRect } | null>(null);
  const [delimDialog, setDelimDialog] = useState<{ anchorRect: DOMRect } | null>(null);

  function updateFormConfig(patch: Partial<FormConfig>) {
    setForm((f) => ({ ...f, config: { ...f.config, ...patch } }));
  }

  function handleTypeChange(type: DataSourceType) {
    const config: FormConfig =
      type === "mariadb"
        ? { host: "localhost", port: 3306, database: "", user: "", password: "" }
        : { filePath: "" };
    setForm({ ...DEFAULT_FORM, type, config });
  }

  async function handleSelectFile() {
    const filters =
      form.type === "csv"
        ? [{ name: "CSV 파일", extensions: ["csv", "txt"] }]
        : form.type === "jsonl"
        ? [{ name: "JSONL 파일", extensions: ["jsonl", "ndjson"] }]
        : [{ name: "JSON 파일", extensions: ["json"] }];
    const srcPath = await window.aidclaude.dialog.openFile(filters);
    if (!srcPath) return;
    const destPath = await window.aidclaude.files.copyToData(srcPath);
    updateFormConfig({ filePath: destPath });
    // 이름 필드가 비어 있으면 파일명(확장자 제외)으로 자동 채우기
    if (!form.name.trim()) {
      const baseName = srcPath.replace(/\\/g, "/").split("/").pop()?.replace(/\.[^.]+$/, "") ?? "";
      if (baseName) setForm((f) => ({ ...f, name: baseName }));
    }
  }

  async function handleAdd() {
    if (!form.name.trim()) return;
    const ds = await window.aidclaude.catalog.add({
      name: form.name.trim(),
      type: form.type,
      config: form.config as MariaDbConfig | CsvConfig | JsonConfig | JsonlConfig,
    });
    setSources([...sources, ds]);
    setForm(DEFAULT_FORM);
    setAdding(false);
    if (activeJobId) await window.aidclaude.jobs.refreshSources(activeJobId);
  }

  async function handleRemove(id: string) {
    if (schemaPopup?.sourceId === id) setSchemaPopup(null);
    await window.aidclaude.catalog.remove(id);
    setSources(sources.filter((s) => s.id !== id));
    if (activeJobId) await window.aidclaude.jobs.refreshSources(activeJobId);
  }

  async function handlePreview(id: string) {
    try {
      const ds = sources.find((s) => s.id === id);
      const limit = 500;
      const result = await window.aidclaude.catalog.previewData(id, limit);
      openCenterTab({
        id: `src:${id}`,
        title: ds?.name ?? result.title,
        headers: result.headers,
        rows: result.rows,
        sourceRef: { kind: "catalog", sourceId: id },
        fullyLoaded: result.rows.length < limit,
      });
    } catch (e) {
      alert(`미리보기 실패: ${(e as Error).message}`);
    }
  }

  async function handleSchema(id: string, e: React.MouseEvent<HTMLButtonElement>) {
    if (schemaPopup?.sourceId === id) {
      setSchemaPopup(null);
      return;
    }
    const rect = e.currentTarget.getBoundingClientRect();
    if (!schemas.has(id)) {
      try {
        const schema = await window.aidclaude.catalog.getSchema(id);
        setSchema(id, schema);
      } catch (err) {
        alert(`스키마 조회 실패: ${(err as Error).message}`);
        return;
      }
    }
    setSchemaPopup({ sourceId: id, anchorRect: rect });
  }

  const popupSource = schemaPopup ? sources.find((s) => s.id === schemaPopup.sourceId) : null;
  const popupSchema = schemaPopup ? schemas.get(schemaPopup.sourceId) : undefined;

  return (
    <div className="dsp-root">
      {/* Header */}
      <div className="dsp-header">
        <strong className="dsp-title">데이터 소스</strong>
        <button
          type="button"
          className="dsp-add-btn"
          onClick={() => { setAdding((v) => !v); setForm(DEFAULT_FORM); }}
        >
          {adding ? "취소" : "+ 추가"}
        </button>
      </div>

      {/* Add Form */}
      {adding && (
        <div className="dsp-form">
          <div className="dsp-field">
            <label className="dsp-label">이름</label>
            <input
              className="dsp-input"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              placeholder="소스 이름"
            />
          </div>
          <div className="dsp-field">
            <label className="dsp-label">유형</label>
            <select
              className="dsp-input"
              title="데이터 소스 유형"
              value={form.type}
              onChange={(e) => handleTypeChange(e.target.value as DataSourceType)}
            >
              <option value="mariadb">MariaDB</option>
              <option value="csv">CSV 파일</option>
              <option value="json">JSON 파일</option>
              <option value="jsonl">JSONL 파일</option>
            </select>
          </div>

          {form.type === "mariadb" && (
            (["host", "port", "database", "user", "password"] as const).map((k) => (
              <div key={k} className="dsp-field">
                <label className="dsp-label">{k}</label>
                <input
                  className="dsp-input"
                  type={k === "password" ? "password" : "text"}
                  title={k}
                  placeholder={k === "host" ? "localhost" : k === "port" ? "3306" : k}
                  value={String((form.config as MariaDbConfig)[k] ?? "")}
                  onChange={(e) =>
                    updateFormConfig({ [k]: k === "port" ? Number(e.target.value) : e.target.value })
                  }
                />
              </div>
            ))
          )}

          {(form.type === "csv" || form.type === "json" || form.type === "jsonl") && (
            <>
              <div className="dsp-field">
                <label className="dsp-label">파일</label>
                <div className="dsp-file-row">
                  <button type="button" className="dsp-file-btn" onClick={handleSelectFile}>
                    파일 선택
                  </button>
                  <span className="dsp-file-name">
                    {(form.config as CsvConfig).filePath
                      ? (form.config as CsvConfig).filePath!.replace(/\\/g, "/").split("/").pop()
                      : "선택된 파일 없음"}
                  </span>
                </div>
              </div>
              {form.type === "json" && (
                <div className="dsp-field">
                  <label className="dsp-label">루트 경로 (선택)</label>
                  <input
                    className="dsp-input"
                    value={(form.config as JsonConfig).rootPath ?? ""}
                    onChange={(e) => updateFormConfig({ rootPath: e.target.value || undefined })}
                    placeholder="data.items"
                  />
                </div>
              )}
              {form.type === "csv" && (
                <div className="dsp-field">
                  <label className="dsp-label">구분자 (선택)</label>
                  <button
                    type="button"
                    className="dsp-delim-btn"
                    onClick={(e) => setDelimDialog({ anchorRect: e.currentTarget.getBoundingClientRect() })}
                  >
                    {(form.config as CsvConfig).delimiter
                      ? ((form.config as CsvConfig).delimiter === "\t" ? "Tab" : (form.config as CsvConfig).delimiter)
                      : "자동 감지 ▾"}
                  </button>
                </div>
              )}
            </>
          )}

          <button type="button" className="dsp-save-btn" onClick={handleAdd}>저장</button>
        </div>
      )}

      {/* Source list */}
      {sources.length === 0 && !adding && (
        <p className="dsp-empty">데이터 소스가 없습니다.</p>
      )}

      {sources.map((ds) => {
        const badge = TYPE_BADGE[ds.type];
        const isSchemaOpen = schemaPopup?.sourceId === ds.id;
        return (
          <div key={ds.id} className="dsp-item">
            <div className="dsp-item-row">
              <div className="dsp-item-name">
                <span className={`ds-badge ${badge.cls}`}>{badge.label}</span>
                <span className="dsp-name-text" title={ds.name}>{ds.name}</span>
              </div>
              <div className="dsp-item-actions">
                <button
                  type="button"
                  className="dsp-icon-btn"
                  title="미리보기"
                  onClick={() => handlePreview(ds.id)}
                >
                  ▤
                </button>
                <button
                  type="button"
                  className={`dsp-icon-btn${isSchemaOpen ? " dsp-icon-btn-active" : ""}`}
                  title="스키마"
                  onClick={(e) => handleSchema(ds.id, e)}
                >
                  ℹ
                </button>
                <button
                  type="button"
                  className="dsp-icon-btn dsp-icon-btn-danger"
                  title="삭제"
                  onClick={() => handleRemove(ds.id)}
                >
                  ✕
                </button>
              </div>
            </div>
          </div>
        );
      })}

      {/* Schema popup (non-modal, portal) */}
      {schemaPopup && popupSchema && popupSource && (
        <SchemaPopup
          schema={popupSchema}
          title={popupSource.name}
          anchorRect={schemaPopup.anchorRect}
          onClose={() => setSchemaPopup(null)}
        />
      )}

      {/* Delimiter picker dialog (non-modal, portal) */}
      {delimDialog && (
        <DelimiterDialog
          filePath={(form.config as CsvConfig).filePath}
          current={(form.config as CsvConfig).delimiter}
          anchorRect={delimDialog.anchorRect}
          onClose={() => setDelimDialog(null)}
          onConfirm={(delim) => updateFormConfig({ delimiter: delim })}
        />
      )}
    </div>
  );
}
