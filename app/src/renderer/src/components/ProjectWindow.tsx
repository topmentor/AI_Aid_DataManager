import { useEffect, useRef, useState } from "react";
import { DataSourcePanel } from "./DataSourcePanel";
import { ChatPanel } from "./ChatPanel";
import { CodePanel } from "./CodePanel";
import { CenterPanel } from "./CenterPanel";
import { useAppStore } from "../store/appStore";
import type { Job } from "../../../shared/types";

// ── Drag utilities ──────────────────────────────────────────────────────────

function colDrag(
  e: React.MouseEvent,
  startSize: number,
  direction: 1 | -1,
  min: number,
  max: number,
  setSize: (n: number) => void
) {
  e.preventDefault();
  const startX = e.clientX;
  document.body.style.cursor = "col-resize";
  document.body.style.userSelect = "none";

  const onMove = (ev: MouseEvent) => {
    const next = startSize + (ev.clientX - startX) * direction;
    setSize(Math.max(min, Math.min(max, next)));
  };
  const onUp = () => {
    document.body.style.cursor = "";
    document.body.style.userSelect = "";
    window.removeEventListener("mousemove", onMove);
    window.removeEventListener("mouseup", onUp);
  };
  window.addEventListener("mousemove", onMove);
  window.addEventListener("mouseup", onUp);
}

function rowDrag(
  e: React.MouseEvent,
  containerEl: HTMLElement | null,
  startRatio: number,
  min: number,
  max: number,
  setRatio: (r: number) => void
) {
  e.preventDefault();
  const startY = e.clientY;
  const h = containerEl?.offsetHeight ?? 600;
  document.body.style.cursor = "row-resize";
  document.body.style.userSelect = "none";

  const onMove = (ev: MouseEvent) => {
    const next = startRatio + (ev.clientY - startY) / h;
    setRatio(Math.max(min, Math.min(max, next)));
  };
  const onUp = () => {
    document.body.style.cursor = "";
    document.body.style.userSelect = "";
    window.removeEventListener("mousemove", onMove);
    window.removeEventListener("mouseup", onUp);
  };
  window.addEventListener("mousemove", onMove);
  window.addEventListener("mouseup", onUp);
}

// ── Handle components ───────────────────────────────────────────────────────

function VHandle({ onMouseDown }: { onMouseDown: (e: React.MouseEvent) => void }) {
  return (
    <div className="pw-vhandle" onMouseDown={onMouseDown}>
      <span className="pw-grip pw-vgrip" />
    </div>
  );
}

function HHandle({ onMouseDown }: { onMouseDown: (e: React.MouseEvent) => void }) {
  return (
    <div className="pw-hhandle" onMouseDown={onMouseDown}>
      <span className="pw-grip pw-hgrip" />
    </div>
  );
}

// ── IPC event helpers ───────────────────────────────────────────────────────

function summarizeToolInput(name: string, input: Record<string, unknown>): string {
  if (name === "Read" || name === "Write" || name === "Edit") {
    const p = input.file_path;
    if (typeof p === "string") return p.replace(/\\/g, "/").split("/").pop() ?? p;
  }
  return "";
}

// ── ProjectWindow ────────────────────────────────────────────────────────────

export function ProjectWindow() {
  const [leftW,    setLeftW]    = useState(240);
  const [rightW,   setRightW]   = useState(380);
  const [chatRatio, setChatRatio] = useState(0.55);
  const rightRef = useRef<HTMLDivElement>(null);

  // ── IPC event subscriptions ─────────────────────────────────────────────
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
        const blocks = message?.content ?? [];
        // 같은 이벤트 안에 tool_use가 있으면 text는 "앞으로 할 일" 예고이므로 건너뜀
        // (도구 실행 후 후속 이벤트에서 같은 내용이 다시 오면 중복이 됨)
        const hasToolUse = blocks.some((b) => b.type === "tool_use");
        for (const block of blocks) {
          if (block.type === "text" && typeof block.text === "string" && !hasToolUse) {
            store().appendAssistantText(assistantId, block.text);
          } else if (block.type === "tool_use") {
            const name  = (block.name as string) ?? "tool";
            const input = (block.input as Record<string, unknown>) ?? {};
            const id    = (block.id as string) ?? crypto.randomUUID();
            store().upsertToolCall(assistantId, {
              id, name, summary: summarizeToolInput(name, input), status: "running",
            });
          }
        }
      } else if (type === "user") {
        const message = event.message as { content?: Array<Record<string, unknown>> } | undefined;
        for (const block of message?.content ?? []) {
          if (block.type === "tool_result" && typeof block.tool_use_id === "string") {
            const msg = store().chatMessages.get(jobId)?.find((m) => m.id === assistantId);
            const tc  = msg?.toolCalls.find((c) => c.id === block.tool_use_id);
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
      useAppStore.getState().updateJob(args[0] as Job);
    };

    const onAnalyzeCode = (...args: unknown[]) => {
      const payload = args[0] as { jobId: string; code: string };
      const s = useAppStore.getState();
      if (payload.jobId === s.activeJobId) s.setActiveCode(payload.code);
    };

    window.aidclaude.on("claude:stream",     onStream);
    window.aidclaude.on("claude:done",       onDone);
    window.aidclaude.on("claude:error",      onError);
    window.aidclaude.on("job:update",        onJobUpdate);
    window.aidclaude.on("job:analyze_code",  onAnalyzeCode);
    return () => {
      window.aidclaude.off("claude:stream",    onStream);
      window.aidclaude.off("claude:done",      onDone);
      window.aidclaude.off("claude:error",     onError);
      window.aidclaude.off("job:update",       onJobUpdate);
      window.aidclaude.off("job:analyze_code", onAnalyzeCode);
    };
  }, []);

  return (
    <div
      className="pw-root"
      style={{ "--pw-left-w": `${leftW}px`, "--pw-right-w": `${rightW}px` } as React.CSSProperties}
    >
      {/* ── Left: DataSource ── */}
      <div className="pw-left">
        <DataSourcePanel />
      </div>

      <VHandle onMouseDown={(e) => colDrag(e, leftW,   1,  160, 480, setLeftW)} />

      {/* ── Center: Content tabs ── */}
      <div className="pw-center">
        <CenterPanel />
      </div>

      <VHandle onMouseDown={(e) => colDrag(e, rightW, -1,  240, 640, setRightW)} />

      {/* ── Right: Chat + Code ── */}
      <div
        className="pw-right"
        ref={rightRef}
        style={{ "--pw-chat-ratio": `${chatRatio * 100}%` } as React.CSSProperties}
      >
        <ChatPanel />
        <HHandle onMouseDown={(e) =>
          rowDrag(e, rightRef.current, chatRatio, 0.2, 0.85, setChatRatio)
        } />
        <CodePanel />
      </div>
    </div>
  );
}
