import { useState, useEffect, useRef } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { useAppStore } from "../store/appStore";
import type { ClaudeStreamEvent, Job } from "../../../shared/types";

export function ChatPanel() {
  const {
    sources, jobs, activeJobId, chatMessages,
    addJob, setActiveJob, addChatMessage, updateJob,
    setActiveCode,
  } = useAppStore();
  const [input, setInput] = useState("");
  const [isRunning, setIsRunning] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const messages = activeJobId ? chatMessages.get(activeJobId) ?? [] : [];

  // Auto-scroll to bottom
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Listen to Claude streaming events
  useEffect(() => {
    const handleStream = (...args: unknown[]) => {
      const payload = args[0] as { jobId: string; event: ClaudeStreamEvent };
      if (payload.jobId !== activeJobId) return;
      const { event } = payload;
      if (event.type === "assistant" && event.text) {
        // Append to last assistant message or create new one
        const msgs = useAppStore.getState().chatMessages.get(payload.jobId) ?? [];
        const last = msgs[msgs.length - 1];
        if (last?.role === "assistant") {
          // Update last message in place by replacing it
          const updated = [...msgs.slice(0, -1), { ...last, text: last.text + event.text }];
          // Direct store update via Zustand
          useAppStore.setState((s) => {
            const m = new Map(s.chatMessages);
            m.set(payload.jobId, updated);
            return { chatMessages: m };
          });
        } else {
          addChatMessage(payload.jobId, {
            role: "assistant",
            text: event.text,
            timestamp: new Date().toISOString(),
          });
        }
      } else if (event.type === "result" || event.type === "error") {
        setIsRunning(false);
        if (event.type === "error") {
          addChatMessage(payload.jobId, {
            role: "assistant",
            text: `❌ 오류: ${event.message}`,
            timestamp: new Date().toISOString(),
          });
        }
      }
    };

    const handleJobUpdate = (...args: unknown[]) => {
      const job = args[0] as Job;
      if (job.id === activeJobId) updateJob(job);
    };

    const handleAnalyzeCode = (...args: unknown[]) => {
      const payload = args[0] as { jobId: string; code: string };
      if (payload.jobId === activeJobId) setActiveCode(payload.code);
    };

    window.aidclaude.on("claude:stream", handleStream);
    window.aidclaude.on("job:update", handleJobUpdate);
    window.aidclaude.on("job:analyze_code", handleAnalyzeCode);

    return () => {
      window.aidclaude.off("claude:stream", handleStream);
      window.aidclaude.off("job:update", handleJobUpdate);
      window.aidclaude.off("job:analyze_code", handleAnalyzeCode);
    };
  }, [activeJobId]);

  async function handleSend() {
    if (!input.trim() || isRunning) return;
    const text = input.trim();
    setInput("");

    let jobId = activeJobId;
    if (!jobId) {
      // Create new job
      const job = await window.aidclaude.jobs.create(text, sources.map((s) => s.id));
      addJob(job);
      setActiveJob(job.id);
      jobId = job.id;
    }

    addChatMessage(jobId, {
      role: "user",
      text,
      timestamp: new Date().toISOString(),
    });

    setIsRunning(true);
    try {
      await window.aidclaude.claude.sendMessage(jobId, text);
    } catch (e) {
      setIsRunning(false);
      addChatMessage(jobId, {
        role: "assistant",
        text: `❌ 오류: ${(e as Error).message}`,
        timestamp: new Date().toISOString(),
      });
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      handleSend();
    }
  }

  function handleNewJob() {
    setActiveJob(null);
    setActiveCode("");
    setInput("");
    setIsRunning(false);
  }

  // Job history tabs
  const recentJobs = [...jobs].reverse().slice(0, 10);

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%", borderBottom: "1px solid #333" }}>
      {/* Job tabs */}
      <div
        style={{
          display: "flex",
          overflowX: "auto",
          background: "#252525",
          borderBottom: "1px solid #333",
          minHeight: 32,
          alignItems: "center",
          padding: "0 6px",
          gap: 4,
        }}
      >
        <button
          onClick={handleNewJob}
          style={{
            fontSize: 11, padding: "2px 8px", whiteSpace: "nowrap",
            background: activeJobId === null ? "#0e639c" : "#3c3c3c",
          }}
        >
          + 새 작업
        </button>
        {recentJobs.map((j) => (
          <button
            key={j.id}
            onClick={() => setActiveJob(j.id)}
            title={j.userRequest}
            style={{
              fontSize: 11, padding: "2px 8px", maxWidth: 120,
              overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
              background: j.id === activeJobId ? "#0e639c" : "#3c3c3c",
            }}
          >
            {j.userRequest.slice(0, 20)}…
          </button>
        ))}
      </div>

      {/* Messages */}
      <div style={{ flex: 1, overflowY: "auto", padding: "10px 12px" }}>
        {messages.length === 0 && (
          <p style={{ color: "#555", fontSize: 12, textAlign: "center", marginTop: 20 }}>
            분석 요청을 입력하세요
          </p>
        )}
        {messages.map((m, i) => (
          <div
            key={i}
            style={{
              marginBottom: 10,
              display: "flex",
              justifyContent: m.role === "user" ? "flex-end" : "flex-start",
            }}
          >
            <div
              style={{
                maxWidth: "85%",
                padding: "7px 12px",
                borderRadius: m.role === "user" ? "12px 12px 2px 12px" : "12px 12px 12px 2px",
                background: m.role === "user" ? "#0e639c" : "#2d2d2d",
                fontSize: 13,
                lineHeight: 1.5,
              }}
            >
              {m.role === "assistant" ? (
                <ReactMarkdown remarkPlugins={[remarkGfm]}>{m.text}</ReactMarkdown>
              ) : (
                m.text
              )}
            </div>
          </div>
        ))}
        <div ref={endRef} />
      </div>

      {/* Input area */}
      <div
        style={{
          padding: "8px 10px",
          borderTop: "1px solid #333",
          display: "flex",
          gap: 6,
          background: "#252525",
        }}
      >
        <textarea
          ref={textareaRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="분석 요청 입력 (Enter: 전송, Shift+Enter: 줄바꿈)"
          rows={3}
          style={{ flex: 1, resize: "none", fontSize: 13, lineHeight: 1.4 }}
          disabled={isRunning}
        />
        <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          <button
            onClick={handleSend}
            disabled={isRunning || !input.trim()}
            style={{ flex: 1 }}
          >
            전송
          </button>
          {isRunning && (
            <button
              onClick={() => activeJobId && window.aidclaude.claude.abort(activeJobId)}
              style={{ background: "#8b2525", flex: 1 }}
            >
              중단
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
