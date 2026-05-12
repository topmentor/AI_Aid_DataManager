import { useEffect } from "react";
import { DataSourcePanel } from "./DataSourcePanel";
import { ChatPanel } from "./ChatPanel";
import { CodePanel } from "./CodePanel";
import { ResultPanel } from "./ResultPanel";
import { useAppStore } from "../store/appStore";
import type { Job } from "../../../shared/types";

function summarizeToolInput(name: string, input: Record<string, unknown>): string {
  if (name === "Read" || name === "Write" || name === "Edit") {
    const p = input.file_path;
    if (typeof p === "string") return p.replace(/\\/g, "/").split("/").pop() ?? p;
  }
  return "";
}

export function ProjectWindow() {
  useEffect(() => {
    const store = useAppStore.getState;

    const onStream = (...args: unknown[]) => {
      const { jobId, event } = args[0] as { jobId: string; event: Record<string, unknown> };
      const current = store().streaming;
      if (!current || current.jobId !== jobId) return;
      const assistantId = current.assistantMessageId;
      const type = event.type as string | undefined;

      if (type === "assistant") {
        const message = event.message as { content?: Array<Record<string, unknown>> } | undefined;
        for (const block of message?.content ?? []) {
          if (block.type === "text" && typeof block.text === "string") {
            store().appendAssistantText(assistantId, block.text);
          } else if (block.type === "tool_use") {
            const name = (block.name as string) ?? "tool";
            const input = (block.input as Record<string, unknown>) ?? {};
            const id = (block.id as string) ?? crypto.randomUUID();
            store().upsertToolCall(assistantId, {
              id,
              name,
              summary: summarizeToolInput(name, input),
              status: "running",
            });
          }
        }
      } else if (type === "user") {
        const message = event.message as { content?: Array<Record<string, unknown>> } | undefined;
        for (const block of message?.content ?? []) {
          if (block.type === "tool_result" && typeof block.tool_use_id === "string") {
            const msg = store().chatMessages.get(jobId)?.find((m) => m.id === assistantId);
            const tc = msg?.toolCalls.find((c) => c.id === block.tool_use_id);
            if (tc) store().upsertToolCall(assistantId, { ...tc, status: "done" });
          }
        }
      }
    };

    const onDone = (...args: unknown[]) => {
      const { jobId } = args[0] as { jobId: string };
      const current = store().streaming;
      if (!current || current.jobId !== jobId) return;
      store().markAssistantDone(current.assistantMessageId);
    };

    const onError = (...args: unknown[]) => {
      const { jobId, error } = args[0] as { jobId: string; error: string };
      const current = store().streaming;
      if (!current || current.jobId !== jobId) return;
      store().markAssistantError(current.assistantMessageId, error);
    };

    const onJobUpdate = (...args: unknown[]) => {
      const job = args[0] as Job;
      useAppStore.getState().updateJob(job);
    };

    const onAnalyzeCode = (...args: unknown[]) => {
      const payload = args[0] as { jobId: string; code: string };
      const activeJobId = useAppStore.getState().activeJobId;
      if (payload.jobId === activeJobId) {
        useAppStore.getState().setActiveCode(payload.code);
      }
    };

    window.aidclaude.on("claude:stream", onStream);
    window.aidclaude.on("claude:done", onDone);
    window.aidclaude.on("claude:error", onError);
    window.aidclaude.on("job:update", onJobUpdate);
    window.aidclaude.on("job:analyze_code", onAnalyzeCode);

    return () => {
      window.aidclaude.off("claude:stream", onStream);
      window.aidclaude.off("claude:done", onDone);
      window.aidclaude.off("claude:error", onError);
      window.aidclaude.off("job:update", onJobUpdate);
      window.aidclaude.off("job:analyze_code", onAnalyzeCode);
    };
  }, []);

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "240px 1fr 320px",
        height: "100vh",
        overflow: "hidden",
      }}
    >
      {/* Left: Data Source Panel */}
      <div style={{ borderRight: "1px solid #333", overflowY: "auto", background: "#252525" }}>
        <DataSourcePanel />
      </div>

      {/* Center: Chat (top) + Code (bottom) */}
      <div style={{ display: "grid", gridTemplateRows: "55% 45%", overflow: "hidden" }}>
        <ChatPanel />
        <CodePanel />
      </div>

      {/* Right: Results Panel */}
      <div style={{ borderLeft: "1px solid #333", overflowY: "auto" }}>
        <ResultPanel />
      </div>
    </div>
  );
}
