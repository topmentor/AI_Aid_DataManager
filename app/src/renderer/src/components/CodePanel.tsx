import Editor from "@monaco-editor/react";
import { useAppStore } from "../store/appStore";

export function CodePanel() {
  const activeAnalyzeCode = useAppStore((s) => s.activeAnalyzeCode);

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
      <div
        style={{
          padding: "5px 12px",
          borderBottom: "1px solid #333",
          fontSize: 11,
          color: "#888",
          background: "#252525",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <span>analyze.py</span>
        <span style={{ fontSize: 10, color: "#555" }}>
          {activeAnalyzeCode ? "Claude 생성 코드" : "생성 대기 중"}
        </span>
      </div>
      <div style={{ flex: 1 }}>
        <Editor
          height="100%"
          language="python"
          theme="vs-dark"
          value={activeAnalyzeCode || "# 아직 생성된 코드가 없습니다.\n# 채팅 패널에서 분석 요청을 입력하세요."}
          options={{
            readOnly: true,
            minimap: { enabled: false },
            fontSize: 13,
            lineNumbers: "on",
            scrollBeyondLastLine: false,
            wordWrap: "on",
          }}
        />
      </div>
    </div>
  );
}
