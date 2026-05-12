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

  // Auto-scroll to bottom
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

    // Fire-and-forget — completion signaled by claude:done / claude:error
    window.aidclaude.claude.sendMessage(jobId, text).catch(() => {
      // Swallow IPC-level errors; claude:error event handles messaging to the user
    });
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

  function handleNewJob() {
    setActiveJob(null);
    setActiveCode("");
    setInput("");
  }

  // Job history tabs
  const recentJobs = [...jobs].reverse().slice(0, 10);

  return (
    <div className="chat-panel">
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
            onClick={() => setActiveJob(j.id)}
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
                    {m.text ? (
                      <ReactMarkdown remarkPlugins={[remarkGfm]} components={MARKDOWN_COMPONENTS}>
                        {m.text}
                      </ReactMarkdown>
                    ) : (
                      <span className="cld-thinking">응답 생성 중…</span>
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
