# Claude Code 패널 → Agent PTY 터미널 대체 (E) 설계

- 작성일: 2026-06-03
- 소스 킷: `reusable/agent-pty-kit` (pty4j 로컬 PTY + @ServerEndpoint WS + xterm.js UI)
- 결정: **터미널 cwd = job 워크스페이스**(시작 시 job 생성→소스 적재→그 dir에서 claude/codex), **데이터 패널·job/DB 백엔드 유지**, Remote/SSHJ 미사용.

## 목표
기존 ChatPanel(스트리밍 IPC)·CodePanel(SQL 패널)을 제거하고, 서버에서 `claude`/`codex` CLI를 PTY로 실행해 xterm.js 터미널로 연결하는 패널로 대체. 에이전트는 워크스페이스에서 직접 파일/데이터(data.db, data_helpers.py)를 다룬다.

## 백엔드 (SSF, `com.ithows.aidclaude.agentpty`)
킷의 **로컬 전용** 클래스만 이식(Remote/SSHJ 제외):
- 이식: `AgentCommandCatalog`, `PathGuard`, `LocalAgentPtyOptions`, `LocalAgentPtyRegistry`, `LocalAgentPtySession`, `LocalAgentPtyRunner`, `LocalAgentPtyWebSocketEndpoint` (패키지명 변경).
- 신규(슬림): `AgentPtyRuntime`(local-only), `AgentPtyServlet`(start/kill 라우팅).
- 의존성: `org.jetbrains.pty4j:pty4j:0.13.6` 추가(gson은 기존 보유). sshj 불필요.
- 등록(신규 `AgentPtyInitListener` 또는 기존 init 확장):
  - `AgentPtyRuntime.init(localRunner)` — allowedCwdPrefixes = `[AcContext.home()]` (경로 가드).
  - WS: `ServerContainer sc = (ServerContainer) ctx.getAttribute(ServerContainer.class.getName()); sc.addEndpoint(LocalAgentPtyWebSocketEndpoint.class);` (fat-jar는 @ServerEndpoint 자동 스캔 안 되므로 **프로그래매틱 등록**).
- web.xml: `AgentPtyServlet`을 `/api/agent-pty/*`에 매핑. (CORS는 기존 CorsFilter `/*`가 커버. WS Origin 검사는 기본 허용.)

### 엔드포인트(킷 계약 유지)
- `POST /api/agent-pty/local` — form: `agent`(claude|codex), `workingDirectory` → `{sessionId, agent, command, workingDirectory}`
- `POST /api/agent-pty/local/{sessionId}/kill`
- `WS /ws/agent-pty/local/{sessionId}?cols&rows` — 바이너리 in/out, `{type:resize,cols,rows}` JSON

## 프론트엔드 (React)
- 의존성: `@xterm/xterm`, `@xterm/addon-fit`.
- preload `agent` 네임스페이스 추가(HTTP/WS는 SSF로):
  - `agent.startLocal({agent, workingDirectory})` → POST(form-urlencoded) `/api/agent-pty/local`, `{sessionId,...}`
  - `agent.killLocal(sessionId)`
  - `agent.wsUrl(sessionId, cols, rows)` → serverUrl(http)→ws 변환 + 경로 구성
- 신규 `TerminalPanel.tsx`:
  - 에이전트 선택(claude/codex), "터미널 시작" → `jobs.create("terminal", 선택 소스 ids)` → `agent.startLocal({agent, workingDirectory: job.workspaceDir})` → xterm open + WS 연결.
  - 입력 keystroke→WS 바이트, 출력 WS→term.write, fit/resize, "중지"→killLocal+cleanup.
  - 소스: 기본 전체 카탈로그 소스를 job에 적재(간단). (후속: 선택 UI)
- `ProjectWindow`: 우측 컬럼(ChatPanel+CodePanel) → `TerminalPanel`로 교체. claude:stream/done/error·job:analyze_code 이벤트 배선 제거.
- `appStore`: 스트리밍/activeAnalyzeCode/채팅 상태·액션 제거. centerTabs/sources/schemas/jobs 유지.

## 삭제 (사용자 승인)
- 프론트: `ChatPanel.tsx`, `CodePanel.tsx`, 관련 store 상태, ProjectWindow 이벤트 배선, exportUtils의 SQL 의존(있으면).
- main: `claude-bridge.ts`, `claude-service.ts`, `claude-detector.ts`, `ssf-client.ts`; `claude:probe/sendMessage/abort` IPC; `chokidar` 의존.
- preload: `claude` 네임스페이스, 이벤트 채널(claude:*, job:analyze_code) — job:update도 emitter 없음 → 제거.
- SSF: SQL 엔드포인트(runJobSql/runJobAnalysis/getSqlOptions)는 **유지**(범용·테스트됨, CenterPanel/후속 활용). 삭제는 프론트 연계부에 한정.

## 데이터 흐름(신규)
1. DataSourcePanel에서 소스 등록.
2. TerminalPanel "시작" → job 생성(소스 data.db 적재 + CLAUDE.md/data_helpers.py) → 그 cwd에서 claude/codex PTY.
3. 사용자가 터미널에서 에이전트와 직접 대화; 에이전트가 data_helpers/data.db로 분석·출력 생성.
4. CenterPanel에서 DB/결과 테이블 조회(수동 새로고침).

## 검증
- 백엔드: `mvn package` + 스모크 — `POST /api/agent-pty/local`(command=whoami, cwd=home 하위) → sessionId → WS 연결 → 바이트 수신 → kill. (PTY 배관 검증; claude/codex 실제 실행은 GUI에서)
- 프론트: typecheck + electron-vite build.
- 회귀: 기존 서버 스모크/통합.

## 리스크
- pty4j 네이티브가 shade fat-jar에서 추출되는지 → 스모크로 확인.
- @ServerEndpoint 프로그래매틱 등록 시점(SCI 이후 contextInitialized) → ServerContainer attribute 존재 확인.
- WS cross-origin(Electron origin↔SSF) → 기본 허용 + CSP ws://localhost:* (기존 반영).
