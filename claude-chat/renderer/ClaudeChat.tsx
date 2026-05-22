import { useState, useEffect, useRef, useMemo } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { useChatStore } from "./useChatStore";
import type { MentionItem, ChatSession } from "../shared/chat-types";
import "./chat.css";

// ─── Markdown link ───────────────────────────────────────────────────────────
const MARKDOWN_COMPONENTS = {
  a: ({ href, children, ...rest }: React.ComponentPropsWithoutRef<"a">) => (
    <a href={href} target="_blank" rel="noopener noreferrer" {...rest}>
      {children}
    </a>
  ),
};

// ─── Options tag parser ───────────────────────────────────────────────────────
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

// ─── Raw event types from Claude CLI ─────────────────────────────────────────
interface RawAssistantEvent {
  type: "assistant";
  message: {
    content: Array<
      | { type: "text"; text: string }
      | { type: "tool_use"; id: string; name: string; input: Record<string, unknown> }
    >;
  };
}
interface RawToolResultEvent {
  type: "tool_result";
  tool_use_id: string;
}
interface RawResultEvent {
  type: "result";
  session_id: string;
}

// ─── ClaudeConnectBar ─────────────────────────────────────────────────────────
type ProbeStatus = "idle" | "checking" | "ok" | "error";

function ClaudeConnectBar() {
  const { probe, setProbe } = useChatStore();
  const [status, setStatus] = useState<ProbeStatus>(
    probe ? (probe.authenticated ? "ok" : "error") : "idle"
  );

  async function handleProbe() {
    setStatus("checking");
    try {
      const result = await window.claudeChat.probe();
      setProbe(result);
      setStatus(result.authenticated ? "ok" : "error");
    } catch {
      setStatus("error");
    }
  }

  useEffect(() => {
    if (status === "idle") handleProbe();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const label =
    status === "checking"
      ? "확인 중…"
      : status === "ok"
      ? `✓ Claude ${probe?.version ?? ""}`
      : status === "error"
      ? `✗ ${probe?.error ?? "연결 실패"}`
      : "Claude 연결 확인";

  const barClass =
    status === "ok"
      ? "claude-bar claude-bar-ok"
      : status === "error"
      ? "claude-bar claude-bar-error"
      : "claude-bar";

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

// ─── Props ────────────────────────────────────────────────────────────────────
export interface ClaudeChatProps {
  /** Working directory for Claude. If omitted, a temp dir is created per session. */
  cwd?: string;
  /** Items to show in the @-mention popup. */
  mentionItems?: MentionItem[];
  /** Called when a new session is created. */
  onSessionCreate?: (session: ChatSession) => void;
  /**
   * Called when an option button is clicked.
   * Return `true` to indicate the host handled it (skips default Claude fallback).
   */
  onOptionSelect?: (
    sessionId: string,
    option: string,
    index: number
  ) => Promise<boolean>;
  /** Called when a @mention item is selected from the popup. */
  onMentionSelect?: (sessionId: string, item: MentionItem) => void;
  /** Textarea placeholder shown when not streaming. */
  placeholder?: string;
  /** Text shown when the message list is empty. */
  emptyHint?: string;
}

// ─── Main component ───────────────────────────────────────────────────────────
export function ClaudeChat({
  cwd,
  mentionItems = [],
  onSessionCreate,
  onOptionSelect,
  onMentionSelect,
  placeholder = "메시지를 입력하세요 (@항목으로 멘션, Enter: 전송, Shift+Enter: 줄바꿈)",
  emptyHint = "Claude에게 질문하거나 작업을 요청해보세요",
}: ClaudeChatProps) {
  const {
    sessions,
    activeSessionId,
    messages,
    streaming,
    addSession,
    setActiveSession,
    addMessage,
    ensureAssistantMessage,
    appendAssistantText,
    addToolCall,
    updateToolCall,
    finalizeAssistantMessage,
    setStreaming,
  } = useChatStore();

  const [input, setInput] = useState("");
  const endRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // @-mention popup state
  const [mentionOpen, setMentionOpen] = useState(false);
  const [mentionQuery, setMentionQuery] = useState("");
  const [mentionStart, setMentionStart] = useState(0);
  const [mentionHighlight, setMentionHighlight] = useState(0);

  const currentMessages = activeSessionId
    ? (messages.get(activeSessionId) ?? [])
    : [];

  // Auto-scroll on new messages
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [currentMessages]);

  // Subscribe to IPC events for the active session
  useEffect(() => {
    if (!activeSessionId) return;
    let assistantId = "";

    const unsubStream = window.claudeChat.onStream(activeSessionId, (raw) => {
      const ev = raw as RawAssistantEvent | RawToolResultEvent | RawResultEvent;

      if (ev.type === "assistant") {
        for (const part of (ev as RawAssistantEvent).message.content) {
          if (part.type === "text" && part.text) {
            if (!assistantId) {
              assistantId = crypto.randomUUID();
              ensureAssistantMessage(assistantId);
            }
            appendAssistantText(assistantId, part.text);
          } else if (part.type === "tool_use") {
            if (!assistantId) {
              assistantId = crypto.randomUUID();
              ensureAssistantMessage(assistantId);
            }
            addToolCall(assistantId, {
              id: part.id,
              name: part.name,
              summary: JSON.stringify(part.input).slice(0, 120),
              status: "running",
            });
          }
        }
      } else if (ev.type === "tool_result") {
        if (assistantId) {
          updateToolCall(assistantId, (ev as RawToolResultEvent).tool_use_id, {
            status: "done",
          });
        }
      }
    });

    const unsubDone = window.claudeChat.onDone(activeSessionId, () => {
      if (assistantId) finalizeAssistantMessage(assistantId, "done");
      assistantId = "";
      setStreaming(null);
    });

    const unsubError = window.claudeChat.onError(activeSessionId, (err) => {
      if (assistantId) finalizeAssistantMessage(assistantId, "error", err.message);
      assistantId = "";
      setStreaming(null);
    });

    return () => {
      unsubStream();
      unsubDone();
      unsubError();
    };
  }, [activeSessionId]); // eslint-disable-line react-hooks/exhaustive-deps

  // Filtered mention items
  const mentionFiltered = useMemo(() => {
    if (!mentionOpen) return [];
    const q = mentionQuery.toLowerCase();
    return mentionItems.filter((item) =>
      item.name.toLowerCase().includes(q)
    );
  }, [mentionOpen, mentionQuery, mentionItems]);

  useEffect(() => {
    setMentionHighlight(0);
  }, [mentionFiltered]);

  async function handleSend() {
    if (!input.trim() || streaming) return;
    if (mentionOpen) return;
    const text = input.trim();
    setInput("");
    setMentionOpen(false);

    let sessionId = activeSessionId;
    if (!sessionId) {
      const session = await window.claudeChat.createSession({ cwd });
      addSession(session);
      setActiveSession(session.id);
      onSessionCreate?.(session);
      sessionId = session.id;
    }

    addMessage(sessionId, {
      id: crypto.randomUUID(),
      role: "user",
      text,
      status: "done",
      toolCalls: [],
      timestamp: new Date().toISOString(),
    });

    const aId = crypto.randomUUID();
    ensureAssistantMessage(aId);
    setStreaming({ sessionId, assistantMessageId: aId });

    window.claudeChat.sendMessage(sessionId, text).catch(() => {});
  }

  function handleInputChange(e: React.ChangeEvent<HTMLTextAreaElement>) {
    const value = e.target.value;
    setInput(value);
    const cursor = e.target.selectionStart ?? value.length;
    const before = value.slice(0, cursor);
    const atMatch = before.match(/@([^\s@]*)$/);
    if (atMatch) {
      setMentionQuery(atMatch[1]);
      setMentionStart(cursor - atMatch[0].length);
      setMentionOpen(true);
    } else {
      setMentionOpen(false);
    }
  }

  function selectMention(item: MentionItem) {
    const after = input.slice(mentionStart + 1 + mentionQuery.length);
    const newInput = input.slice(0, mentionStart) + `@${item.name} ` + after;
    setInput(newInput);
    setMentionOpen(false);
    if (activeSessionId) onMentionSelect?.(activeSessionId, item);
    const pos = mentionStart + item.name.length + 2;
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
    if (activeSessionId) window.claudeChat.abort(activeSessionId);
  }

  async function handleOptionSelect(text: string, index: number) {
    const sessionId = activeSessionId;
    if (!sessionId || streaming) return;

    if (onOptionSelect) {
      const handled = await onOptionSelect(sessionId, text, index);
      if (handled) return;
    }

    // Default: send option text as a new message to Claude
    addMessage(sessionId, {
      id: crypto.randomUUID(),
      role: "user",
      text,
      status: "done",
      toolCalls: [],
      timestamp: new Date().toISOString(),
    });
    const aId = crypto.randomUUID();
    ensureAssistantMessage(aId);
    setStreaming({ sessionId, assistantMessageId: aId });
    window.claudeChat.sendMessage(sessionId, text).catch(() => {});
  }

  function handleNewSession() {
    setActiveSession(null);
    setInput("");
  }

  const recentSessions = [...sessions].reverse().slice(0, 10);

  return (
    <div className="chat-panel">
      <ClaudeConnectBar />

      {/* Session tabs */}
      <div className="chat-tabs">
        <button
          type="button"
          className={`chat-tab-btn${activeSessionId === null ? " chat-tab-btn-active" : ""}`}
          onClick={handleNewSession}
        >
          + 새 대화
        </button>
        {recentSessions.map((s) => (
          <button
            type="button"
            key={s.id}
            className={`chat-tab-btn chat-tab-btn-job${
              s.id === activeSessionId ? " chat-tab-btn-active" : ""
            }`}
            onClick={() => setActiveSession(s.id)}
            title={s.label}
          >
            {s.label.slice(0, 20)}
          </button>
        ))}
      </div>

      {/* Messages */}
      <div className="cld-messages">
        {currentMessages.length === 0 && (
          <p className="chat-empty-hint">{emptyHint}</p>
        )}
        {currentMessages.map((m) =>
          m.role === "user" ? (
            <article key={m.id} className="cld-msg cld-msg-user">
              <div className="cld-bubble">
                <div className="cld-msg-text">{m.text}</div>
              </div>
            </article>
          ) : (
            <article
              key={m.id}
              className={`cld-msg cld-msg-assistant cld-msg-${m.status}`}
            >
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
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        components={MARKDOWN_COMPONENTS}
                      >
                        {m.text}
                      </ReactMarkdown>
                    ) : (
                      parseMessageParts(m.text).map((part, pi) =>
                        part.type === "text" ? (
                          <ReactMarkdown
                            key={pi}
                            remarkPlugins={[remarkGfm]}
                            components={MARKDOWN_COMPONENTS}
                          >
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
                          c.status === "done"
                            ? "cld-bullet-done"
                            : "cld-bullet-live"
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
          {/* @-mention popup */}
          {mentionOpen && mentionFiltered.length > 0 && (
            <div className="cld-mention-popup">
              {mentionFiltered.map((item, i) => (
                <button
                  key={item.id}
                  type="button"
                  className={`cld-mention-item${
                    i === mentionHighlight ? " cld-mention-item-active" : ""
                  }`}
                  onMouseDown={(e) => {
                    e.preventDefault();
                    selectMention(item);
                  }}
                  onMouseEnter={() => setMentionHighlight(i)}
                >
                  <span className="cld-mention-name">@{item.name}</span>
                  <span className="cld-mention-type">{item.type}</span>
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
              placeholder={streaming ? "Claude가 응답 중입니다…" : placeholder}
              rows={3}
              disabled={!!streaming}
            />
          </div>
          <div className="cld-composer-actions">
            <div className="cld-actions-spacer" />
            {streaming ? (
              <button
                type="button"
                className="cld-send-btn cld-send-stop"
                onClick={handleAbort}
              >
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
