import { useState, useEffect, useRef } from "react";
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

  const messages = activeJobId ? (chatMessages.get(activeJobId) ?? []) : [];

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function handleSend() {
    if (!input.trim() || streaming) return;
    const text = input.trim();
    setInput("");

    let jobId = activeJobId;
    if (!jobId) {
      const job = await window.aidclaude.jobs.create(text, sources.map((s) => s.id));
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

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
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
          <div className="cld-input-wrap">
            <textarea
              ref={textareaRef}
              className="cld-composer-input"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={
                streaming ? "Claude가 응답 중입니다…" : "분석 요청을 입력하세요 (Enter: 전송, Shift+Enter: 줄바꿈)"
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
