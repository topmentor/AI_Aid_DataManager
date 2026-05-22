import { useState, useEffect, useRef, useMemo } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { useAppStore } from "../store/appStore";

const MARKDOWN_COMPONENTS = {
  a: ({ href, children, ...rest }: React.ComponentPropsWithoutRef<"a">) => (
    <a href={href} target="_blank" rel="noopener noreferrer" {...rest}>
      {children}
    </a>
  ),
};

type MessagePart =
  | { type: "text"; content: string }
  | { type: "options"; items: string[] };

function parseMessageParts(text: string): MessagePart[] {
  const parts: MessagePart[] = [];
  const regex = /<options>([\s\S]*?)<\/options>/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = regex.exec(text)) !== null) {
    const before = text.slice(lastIndex, match.index).trim();
    if (before) parts.push({ type: "text", content: before });
    const items = match[1]
      .trim()
      .split("\n")
      .map((l) => l.replace(/^\d+[.)]\s*/, "").trim())
      .filter((l) => l.length > 0);
    if (items.length > 0) parts.push({ type: "options", items });
    lastIndex = match.index + match[0].length;
  }
  const after = text.slice(lastIndex).trim();
  if (after) parts.push({ type: "text", content: after });
  return parts.length > 0 ? parts : [{ type: "text", content: text }];
}

type ProbeStatus = "idle" | "checking" | "ok" | "error";

function ClaudeConnectBar() {
  const { probe, setProbe } = useAppStore();
  const [status, setStatus] = useState<ProbeStatus>(probe ? (probe.authenticated ? "ok" : "error") : "idle");

  async function handleProbe() {
    setStatus("checking");
    try {
      const result = await window.aidclaude.claude.probe();
      setProbe(result);
      setStatus(result.authenticated ? "ok" : "error");
    } catch {
      setStatus("error");
    }
  }

  useEffect(() => {
    if (status === "idle") handleProbe();
  }, []);  // eslint-disable-line react-hooks/exhaustive-deps

  const label =
    status === "checking" ? "확인 중…" :
    status === "ok"       ? `✓ Claude ${probe?.version ?? ""}` :
    status === "error"    ? `✗ ${probe?.error ?? "연결 실패"}` :
    "Claude 연결 확인";

  const barClass =
    status === "ok"    ? "claude-bar claude-bar-ok" :
    status === "error" ? "claude-bar claude-bar-error" :
    "claude-bar";

  return (
    <div className={barClass}>
      <span className="claude-bar-label">{label}</span>
      <button
        type="button"
        className="claude-bar-btn"
        onClick={handleProbe}
        disabled={status === "checking"}
      >
        {status === "checking" ? "…" : status === "ok" ? "재확인" : "연결 확인"}
      </button>
    </div>
  );
}

export function ChatPanel() {
  const {
    sources,
    jobs,
    activeJobId,
    chatMessages,
    streaming,
    addJob,
    setActiveJob,
    addChatMessage,
    ensureAssistantMessage,
    setStreaming,
    setActiveCode,
  } = useAppStore();

  const [input, setInput] = useState("");
  const endRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // @-mention 팝업 상태
  const [mentionOpen, setMentionOpen] = useState(false);
  const [mentionQuery, setMentionQuery] = useState("");
  const [mentionStart, setMentionStart] = useState(0); // input 내 '@' 위치
  const [mentionHighlight, setMentionHighlight] = useState(0);
  const mentionRef = useRef<HTMLDivElement>(null);

  const messages = activeJobId ? (chatMessages.get(activeJobId) ?? []) : [];

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // 멘션 팝업 필터링
  const mentionFiltered = useMemo(() => {
    if (!mentionOpen) return [];
    const q = mentionQuery.toLowerCase();
    return sources.filter((s) => s.name.toLowerCase().includes(q));
  }, [mentionOpen, mentionQuery, sources]);

  // highlight 범위 보정
  useEffect(() => {
    setMentionHighlight(0);
  }, [mentionFiltered]);

  // 메시지에서 @멘션된 소스 ID 추출 (새 작업에서만 사용)
  function extractMentionedSourceIds(text: string): string[] {
    const matches = [...text.matchAll(/@([^\s@]+)/g)].map((m) => m[1]);
    if (matches.length === 0) return sources.map((s) => s.id);
    const matched = sources.filter((s) => matches.includes(s.name));
    return matched.length > 0 ? matched.map((s) => s.id) : sources.map((s) => s.id);
  }

  async function handleSend() {
    if (!input.trim() || streaming) return;
    if (mentionOpen) return; // 팝업 열려 있으면 Enter는 선택으로 처리
    const text = input.trim();
    setInput("");
    setMentionOpen(false);

    let jobId = activeJobId;
    if (!jobId) {
      const sourceIds = extractMentionedSourceIds(text);
      const job = await window.aidclaude.jobs.create(text, sourceIds);
      addJob(job);
      setActiveJob(job.id);
      jobId = job.id;
    }

    addChatMessage(jobId, {
      role: "user",
      text,
      status: "done",
      toolCalls: [],
      timestamp: new Date().toISOString(),
    });

    const assistantId = crypto.randomUUID();
    ensureAssistantMessage(assistantId);
    setStreaming({ jobId, assistantMessageId: assistantId });

    window.aidclaude.claude.sendMessage(jobId, text).catch(() => {});
  }

  function handleInputChange(e: React.ChangeEvent<HTMLTextAreaElement>) {
    const value = e.target.value;
    setInput(value);

    const cursor = e.target.selectionStart ?? value.length;
    const before = value.slice(0, cursor);
    // '@' 뒤로 공백/엔터 없이 이어지는 텍스트 감지
    const atMatch = before.match(/@([^\s@]*)$/);
    if (atMatch) {
      setMentionQuery(atMatch[1]);
      setMentionStart(cursor - atMatch[0].length);
      setMentionOpen(true);
    } else {
      setMentionOpen(false);
    }
  }

  function selectMention(source: import("../../../shared/types").DataSource) {
    const after = input.slice(mentionStart + 1 + mentionQuery.length);
    const newInput = input.slice(0, mentionStart) + `@${source.name} ` + after;
    setInput(newInput);
    setMentionOpen(false);
    // 커서를 삽입 뒤로 이동
    const pos = mentionStart + source.name.length + 2;
    requestAnimationFrame(() => {
      if (textareaRef.current) {
        textareaRef.current.focus();
        textareaRef.current.setSelectionRange(pos, pos);
      }
    });
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (mentionOpen && mentionFiltered.length > 0) {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setMentionHighlight((h) => Math.min(h + 1, mentionFiltered.length - 1));
        return;
      }
      if (e.key === "ArrowUp") {
        e.preventDefault();
        setMentionHighlight((h) => Math.max(h - 1, 0));
        return;
      }
      if (e.key === "Enter" && !e.nativeEvent.isComposing) {
        e.preventDefault();
        selectMention(mentionFiltered[mentionHighlight]);
        return;
      }
      if (e.key === "Escape") {
        e.preventDefault();
        setMentionOpen(false);
        return;
      }
    }
    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      handleSend();
    }
  }

  function handleAbort() {
    if (activeJobId) window.aidclaude.claude.abort(activeJobId);
  }

  async function handleOptionSelect(text: string, index: number) {
    const jobId = activeJobId;
    if (!jobId || streaming) return;

    // 직접 SQL 실행: query.sql에서 해당 옵션 SQL을 추출해 Claude 재호출 없이 바로 실행
    const options = await window.aidclaude.jobs.getSqlOptions(jobId);
    const matched = options[index] ?? options.find((o) => o.title === text);

    if (matched) {
      addChatMessage(jobId, {
        role: "user",
        text,
        status: "done",
        toolCalls: [],
        timestamp: new Date().toISOString(),
      });
      window.aidclaude.jobs.runSql(jobId, matched.sql).catch(() => {});
      return;
    }

    // 폴백: SQL 옵션을 찾지 못한 경우 Claude에게 재요청
    addChatMessage(jobId, {
      role: "user",
      text,
      status: "done",
      toolCalls: [],
      timestamp: new Date().toISOString(),
    });
    const assistantId = crypto.randomUUID();
    ensureAssistantMessage(assistantId);
    setStreaming({ jobId, assistantMessageId: assistantId });
    window.aidclaude.claude.sendMessage(jobId, text).catch(() => {});
  }

  function handleNewJob() {
    setActiveJob(null);
    setActiveCode("");
    setInput("");
  }

  const recentJobs = [...jobs].reverse().slice(0, 10);

  return (
    <div className="chat-panel">
      {/* Claude connection bar */}
      <ClaudeConnectBar />

      {/* Job tabs */}
      <div className="chat-tabs">
        <button
          type="button"
          className={`chat-tab-btn${activeJobId === null ? " chat-tab-btn-active" : ""}`}
          onClick={handleNewJob}
        >
          + 새 작업
        </button>
        {recentJobs.map((j) => (
          <button
            type="button"
            key={j.id}
            className={`chat-tab-btn chat-tab-btn-job${j.id === activeJobId ? " chat-tab-btn-active" : ""}`}
            onClick={() => {
              setActiveJob(j.id);
              // 탭 전환 시 data.db + CLAUDE.md를 최신 소스 카탈로그로 갱신 (background)
              window.aidclaude.jobs.refreshSources(j.id).catch(() => {});
            }}
            title={j.userRequest}
          >
            {j.userRequest.slice(0, 20)}…
          </button>
        ))}
      </div>

      {/* Messages */}
      <div className="cld-messages">
        {messages.length === 0 && (
          <p className="chat-empty-hint">분석 요청을 입력하세요</p>
        )}
        {messages.map((m) =>
          m.role === "user" ? (
            <article key={m.id} className="cld-msg cld-msg-user">
              <div className="cld-bubble">
                <div className="cld-msg-text">{m.text}</div>
              </div>
            </article>
          ) : (
            <article key={m.id} className={`cld-msg cld-msg-assistant cld-msg-${m.status}`}>
              {(m.text || m.status === "streaming") && (
                <div className="cld-turn">
                  <span
                    className={`cld-bullet ${
                      m.status === "streaming"
                        ? "cld-bullet-live"
                        : m.status === "error"
                        ? "cld-bullet-error"
                        : "cld-bullet-done"
                    }`}
                  />
                  <div className="cld-msg-text cld-md">
                    {!m.text ? (
                      <span className="cld-thinking">응답 생성 중…</span>
                    ) : m.status === "streaming" ? (
                      <ReactMarkdown remarkPlugins={[remarkGfm]} components={MARKDOWN_COMPONENTS}>
                        {m.text}
                      </ReactMarkdown>
                    ) : (
                      parseMessageParts(m.text).map((part, pi) =>
                        part.type === "text" ? (
                          <ReactMarkdown key={pi} remarkPlugins={[remarkGfm]} components={MARKDOWN_COMPONENTS}>
                            {part.content}
                          </ReactMarkdown>
                        ) : (
                          <div key={pi} className="cld-options">
                            {part.items.map((item, ii) => (
                              <button
                                key={ii}
                                type="button"
                                className="cld-option-btn"
                                disabled={!!streaming}
                                onClick={() => handleOptionSelect(item, ii)}
                              >
                                {item}
                              </button>
                            ))}
                          </div>
                        )
                      )
                    )}
                  </div>
                </div>
              )}
              {m.toolCalls.length > 0 && (
                <ul className="cld-tool-calls">
                  {m.toolCalls.map((c) => (
                    <li key={c.id} className={`cld-tool cld-tool-${c.status}`}>
                      <span
                        className={`cld-bullet ${
                          c.status === "done" ? "cld-bullet-done" : "cld-bullet-live"
                        }`}
                      />
                      <span className="cld-tool-name">{c.name}</span>
                      <span className="cld-tool-summary">{c.summary}</span>
                    </li>
                  ))}
                </ul>
              )}
              {m.status === "error" && m.error && (
                <div className="cld-msg-error">⚠ {m.error}</div>
              )}
            </article>
          )
        )}
        <div ref={endRef} />
      </div>

      {/* Composer */}
      <div className={`cld-composer ${streaming ? "cld-composer-streaming" : ""}`}>
        <div className="cld-composer-frame">
          {/* @-mention 팝업 */}
          {mentionOpen && mentionFiltered.length > 0 && (
            <div ref={mentionRef} className="cld-mention-popup">
              {mentionFiltered.map((s, i) => (
                <button
                  key={s.id}
                  type="button"
                  className={`cld-mention-item${i === mentionHighlight ? " cld-mention-item-active" : ""}`}
                  onMouseDown={(e) => { e.preventDefault(); selectMention(s); }}
                  onMouseEnter={() => setMentionHighlight(i)}
                >
                  <span className="cld-mention-name">@{s.name}</span>
                  <span className="cld-mention-type">{s.type}</span>
                </button>
              ))}
            </div>
          )}
          <div className="cld-input-wrap">
            <textarea
              ref={textareaRef}
              className="cld-composer-input"
              value={input}
              onChange={handleInputChange}
              onKeyDown={handleKeyDown}
              onBlur={() => setTimeout(() => setMentionOpen(false), 150)}
              placeholder={
                streaming ? "Claude가 응답 중입니다…" : "분석 요청을 입력하세요 (@소스명으로 소스 지정, Enter: 전송)"
              }
              rows={3}
              disabled={!!streaming}
            />
          </div>
          <div className="cld-composer-actions">
            <div className="cld-actions-spacer" />
            {streaming ? (
              <button type="button" className="cld-send-btn cld-send-stop" onClick={handleAbort}>
                ■
              </button>
            ) : (
              <button
                type="button"
                className="cld-send-btn"
                onClick={handleSend}
                disabled={!input.trim()}
              >
                ↑
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
