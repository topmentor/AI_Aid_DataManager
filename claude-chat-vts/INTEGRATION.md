# Claude Chat Module — Vanilla TypeScript 통합 가이드

React / Zustand 의존성 없이 순수 TypeScript 클래스로 동작하는 Claude 채팅 위젯입니다.  
`claude-chat/` (React 버전)과 동일한 UI와 기능을 제공하지만, 외부 UI 라이브러리가 필요 없습니다.

---

## 폴더 구조

```
claude-chat-vts/
├── shared/
│   └── chat-types.ts           # MentionItem, ChatSession, ClaudeProbe, ClaudeChatApi
├── main/
│   ├── claude-bridge.ts        # Claude CLI subprocess + NDJSON async iterator
│   ├── claude-detector.ts      # CLI 탐지 + round-trip probe
│   ├── claude-chat-service.ts  # 세션 생성, 메시지 전송, abort
│   └── register-ipc.ts         # IPC 핸들러 일괄 등록
├── preload/
│   └── claude-chat-preload.ts  # contextBridge window.claudeChat
└── renderer/
    ├── ClaudeChat.ts           # 바닐라 TS 위젯 클래스 (핵심)
    ├── chat.css                # 채팅 UI 스타일 (.cld-*, .claude-bar*, .chat-*)
    └── claude-chat.d.ts        # window.claudeChat 전역 타입 선언
```

---

## React 버전(`claude-chat/`)과의 차이점

| 항목 | React 버전 (`claude-chat/`) | Vanilla TS 버전 (`claude-chat-vts/`) |
|------|---------------------------|--------------------------------------|
| UI 프레임워크 | React 18 + JSX | 없음 (DOM API 직접 조작) |
| 상태 관리 | Zustand (`useChatStore`) | 클래스 인스턴스 내부 private 필드 |
| 마크다운 렌더링 | `react-markdown` + `remark-gfm` | 내장 미니 렌더러 (`renderMarkdown()`) |
| 번들러 | electron-vite (Vite) | 프로젝트 번들러에 종속 |
| 사용 방법 | `<ClaudeChat ... />` JSX | `new ClaudeChat('#id', opts)` |
| 정리(cleanup) | React `useEffect` cleanup | `instance.destroy()` 명시적 호출 |
| main / preload | 동일 | 동일 (공유 가능) |

> **언제 이 버전을 선택하나요?**  
> 기존 Electron 프로젝트가 React 없이 바닐라 JS/TS로 구성되어 있거나, React 도입 없이 채팅 위젯만 추가하고 싶을 때.

---

## 요구 사항

| 항목 | 최소 버전 |
|------|----------|
| Electron | 25+ |
| Node.js | 18+ |
| TypeScript | 5+ |
| Claude Code CLI | 최신 (`claude login` 완료) |

추가 npm 패키지 **없음** — 번들러(webpack, vite, esbuild 등)만 있으면 됩니다.

---

## Step 1 — Main 프로세스에 IPC 등록

`src/main/index.ts` (또는 `main.js`)에서 BrowserWindow 생성 후 한 줄 추가:

```typescript
import { registerClaudeChatIpc } from "../../claude-chat-vts/main/register-ipc.js";

function createWindow() {
  const win = new BrowserWindow({
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
    },
  });
  // ...

  // ★ BrowserWindow 생성 직후 등록
  registerClaudeChatIpc(win, {
    claudeBin: "claude",        // Claude CLI 실행파일 경로 (생략 시 PATH에서 탐색)
    probeDir: app.getPath("userData"),  // probe용 작업 디렉터리
  });
}
```

### RegisterClaudeChatOptions

| 옵션 | 타입 | 설명 |
|------|------|------|
| `claudeBin` | `string?` | Claude CLI 경로. 생략 시 `CLAUDE_BIN` 환경변수 → `"claude"` 순서 |
| `probeDir` | `string?` | `probe()` 호출 시 사용할 작업 디렉터리. 생략 시 `app.getPath("userData")` |

---

## Step 2 — Preload에 contextBridge 추가

`src/preload/index.ts`에서 import:

```typescript
import "../../claude-chat-vts/preload/claude-chat-preload.js";
```

또는 preload 파일 자체를 `claude-chat-preload.ts`로 교체해도 됩니다.

> **주의:** `contextIsolation: true`가 설정된 BrowserWindow에서만 동작합니다.

---

## Step 3 — Renderer에서 위젯 생성

```typescript
import { ClaudeChat } from "../../claude-chat-vts/renderer/ClaudeChat.js";
import "../../claude-chat-vts/renderer/chat.css";

// HTML에 컨테이너 엘리먼트 필요:
// <div id="chat" style="height: 600px;"></div>

const chat = new ClaudeChat("#chat", {
  cwd: "/path/to/workspace",   // 세션 기본 작업 디렉터리 (선택)

  mentionItems: [
    { id: "ds1", name: "sales_data", type: "CSV" },
    { id: "ds2", name: "users_db",   type: "MariaDB" },
  ],

  placeholder: "질문을 입력하세요…",
  emptyHint:   "Claude에게 무엇이든 물어보세요",

  onSessionCreate(session) {
    console.log("새 세션:", session.id);
  },

  async onOptionSelect(sessionId, option, index) {
    // true 반환 → 호스트가 처리 (Claude 재호출 없음)
    // false/undefined → 기본 동작 (Claude에 옵션 텍스트 재전송)
    return false;
  },

  onMentionSelect(sessionId, item) {
    console.log("@" + item.name + " 선택");
  },
});

// 페이지 언마운트 시 정리
window.addEventListener("beforeunload", () => chat.destroy());
```

### CSS import 방식

번들러를 사용하는 경우 위처럼 import합니다. 직접 HTML에 포함하는 경우:

```html
<link rel="stylesheet" href="path/to/chat.css">
```

---

## ClaudeChatOptions 레퍼런스

| 옵션 | 타입 | 설명 |
|------|------|------|
| `cwd` | `string?` | 세션 디렉터리 기본 경로. 생략 시 OS 임시 디렉터리 사용 |
| `mentionItems` | `MentionItem[]?` | `@` 팝업에 표시할 항목 목록 |
| `onSessionCreate` | `(session) => void` | 세션 생성 직후 콜백 |
| `onOptionSelect` | `async (sessionId, option, index) => boolean` | 옵션 버튼 클릭 핸들러 |
| `onMentionSelect` | `(sessionId, item) => void` | @멘션 선택 콜백 |
| `placeholder` | `string?` | 입력창 플레이스홀더 |
| `emptyHint` | `string?` | 메시지 없을 때 중앙 힌트 텍스트 |

---

## 공개 API

```typescript
const chat = new ClaudeChat(container, options);

// 인스턴스 정리 — IPC 구독 해제 + DOM 이벤트 리스너 제거
chat.destroy();
```

`destroy()`는 위젯을 DOM에서 제거하지 않습니다. 컨테이너 엘리먼트를 제거하려면 별도로 처리하세요.

---

## IPC 채널

| 채널 | 방향 | 설명 |
|------|------|------|
| `claude-chat:probe` | renderer → main | Claude CLI 탐지 |
| `claude-chat:createSession` | renderer → main | 세션 생성 |
| `claude-chat:listSessions` | renderer → main | 세션 목록 |
| `claude-chat:sendMessage` | renderer → main | 메시지 전송 (스트리밍 시작) |
| `claude-chat:abort` | renderer → main | 진행 중인 스트림 중단 |
| `claude-chat:stream:{id}` | main → renderer | NDJSON 이벤트 push |
| `claude-chat:done:{id}` | main → renderer | 스트림 완료 신호 |
| `claude-chat:error:{id}` | main → renderer | 에러 신호 `{ message }` |

---

## CLAUDE.md 시스템 프롬프트 주입

세션 생성 후 `session.cwd` 안에 `CLAUDE.md`를 작성하면 Claude Code가 자동으로 시스템 프롬프트로 사용합니다:

```typescript
import fs from "node:fs/promises";
import path from "node:path";

chat.opt.onSessionCreate = async (session) => {
  await fs.writeFile(
    path.join(session.cwd, "CLAUDE.md"),
    "당신은 데이터 분석 전문가입니다. 항상 한국어로 응답하세요.",
    "utf-8"
  );
};
```

> `onSessionCreate`는 동기 콜백이므로, 비동기 작업은 `session.cwd`를 저장해두고 `sendMessage` 전에 처리하세요.

---

## CSS 커스터마이즈

`chat.css`의 CSS 변수를 앱 CSS에서 override합니다:

```css
:root {
  --cld-accent:       #4a90d9;  /* 포인트 색상 */
  --cld-accent-hover: #6aaae9;
  --cld-bullet-live:  #4a90d9;
  --cld-panel-bg:     #18181b;
  --cld-frame-bg:     #27272a;
}
```

---

## 멀티 인스턴스

같은 페이지에 여러 인스턴스를 생성할 수 있습니다. IPC 채널은 세션 ID로 분리됩니다.

```typescript
const chat1 = new ClaudeChat("#chat-1", { ... });
const chat2 = new ClaudeChat("#chat-2", { ... });

// 정리
chat1.destroy();
chat2.destroy();
```

단, `registerClaudeChatIpc(win, ...)` 호출은 하나의 `BrowserWindow` 당 **한 번만** 해야 합니다 (중복 IPC 핸들러 방지).
