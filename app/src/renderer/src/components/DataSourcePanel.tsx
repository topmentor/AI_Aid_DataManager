import { useState } from "react";
import { useAppStore } from "../store/appStore";
import type { DataSource, DataSourceType, MariaDbConfig, CsvConfig, JsonConfig, DataSourceSchema } from "../../../shared/types";

type FormConfig = Partial<MariaDbConfig & CsvConfig & JsonConfig>;

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

export function DataSourcePanel() {
  const { sources, schemas, setSources, setSchema } = useAppStore();
  const [adding, setAdding] = useState(false);
  const [form, setForm] = useState<AddForm>(DEFAULT_FORM);
  const [testResults, setTestResults] = useState<Record<string, { ok: boolean; error?: string }>>({});
  const [schemaView, setSchemaView] = useState<string | null>(null); // sourceId being viewed

  function updateFormConfig(patch: Partial<FormConfig>) {
    setForm((f) => ({ ...f, config: { ...f.config, ...patch } }));
  }

  function handleTypeChange(type: DataSourceType) {
    const config: FormConfig =
      type === "mariadb"
        ? { host: "localhost", port: 3306, database: "", user: "", password: "" }
        : type === "csv"
        ? { filePath: "" }
        : { filePath: "" };
    setForm({ ...DEFAULT_FORM, type, config });
  }

  async function handleAdd() {
    if (!form.name.trim()) return;
    const ds = await window.aidclaude.catalog.add({
      name: form.name.trim(),
      type: form.type,
      config: form.config as MariaDbConfig | CsvConfig | JsonConfig,
    });
    setSources([...sources, ds]);
    setForm(DEFAULT_FORM);
    setAdding(false);
  }

  async function handleRemove(id: string) {
    await window.aidclaude.catalog.remove(id);
    setSources(sources.filter((s) => s.id !== id));
  }

  async function handleTest(id: string) {
    setTestResults((r) => ({ ...r, [id]: { ok: false, error: "테스트 중…" } }));
    const result = await window.aidclaude.catalog.testConnection(id);
    setTestResults((r) => ({ ...r, [id]: result }));
  }

  async function handleSchema(id: string) {
    if (schemaView === id) {
      setSchemaView(null);
      return;
    }
    if (!schemas.has(id)) {
      try {
        const schema = await window.aidclaude.catalog.getSchema(id);
        setSchema(id, schema);
      } catch (e) {
        alert(`스키마 조회 실패: ${(e as Error).message}`);
        return;
      }
    }
    setSchemaView(id);
  }

  function renderSchemaView(schema: DataSourceSchema) {
    if (schema.type === "mariadb" && schema.tables) {
      return (
        <div style={{ marginTop: 8, fontSize: 11, color: "#aaa" }}>
          {schema.tables.map((t) => (
            <div key={t.tableName} style={{ marginBottom: 6 }}>
              <strong style={{ color: "#d4d4d4" }}>{t.tableName}</strong>
              {t.columns.map((c) => (
                <div key={c.name} style={{ paddingLeft: 12, color: "#888" }}>
                  {c.name} <span style={{ color: "#666" }}>({c.type})</span>
                </div>
              ))}
            </div>
          ))}
        </div>
      );
    }
    if ((schema.type === "csv" || schema.type === "json") && schema.columns) {
      return (
        <div style={{ marginTop: 8, fontSize: 11 }}>
          {schema.columns.map((c) => (
            <div key={c.name} style={{ color: "#888", paddingLeft: 4 }}>
              {c.name}
              {c.sample && <span style={{ color: "#666" }}> — {c.sample}</span>}
            </div>
          ))}
        </div>
      );
    }
    if (schema.structure) {
      return (
        <pre style={{ marginTop: 8, fontSize: 10, color: "#888", overflow: "auto", maxHeight: 150 }}>
          {schema.structure}
        </pre>
      );
    }
    return <div style={{ color: "#666", fontSize: 11 }}>스키마 없음</div>;
  }

  return (
    <div style={{ padding: 10, overflowY: "auto", height: "100%", fontSize: 13 }}>
      {/* Header */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
        <strong>데이터 소스</strong>
        <button
          style={{ fontSize: 11, padding: "2px 8px" }}
          onClick={() => { setAdding((v) => !v); setForm(DEFAULT_FORM); }}
        >
          {adding ? "취소" : "+ 추가"}
        </button>
      </div>

      {/* Add Form */}
      {adding && (
        <div style={{ background: "#2d2d2d", padding: 10, borderRadius: 6, marginBottom: 10 }}>
          {/* Name */}
          <div style={{ marginBottom: 6 }}>
            <label style={{ display: "block", color: "#888", marginBottom: 2, fontSize: 11 }}>이름</label>
            <input
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              placeholder="소스 이름"
              style={{ width: "100%" }}
            />
          </div>
          {/* Type */}
          <div style={{ marginBottom: 6 }}>
            <label style={{ display: "block", color: "#888", marginBottom: 2, fontSize: 11 }}>유형</label>
            <select
              value={form.type}
              onChange={(e) => handleTypeChange(e.target.value as DataSourceType)}
              style={{ width: "100%" }}
            >
              <option value="mariadb">MariaDB</option>
              <option value="csv">CSV 파일</option>
              <option value="json">JSON 파일</option>
            </select>
          </div>

          {/* MariaDB fields */}
          {form.type === "mariadb" && (
            <>
              {(["host", "port", "database", "user", "password"] as const).map((k) => (
                <div key={k} style={{ marginBottom: 5 }}>
                  <label style={{ display: "block", color: "#888", marginBottom: 2, fontSize: 11 }}>{k}</label>
                  <input
                    type={k === "password" ? "password" : "text"}
                    value={String((form.config as MariaDbConfig)[k] ?? "")}
                    onChange={(e) =>
                      updateFormConfig({
                        [k]: k === "port" ? Number(e.target.value) : e.target.value,
                      })
                    }
                    style={{ width: "100%" }}
                  />
                </div>
              ))}
            </>
          )}

          {/* CSV/JSON fields */}
          {(form.type === "csv" || form.type === "json") && (
            <>
              <div style={{ marginBottom: 5 }}>
                <label style={{ display: "block", color: "#888", marginBottom: 2, fontSize: 11 }}>파일 경로</label>
                <input
                  value={(form.config as CsvConfig).filePath ?? ""}
                  onChange={(e) => updateFormConfig({ filePath: e.target.value })}
                  placeholder="C:\data\file.csv"
                  style={{ width: "100%" }}
                />
              </div>
              {form.type === "json" && (
                <div style={{ marginBottom: 5 }}>
                  <label style={{ display: "block", color: "#888", marginBottom: 2, fontSize: 11 }}>루트 경로 (선택)</label>
                  <input
                    value={(form.config as JsonConfig).rootPath ?? ""}
                    onChange={(e) => updateFormConfig({ rootPath: e.target.value || undefined })}
                    placeholder="data.items"
                    style={{ width: "100%" }}
                  />
                </div>
              )}
              {form.type === "csv" && (
                <div style={{ marginBottom: 5 }}>
                  <label style={{ display: "block", color: "#888", marginBottom: 2, fontSize: 11 }}>구분자 (선택)</label>
                  <input
                    value={(form.config as CsvConfig).delimiter ?? ""}
                    onChange={(e) => updateFormConfig({ delimiter: e.target.value || undefined })}
                    placeholder="자동 감지"
                    style={{ width: "100%" }}
                  />
                </div>
              )}
            </>
          )}

          <button onClick={handleAdd} style={{ marginTop: 6, width: "100%" }}>저장</button>
        </div>
      )}

      {/* Source list */}
      {sources.length === 0 && !adding && (
        <p style={{ color: "#555", fontSize: 12, padding: "8px 0" }}>데이터 소스가 없습니다.</p>
      )}

      {sources.map((ds) => {
        const testResult = testResults[ds.id];
        const schema = schemas.get(ds.id);
        const isShowingSchema = schemaView === ds.id;

        return (
          <div
            key={ds.id}
            style={{ background: "#2d2d2d", padding: 10, borderRadius: 6, marginBottom: 8 }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div>
                <strong style={{ fontSize: 13 }}>{ds.name}</strong>
                <span style={{ color: "#666", marginLeft: 6, fontSize: 11 }}>{ds.type}</span>
              </div>
              <div style={{ display: "flex", gap: 4 }}>
                <button
                  style={{ fontSize: 11, padding: "2px 6px", background: "#444" }}
                  onClick={() => handleSchema(ds.id)}
                >
                  {isShowingSchema ? "닫기" : "스키마"}
                </button>
                <button
                  style={{ fontSize: 11, padding: "2px 6px", background: "#444" }}
                  onClick={() => handleTest(ds.id)}
                >
                  테스트
                </button>
                <button
                  style={{ fontSize: 11, padding: "2px 6px", background: "#6b2b2b" }}
                  onClick={() => handleRemove(ds.id)}
                >
                  삭제
                </button>
              </div>
            </div>

            {testResult && (
              <div
                style={{
                  marginTop: 6,
                  fontSize: 11,
                  color: testResult.ok ? "#6dbc6d" : "#e07070",
                }}
              >
                {testResult.ok ? "✓ 연결 성공" : `✗ ${testResult.error}`}
              </div>
            )}

            {isShowingSchema && schema && renderSchemaView(schema)}
          </div>
        );
      })}
    </div>
  );
}
