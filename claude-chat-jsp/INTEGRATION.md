# Claude Chat Module — JSP/Servlet 통합 가이드

Electron 버전(`claude-chat/`)과 동일한 UI를 Java Servlet 기반 프로젝트에 붙여넣기로 적용합니다.  
외부 Java 라이브러리 의존성 없음 (순수 JDK + Servlet API).

---

## 폴더 구조

```
claude-chat-jsp/
├── java/claudechat/
│   ├── ClaudeSession.java          # 세션 데이터 클래스 (JSON 직렬화 내장)
│   ├── ClaudeSessionManager.java   # 싱글턴 — 세션 CRUD + subprocess 관리
│   └── ClaudeApiServlet.java       # 모든 REST + SSE 라우팅 서블릿
├── web/
│   ├── claude-chat.js              # 바닐라 JS 채팅 위젯 (React/번들러 불필요)
│   └── claude-chat.css             # 채팅 CSS (.cld-*, .claude-bar*, .chat-*)
├── WEB-INF-fragment.xml            # web.xml에 추가할 Servlet 매핑 조각
└── demo.jsp                        # 데모 JSP 페이지
```

---

## 요구 사항

| 항목 | 최소 버전 |
|------|----------|
| Java | 8+ |
| Servlet API | 3.0+ (Tomcat 7+, JBoss 6+) |
| Claude Code CLI | 최신 (시스템에 설치 + `claude login` 완료) |
| 브라우저 | Chrome 66+ / Firefox 57+ / Edge 79+ (fetch + ReadableStream 지원) |

---

## Step 1 — Java 소스 복사

`java/claudechat/` 폴더를 프로젝트의 Java 소스 경로에 복사합니다.

```
src/main/java/claudechat/          ← Maven 기준
  ├── ClaudeSession.java
  ├── ClaudeSessionManager.java
  └── ClaudeApiServlet.java
```

또는 기존 패키지에 맞게 package 선언을 수정하고 import를 갱신합니다.

---

## Step 2 — Servlet 등록

### 방법 A: @WebServlet 어노테이션 (Servlet 3.0+, 권장)

`ClaudeApiServlet.java`에 이미 `@WebServlet` 어노테이션이 있습니다.  
`web.xml`에서 어노테이션 스캔이 활성화되어 있으면 추가 설정 불필요.

### 방법 B: web.xml 직접 등록

`WEB-INF-fragment.xml`의 내용을 `WEB-INF/web.xml` 의 `<web-app>` 안에 추가합니다:

```xml
<servlet>
    <servlet-name>ClaudeApiServlet</servlet-name>
    <servlet-class>claudechat.ClaudeApiServlet</servlet-class>
    <!-- 선택적 init-param (아래 '설정' 섹션 참조) -->
</servlet>
<servlet-mapping>
    <servlet-name>ClaudeApiServlet</servlet-name>
    <url-pattern>/api/claude-chat/*</url-pattern>
</servlet-mapping>
```

---

## Step 3 — 정적 파일 배포

`web/` 폴더 내 파일을 웹 루트(webapp)에 복사합니다:

```
webapp/
└── claude-chat/
    ├── claude-chat.js
    └── claude-chat.css
```

JSP 페이지에서 참조:
```html
<link rel="stylesheet" href="${pageContext.request.contextPath}/claude-chat/claude-chat.css">
<script src="${pageContext.request.contextPath}/claude-chat/claude-chat.js"></script>
```

---

## Step 4 — JSP 페이지에 위젯 삽입

```jsp
<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/claude-chat/claude-chat.css">
  <style>
    #my-chat { height: 600px; border: 1px solid #333; }
  </style>
</head>
<body>

  <div id="my-chat"></div>

  <script src="${pageContext.request.contextPath}/claude-chat/claude-chat.js"></script>
  <script>
    var chat = new ClaudeChat('#my-chat', {
      apiBase: '${pageContext.request.contextPath}/api/claude-chat',

      // @-mention 아이템 (선택적)
      mentionItems: [
        { id: 'ds1', name: 'sales_data', type: 'CSV' },
        { id: 'ds2', name: 'users_db',   type: 'DB'  },
      ],

      placeholder: '질문을 입력하세요…',
      emptyHint:   'Claude에게 무엇이든 물어보세요',

      onSessionCreate: function(session) {
        console.log('새 세션:', session.id);
      },

      onOptionSelect: function(sessionId, option, index) {
        // true 반환 → 호스트가 처리 (Claude 재호출 없음)
        // false/undefined 반환 → 기본 동작 (Claude에 옵션 텍스트 재전송)
        return Promise.resolve(false);
      },
    });
  </script>

</body>
</html>
```

---

## 설정 (Servlet init-param)

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| `claudeBin` | Claude CLI 실행 파일 경로 | `$CLAUDE_BIN` 환경변수 또는 `"claude"` |
| `sessionCwd` | 세션 작업 디렉터리 루트 | `java.io.tmpdir/claude-sessions/{sessionId}` |

### sessionCwd 설정 (권장)

`sessionCwd`를 **명시적으로 설정**하는 것을 강력히 권장합니다.  
기본값인 `java.io.tmpdir`은 OS가 임의로 정리할 수 있고, 서버 환경에 따라 쓰기 권한이 없을 수 있습니다.

**세션 디렉터리 결정 우선순위:**

```
1. 클라이언트 POST body의 cwd 필드 (현재 JS 위젯은 전달하지 않음)
2. sessionCwd init-param 값  ← 여기를 설정하면 전체 적용
3. java.io.tmpdir/claude-sessions/{sessionId}  ← 기본값 (비권장)
```

`sessionCwd`를 설정하면 세션별로 다음 경로가 자동 생성됩니다:
```
{sessionCwd}/claude-sessions/{sessionId}/
├── request.md      ← 사용자 메시지
├── CLAUDE.md       ← 시스템 프롬프트 (선택)
└── (Claude가 생성하는 파일들)
```

**환경별 권장 경로:**

| 환경 | 권장 경로 예시 |
|------|--------------|
| Windows (개발) | `C:/myapp/claude-workspace` |
| Windows (운영, Tomcat) | `C:/tomcat/work/claude-workspace` |
| Linux (운영) | `/var/lib/myapp/claude-workspace` |
| Linux (Tomcat) | `/opt/tomcat/work/claude-workspace` |

**설정 예시 (web.xml):**

```xml
<servlet>
    <servlet-name>ClaudeApiServlet</servlet-name>
    <servlet-class>claudechat.ClaudeApiServlet</servlet-class>

    <!-- ★ 앱 서버가 쓰기 권한을 가진 절대 경로로 설정 -->
    <init-param>
        <param-name>sessionCwd</param-name>
        <param-value>/var/lib/myapp/claude-workspace</param-value>
    </init-param>

    <!-- Claude CLI 경로 (PATH에 없는 경우) -->
    <init-param>
        <param-name>claudeBin</param-name>
        <param-value>/usr/local/bin/claude</param-value>
    </init-param>
</servlet>
```

**쓰기 권한 확인 (Linux):**
```bash
# Tomcat 실행 계정(예: tomcat)이 쓸 수 있도록 설정
mkdir -p /var/lib/myapp/claude-workspace
chown tomcat:tomcat /var/lib/myapp/claude-workspace
chmod 750 /var/lib/myapp/claude-workspace
```

**쓰기 권한 확인 (Windows, 관리자 PowerShell):**
```powershell
# IIS 또는 Tomcat 서비스 계정에 쓰기 권한 부여
$path = "C:\myapp\claude-workspace"
New-Item -ItemType Directory -Force -Path $path
icacls $path /grant "IIS_IUSRS:(OI)(CI)F"  # IIS 서비스 계정 예시
```

Windows에서 Claude CLI가 npm으로 설치된 경우 `claudeBin` 설정:
```xml
<init-param>
    <param-name>claudeBin</param-name>
    <param-value>C:/Users/you/AppData/Roaming/npm/claude.cmd</param-value>
</init-param>
```

---

## API 엔드포인트

모든 경로는 `/api/claude-chat/` 기준입니다.

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/probe` | Claude CLI 탐지 및 버전 확인 |
| `GET` | `/sessions` | 세션 목록 조회 |
| `POST` | `/sessions` | 새 세션 생성 `{ cwd?, label? }` |
| `POST` | `/sessions/{id}/send` | 메시지 전송, **SSE 스트림** 응답 `{ message }` |
| `POST` | `/sessions/{id}/abort` | 진행 중인 Claude 프로세스 중단 |

### SSE 이벤트 형식 (`/send` 응답)

응답은 `text/event-stream`이며, 각 줄은 `data: {JSON}\n\n` 형식입니다.  
Claude CLI의 NDJSON을 그대로 relay합니다.

```
data: {"type":"assistant","message":{"content":[{"type":"text","text":"안녕하세요"}]}}

data: {"type":"tool_use","id":"tu_1","name":"Read","input":{"file_path":"request.md"}}

data: {"type":"tool_result","tool_use_id":"tu_1"}

data: {"type":"result","session_id":"abc123"}

data: {"type":"done"}
```

---

## request.md 패턴

사용자 메시지는 세션 `cwd/request.md`에 UTF-8로 저장한 뒤 Claude에 전달됩니다.  
Windows cmd.exe의 한국어 인코딩 문제를 우회하는 패턴입니다.

세션 `cwd`에 `CLAUDE.md`를 두면 Claude Code가 자동으로 시스템 프롬프트에 포함합니다:

```java
// 세션 생성 후 CLAUDE.md 주입 예시
ClaudeSession session = ClaudeSessionManager.getInstance().createSession(cwd, label);
Files.write(Paths.get(session.cwd, "CLAUDE.md"),
    "당신은 데이터 분석 전문가입니다. 항상 한국어로 응답하세요.".getBytes(StandardCharsets.UTF_8));
```

---

## JavaScript API 레퍼런스

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `apiBase` | `string` | `'/api/claude-chat'` | Servlet URL 기본 경로 |
| `mentionItems` | `{id, name, type}[]` | `[]` | `@` 팝업에 표시할 항목 |
| `onSessionCreate` | `function(session)` | — | 세션 생성 시 콜백 |
| `onOptionSelect` | `async function(sessionId, option, index) → boolean` | — | 옵션 버튼 클릭 핸들러 |
| `onMentionSelect` | `function(sessionId, item)` | — | @멘션 선택 콜백 |
| `placeholder` | `string` | 한국어 기본값 | 입력창 플레이스홀더 |
| `emptyHint` | `string` | 한국어 기본값 | 메시지 없을 때 힌트 |

---

## CSS 커스터마이즈

`claude-chat.css`의 CSS 변수를 override합니다:

```css
/* 앱 CSS에서 */
:root {
  --cld-accent:       #4a90d9; /* 포인트 색상 */
  --cld-accent-hover: #6aaae9;
  --cld-bullet-live:  #4a90d9;
  --cld-panel-bg:     #18181b;
  --cld-frame-bg:     #27272a;
}
```

---

## Electron 버전과의 차이

| 항목 | Electron 버전 | JSP 버전 |
|------|--------------|---------|
| 백엔드 | Node.js + Electron IPC | Java Servlet |
| 프론트엔드 | React + Zustand | 바닐라 JS 클래스 |
| 스트리밍 | Electron `webContents.send` | HTTP SSE (`text/event-stream`) |
| 세션 저장 | 메모리 (Map) | 메모리 (ConcurrentHashMap) |
| 마크다운 | react-markdown + remark-gfm | 내장 미니 렌더러 |
| 의존성 | npm 패키지 다수 | 없음 (JDK + Servlet API만) |

---

## 주의 사항

- **스레드**: `/send` 엔드포인트는 Claude CLI가 응답을 완료할 때까지 서블릿 스레드를 블로킹합니다. 동시 사용자가 많은 경우 Tomcat NIO connector + Async Servlet 전환을 검토하세요.
- **프로세스 관리**: `ClaudeSessionManager`는 싱글턴으로 서버 재시작 전까지 세션이 유지됩니다. 세션 정리 로직이 필요하면 `destroySession(id)` 메서드를 추가하세요.
- **보안**: `sendMessage`는 임의의 문자열을 `request.md`에 쓰고 Claude에 전달합니다. 민감한 환경에서는 메시지 검증/제한 로직을 추가하세요.
- **Windows 경로**: `claudeBin`에 공백이 포함된 경우 따옴표로 감싸거나 단축 경로를 사용하세요.
