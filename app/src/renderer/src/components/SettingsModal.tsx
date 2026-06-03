import { useEffect, useState } from "react";
import { useAppStore } from "../store/appStore";

// 터미널용 모노스페이스 폰트 후보
const FONT_OPTIONS = [
  "Consolas, 'D2Coding', monospace",
  "'D2Coding', monospace",
  "'Cascadia Code', monospace",
  "'Cascadia Mono', monospace",
  "Consolas, monospace",
  "'Courier New', monospace",
  "'Nanum Gothic Coding', monospace",
  "monospace",
];

/** 설정 모달 — 애플리케이션 기본 경로(작업 공간) + AI 터미널 폰트. */
export function SettingsModal() {
  const setSettingsOpen = useAppStore((s) => s.setSettingsOpen);
  const setSettings = useAppStore((s) => s.setSettings);

  const [workspaceRoot, setWorkspaceRoot] = useState("");
  const [fontFamily, setFontFamily] = useState("");
  const [fontSize, setFontSize] = useState("13");
  const [systemFonts, setSystemFonts] = useState<string[]>([]);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    window.aida.settings
      .get()
      .then((s) => {
        setWorkspaceRoot(s.workspaceRoot ?? "");
        setFontFamily(s.termFontFamily ?? "");
        setFontSize(String(s.termFontSize ?? "13"));
        setLoading(false);
      })
      .catch(() => setLoading(false));
    window.aida.fonts.list().then(setSystemFonts).catch(() => setSystemFonts([]));
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") setSettingsOpen(false); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [setSettingsOpen]);

  const browse = async () => {
    const dir = await window.aida.dialog.openDirectory();
    if (dir) setWorkspaceRoot(dir);
  };

  const save = async () => {
    setStatus("저장 중...");
    try {
      const merged = await window.aida.settings.set({
        workspaceRoot,
        termFontFamily: fontFamily,
        termFontSize: fontSize,
      });
      setSettings(merged);
      setStatus("저장되었습니다");
      setTimeout(() => setSettingsOpen(false), 500);
    } catch (e) {
      setStatus("저장 실패: " + (e as Error).message);
    }
  };

  return (
    <div className="sm-overlay" onClick={() => setSettingsOpen(false)}>
      <div className="sm-modal" onClick={(e) => e.stopPropagation()}>
        <div className="sm-header">
          <strong>설정</strong>
          <button className="sm-x" onClick={() => setSettingsOpen(false)} aria-label="닫기">×</button>
        </div>

        {loading ? (
          <div className="sm-body">불러오는 중...</div>
        ) : (
          <div className="sm-body">
            <label className="sm-field">
              <span>애플리케이션 기본 경로</span>
              <small className="sm-help">data.db · 프롬프트(CLAUDE.md) 등 작업 데이터를 저장할 폴더</small>
              <div className="sm-row">
                <input value={workspaceRoot} onChange={(e) => setWorkspaceRoot(e.target.value)}
                  placeholder="예: C:\Users\me\.aida" />
                <button onClick={browse}>찾아보기</button>
              </div>
            </label>

            <div className="sm-field">
              <span>AI 터미널 폰트</span>
              <div className="sm-row sm-font-row">
                <select className="sm-font-select" title="AI 터미널 폰트" value={fontFamily} onChange={(e) => setFontFamily(e.target.value)}>
                  {fontFamily && !FONT_OPTIONS.includes(fontFamily) && !systemFonts.includes(fontFamily) && (
                    <option value={fontFamily}>{fontFamily} (현재)</option>
                  )}
                  <optgroup label="추천 (고정폭)">
                    {FONT_OPTIONS.map((f) => (
                      <option key={f} value={f}>{f}</option>
                    ))}
                  </optgroup>
                  {systemFonts.length > 0 && (
                    <optgroup label="시스템 폰트">
                      {systemFonts.map((f) => (
                        <option key={f} value={f}>{f}</option>
                      ))}
                    </optgroup>
                  )}
                </select>
                <input className="sm-font-size" type="number" min={8} max={40} value={fontSize}
                  onChange={(e) => setFontSize(e.target.value)} title="폰트 크기 (px)" />
                <span className="sm-unit">px</span>
              </div>
            </div>

            <div className="sm-note">경로 변경은 이후 새로 시작하는 작업부터 적용됩니다.</div>
          </div>
        )}

        <div className="sm-footer">
          <span className="sm-status">{status}</span>
          <button onClick={() => setSettingsOpen(false)}>취소</button>
          <button className="sm-save" onClick={save}>저장</button>
        </div>
      </div>
    </div>
  );
}
