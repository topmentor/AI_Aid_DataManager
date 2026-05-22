/** @멘션 대상 아이템 — 호스트 앱이 임의 데이터를 주입 */
export interface MentionItem {
  id: string;
  name: string;
  type: string;
}

/** 채팅 세션 (job 개념의 제네릭 대체) */
export interface ChatSession {
  id: string;
  cwd: string;
  label: string;
  createdAt: string;
}

/** Claude CLI 환경 탐지 결과 */
export interface ClaudeProbe {
  binaryPath: string | null;
  version: string | null;
  authenticated: boolean;
  roundTripMs: number | null;
  error: string | null;
}

/** renderer에서 window.claudeChat 타입 선언용 */
export interface ClaudeChatApi {
  probe(): Promise<ClaudeProbe>;
  createSession(opts?: { cwd?: string; label?: string }): Promise<ChatSession>;
  listSessions(): Promise<ChatSession[]>;
  sendMessage(sessionId: string, message: string): Promise<void>;
  abort(sessionId: string): Promise<void>;
  onStream(sessionId: string, fn: (event: unknown) => void): () => void;
  onDone(sessionId: string, fn: () => void): () => void;
  onError(sessionId: string, fn: (err: { message: string }) => void): () => void;
}
