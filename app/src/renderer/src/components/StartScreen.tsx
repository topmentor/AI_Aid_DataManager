import { useEffect } from "react";
import { useAppStore } from "../store/appStore";

export function StartScreen() {
  const { probe, setProbe, setView, setSources } = useAppStore();

  useEffect(() => {
    window.aidclaude.claude.probe().then(setProbe);
    window.aidclaude.catalog.list().then(setSources);
  }, []);

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        height: "100vh",
        gap: 20,
        background: "#1e1e1e",
        color: "#d4d4d4",
      }}
    >
      <h1 style={{ fontSize: 32, fontWeight: 700, letterSpacing: "-0.5px" }}>
        AidClaude
      </h1>
      <p style={{ color: "#888", fontSize: 14 }}>
        Claude Code 기반 데이터 분석·시각화 도구
      </p>

      {probe === null && (
        <p style={{ color: "#666", fontSize: 13 }}>Claude 연결 확인 중…</p>
      )}

      {probe !== null && (
        <div
          style={{
            padding: "10px 18px",
            background: probe.authenticated ? "#1a3a1a" : "#3a1a1a",
            border: `1px solid ${probe.authenticated ? "#2d5a2d" : "#5a2d2d"}`,
            borderRadius: 6,
            fontSize: 13,
            maxWidth: 400,
            textAlign: "center",
          }}
        >
          {probe.authenticated ? (
            <span style={{ color: "#6dbc6d" }}>
              ✓ Claude {probe.version} 연결됨{" "}
              {probe.roundTripMs !== null && `(${probe.roundTripMs}ms)`}
            </span>
          ) : (
            <span style={{ color: "#e07070" }}>✗ {probe.error}</span>
          )}
        </div>
      )}

      <button
        disabled={!probe?.authenticated}
        onClick={() => setView("main")}
        style={{
          padding: "10px 36px",
          fontSize: 15,
          background: probe?.authenticated ? "#0e639c" : "#444",
          color: "#fff",
          border: "none",
          borderRadius: 5,
          cursor: probe?.authenticated ? "pointer" : "not-allowed",
          marginTop: 8,
        }}
      >
        시작
      </button>

      {!probe?.authenticated && probe !== null && (
        <p style={{ color: "#666", fontSize: 12 }}>
          Claude Code CLI를 설치하고 <code>claude login</code>을 실행해주세요.
        </p>
      )}
    </div>
  );
}
