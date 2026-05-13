import { useEffect } from "react";
import { useAppStore } from "../store/appStore";

export function StartScreen() {
  const { setView, setSources } = useAppStore();

  useEffect(() => {
    window.aidclaude.catalog.list().then(setSources);
  }, []);

  return (
    <div className="ss-root">
      <h1 className="ss-title">AidClaude</h1>
      <p className="ss-sub">Claude Code 기반 데이터 분석·시각화 도구</p>
      <button type="button" className="ss-start-btn" onClick={() => setView("main")}>
        시작
      </button>
    </div>
  );
}
