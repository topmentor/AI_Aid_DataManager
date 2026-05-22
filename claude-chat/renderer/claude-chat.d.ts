import type { ClaudeProbe, ChatSession } from "../shared/chat-types";

declare global {
  interface Window {
    claudeChat: {
      probe(): Promise<ClaudeProbe>;
      createSession(opts?: { cwd?: string; label?: string }): Promise<ChatSession>;
      listSessions(): Promise<ChatSession[]>;
      sendMessage(sessionId: string, message: string): Promise<void>;
      abort(sessionId: string): Promise<void>;
      /** Returns a cleanup (unsubscribe) function. */
      onStream(sessionId: string, fn: (event: unknown) => void): () => void;
      onDone(sessionId: string, fn: () => void): () => void;
      onError(sessionId: string, fn: (err: { message: string }) => void): () => void;
    };
  }
}
