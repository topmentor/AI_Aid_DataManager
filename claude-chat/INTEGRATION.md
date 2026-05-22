# Claude Chat Module — 통합 가이드

AidClaude에서 추출한 Claude Code CLI 채팅 모듈입니다.  
Electron + React + TypeScript 프로젝트에 복사-붙여넣기로 적용합니다.

---

## 폴더 구조

```
claude-chat/
├── shared/
│   └── chat-types.ts          # 공유 타입 (MentionItem, ChatSession, ClaudeProbe, ClaudeChatApi)
├── main/
│   ├── claude-bridge.ts       # Claude CLI subprocess + NDJSON 스트림 iterator
│   ├── claude-detector.ts     # CLI 경로 탐지 + 인증 probe
│   ├── claude-chat-service.ts # 세션 생성·메시지 전송 (no SQLite/job 의존성)
│   └── register-ipc.ts        # registerClaudeChatIpc() — 원콜 IPC 등록
├── preload/
│   └── claude-chat-preload.ts # window.claudeChat contextBridge
└── renderer/
    ├── useChatStore.ts         # 독립 Zustand 스토어
    ├── ClaudeChat.tsx          # 채팅 UI 컴포넌트
    ├── chat.css                # 모든 채팅 CSS (.cld-*, .claude-bar*, .chat-*)
    └── claude-chat.d.ts        # window.claudeChat 타입 선언
```

---

## 의존 패키지

프로젝트의 `package.json`에 아래 패키지가 있어야 합니다.

```jsonc
// dependencies
"react": "^18",
"react-dom": "^18",
"react-markdown": "^9",
"remark-gfm": "^4",
"zustand": "^4"

// devDependencies
"@types/react": "^18",
"@types/react-dom": "^18",
"typescript": "^5"
```

Claude Code CLI는 호스트 시스템에 설치되어 있어야 합니다 (`claude login` 완료).

---

## Step 1 — 파일 복사

`claude-chat/` 폴더를 그대로 프로젝트에 복사합니다.  
경로는 자유롭게 정해도 되지만, 이 가이드에서는 아래를 기준으로 설명합니다:

```
my-app/
└── src/
    ├── claude-chat/      ← 복사한 폴더
    ├── main/
    ├── preload/
    └── renderer/
```

---

## Step 2 — Main 프로세스 등록

`src/main/index.ts` (또는 Electron main entry)에 한 줄 추가:

```typescript
import { app, BrowserWindow } from "electron";
import { registerClaudeChatIpc } from "../claude-chat/main/register-ipc.js";

function createWindow() {
  const win = new BrowserWindow({
    webPreferences: {
      preload: path.join(__dirname, "../preload/index.js"),
      contextIsolation: true,
    },
  });
  // ... window 설정 ...

  // ★ 이 한 줄로 IPC 핸들러 전체 등록
  registerClaudeChatIpc(win, {
    claudeBin: "claude",        // 생략 시 CLAUDE_BIN 환경변수 또는 "claude"
    probeDir: app.getPath("userData"), // probe용 임시 cwd
  });
}
```

---

## Step 3 — Preload 등록

`src/preload/index.ts`에 import를 추가합니다.

```typescript
// 기존 contextBridge 코드 아래에 추가
import "../claude-chat/preload/claude-chat-preload";
```

> preload 진입점이 하나인 경우 직접 실행됩니다.  
> 여러 preload 파일이 있다면 electron-vite의 `preload` 빌드 타겟에 엔트리로 추가하세요.

---

## Step 4 — Renderer에서 컴포넌트 사용

```tsx
// src/renderer/src/App.tsx
import { ClaudeChat } from "../../claude-chat/renderer/ClaudeChat";
import type { MentionItem } from "../../claude-chat/shared/chat-types";

// @-mention 목록 예시 (데이터 소스, 파일, 태그 등 무엇이든 가능)
const mentionItems: MentionItem[] = [
  { id: "ds-1", name: "sales_data", type: "CSV" },
  { id: "ds-2", name: "users_db",   type: "MariaDB" },
];

export default function App() {
  return (
    <div style={{ height: "100vh" }}>
      <ClaudeChat
        cwd="C:/my-project/workspace"   // Claude 작업 디렉터리 (생략 시 임시폴더)
        mentionItems={mentionItems}
        placeholder="분석 요청을 입력하세요…"
        emptyHint="Claude에게 질문해보세요"
        onSessionCreate={(session) => {
          console.log("새 세션:", session.id);
        }}
        onMentionSelect={(sessionId, item) => {
          console.log(`@${item.name} 선택됨`);
        }}
        onOptionSelect={async (sessionId, option, index) => {
          // true 반환: 호스트가 직접 처리 (Claude 재호출 건너뜀)
          // false/undefined 반환: 기본 동작 (Claude에게 옵션 텍스트 재전송)
          console.log("옵션 선택:", option, index);
          return false;
        }}
      />
    </div>
  );
}
```

---

## Step 5 — TypeScript 타입 포함

`src/renderer/src/env.d.ts` (또는 `tsconfig.web.json`의 include)에 추가:

```typescript
/// <reference path="../../claude-chat/renderer/claude-chat.d.ts" />
```

또는 `tsconfig.web.json`:
```json
{
  "include": [
    "src/renderer/**/*",
    "src/claude-chat/renderer/claude-chat.d.ts"
  ]
}
```

---

## Props 레퍼런스

| Prop | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `cwd` | `string` | 임시 폴더 자동 생성 | Claude 작업 디렉터리. `CLAUDE.md`가 있으면 자동 로드됨 |
| `mentionItems` | `MentionItem[]` | `[]` | `@` 팝업에 표시할 항목 |
| `onSessionCreate` | `(s: ChatSession) => void` | — | 새 세션 생성 시 콜백 |
| `onOptionSelect` | `async (sessionId, option, index) => boolean` | — | `<options>` 버튼 클릭 시. `true` 반환하면 기본 재전송 생략 |
| `onMentionSelect` | `(sessionId, item: MentionItem) => void` | — | @멘션 선택 시 콜백 |
| `placeholder` | `string` | 한국어 기본값 | 입력창 플레이스홀더 |
| `emptyHint` | `string` | 한국어 기본값 | 메시지 없을 때 힌트 텍스트 |

---

## IPC 채널 목록

| 채널 | 방향 | 설명 |
|------|------|------|
| `claude-chat:probe` | invoke | Claude CLI 탐지 + 인증 확인 |
| `claude-chat:createSession` | invoke | 새 세션 생성 |
| `claude-chat:listSessions` | invoke | 세션 목록 조회 |
| `claude-chat:sendMessage` | invoke | 메시지 전송 (비동기, 스트림은 push로) |
| `claude-chat:abort` | invoke | 진행 중인 스트림 중단 |
| `claude-chat:stream:{sessionId}` | push (main→renderer) | NDJSON 이벤트 스트림 |
| `claude-chat:done:{sessionId}` | push (main→renderer) | 스트림 완료 |
| `claude-chat:error:{sessionId}` | push (main→renderer) | 스트림 오류 |

---

## CLAUDE.md로 시스템 프롬프트 주입

`cwd` 디렉터리에 `CLAUDE.md` 파일을 두면 Claude Code가 자동으로 읽어 시스템 프롬프트에 포함합니다.

```
my-workspace/
├── CLAUDE.md        ← "당신은 데이터 분석 전문가입니다. ..."
└── (Claude가 생성하는 파일들)
```

세션 생성 시 `cwd`를 통해 작업 폴더를 지정하거나, `onSessionCreate` 콜백에서 받은 `session.cwd`에 `CLAUDE.md`를 동적으로 생성해 주입할 수도 있습니다.

---

## 스타일 커스터마이즈

`chat.css`의 CSS 변수를 override하여 색상을 변경합니다:

```css
/* 앱의 global.css 또는 컴포넌트 상위에서 */
:root {
  --cld-accent: #7c5cbf;       /* 보내기 버튼 / 강조색 */
  --cld-accent-hover: #9370d8;
  --cld-bullet-live: #7c5cbf;
  --cld-bullet-done: #56c279;
  --cld-panel-bg: #18181b;
  --cld-frame-bg: #27272a;
  --cld-frame-border: #3f3f46;
}
```

---

## 독립 Zustand 스토어

`useChatStore`는 `window.aidclaude`와 완전히 분리된 독립 스토어입니다.  
호스트 앱의 기존 상태 관리와 충돌하지 않습니다.

호스트 앱에서 채팅 상태를 읽어야 하는 경우:

```typescript
import { useChatStore } from "./claude-chat/renderer/useChatStore";

function MyComponent() {
  const { activeSessionId, sessions } = useChatStore();
  // ...
}
```

---

## 멀티 인스턴스

`useChatStore`는 싱글턴입니다. 한 화면에 `<ClaudeChat />` 인스턴스가 여러 개 있을 경우 모두 같은 스토어를 공유합니다. 완전히 독립된 인스턴스가 필요하다면 Zustand의 `createStore` + `useStore`를 사용해 인스턴스별 스토어를 만드세요.
