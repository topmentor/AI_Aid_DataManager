# AidClaude 개발 히스토리

> Claude Code CLI가 내장된 Electron 데스크탑 데이터 분석·시각화 도구

---

## 프로젝트 개요

- **목적**: MariaDB / CSV / JSON / JSONL 데이터 소스에 대해 Claude가 Python 분석 코드를 생성·실행하고 결과를 테이블·차트·파일로 보여주는 Electron 앱
- **핵심 설계 원칙**
  - DB 자격증명을 Claude에게 노출하지 않음 → `data_helpers.py` 주입 방식
  - Claude가 생성한 Python 코드는 AST 검증 후 로컬 실행
  - Claude Code CLI를 외부 프로세스(subprocess)로 실행 (VSCode 패턴)

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| 앱 프레임워크 | Electron 35, electron-vite 2.3.0 |
| UI | React 18, TypeScript 5 |
| 상태 관리 | Zustand |
| 코드 에디터 | Monaco Editor |
| 마크다운 렌더링 | react-markdown + remark-gfm |
| DB 연결 | mysql2/promise |
| CSV 파싱 | PapaParse |
| 파일 감시 | chokidar |
| 차트 | chart.js + react-chartjs-2 |

---

## 작업 이력

### Phase 1 — 프로젝트 스캐폴드

**생성 파일**
- `app/package.json` — 의존성 정의
- `app/electron.vite.config.ts` — main / preload / renderer 빌드 설정
- `app/tsconfig.json` / `tsconfig.node.json` / `tsconfig.web.json` — 3분할 tsconfig
- `app/src/renderer/index.html` — CSP 메타태그 포함

---

### Phase 2 — 공유 타입 + IPC 아키텍처

**생성 파일**
- `app/src/shared/types.ts` — 전체 IPC 타입 정의
- `app/src/preload/index.ts` — contextBridge `window.aidclaude` 노출
- `app/src/main/index.ts` — BrowserWindow + IPC 라우팅

**주요 타입**
```typescript
// 데이터 소스 (판별 유니온)
export type DataSource =
  | (DataSourceBase & { type: "mariadb"; config: MariaDbConfig })
  | (DataSourceBase & { type: "csv";     config: CsvConfig })
  | (DataSourceBase & { type: "json";    config: JsonConfig })
  | (DataSourceBase & { type: "jsonl";   config: JsonlConfig });

// 스트리밍 이벤트
export type ClaudeStreamEvent =
  | { type: "assistant"; text: string }
  | { type: "result"; sessionId: string; subtype: string; resultText: string }
  | { type: "error"; message: string };
```

**IPC 채널 보안**
- preload에 `ALLOWED_PUSH_CHANNELS` 허용 목록 적용
- `wrapperRegistry` Map으로 메모리 누수 없는 `on`/`off` 구현

---

### Phase 3 — Claude Code CLI Bridge

**생성 파일**
- `app/src/main/services/claude-bridge.ts` — subprocess + NDJSON async iterator
- `app/src/main/services/claude-detector.ts` — CLI 탐지 + round-trip probe

**핵심 구현**
```typescript
export async function* queryClaude(opts: ClaudeQueryOpts): AsyncGenerator<unknown> {
  // claude -p --output-format stream-json --verbose ...
  // readline으로 NDJSON 파싱, AbortController로 중단 지원
}
```

**중요 결정: `--append-system-prompt` 미사용**
- Windows cmd.exe에서 한국어·특수문자 포함 긴 인수 전달 시 args가 truncation되는 문제 발생
- 해결: 시스템 프롬프트를 `CLAUDE.md`로 작업 디렉터리에 기록 → Claude Code가 자동 로드

---

### Phase 4 — Settings + Catalog Service

**생성 파일**
- `app/src/main/services/settings-service.ts` — `userData/settings.json` 영속화
- `app/src/main/services/catalog-service.ts` — `userData/catalog.json` CRUD

**IPC 핸들러**
```
settings:get / settings:set
catalog:list / catalog:add / catalog:update / catalog:remove
```

---

### Phase 5 — Schema Inspector

**생성 파일**
- `app/src/main/services/schema-inspector.ts`

**지원 소스별 구현**
| 소스 | 방법 |
|------|------|
| MariaDB | `information_schema.COLUMNS` 조회 |
| CSV | PapaParse preview:5 파싱 |
| JSON | rootPath 탐색 후 첫 항목 구조 추출 |
| JSONL | 첫 5줄 파싱 |

---

### Phase 6 — Python AST Validator + Runner

**생성 파일**
- `app/src/main/services/ast-validator.ts` — Python subprocess로 AST 검증
- `app/src/main/services/python-runner.ts` — `data_helpers.py` 생성 + 실행

**AST 검증 차단 목록**
- `os`, `subprocess`, `socket`, `requests`, `urllib`, `pymysql`, `sqlalchemy` 등 네트워크·시스템 라이브러리
- `eval()`, `exec()`, `__import__()` 호출
- 절대 경로를 사용하는 `open()` 호출

**`data_helpers.py` 주입 패턴**
```python
# 자동 생성 — Claude는 함수 시그니처만 보고 자격증명은 접근 불가
def load_csv_소스명() -> pd.DataFrame:
    return pd.read_csv("/copied/path/to/file.csv")

def load_mariadb_소스명(sql: str) -> pd.DataFrame:
    conn = pymysql.connect(host="...", password="<실제비밀번호>", ...)
    return pd.read_sql(sql, conn)
```

---

### Phase 7 — Job Service + System Prompt Builder

**생성 파일**
- `app/src/main/services/job-service.ts` — 작업 공간(workspace) 관리
- `app/src/main/services/system-prompt.ts` — 스키마 기반 시스템 프롬프트 빌드

**작업 공간 구조**
```
workspaceRoot/
└── job_<uuid>/
    ├── CLAUDE.md         ← 시스템 프롬프트 (Claude Code 자동 로드)
    ├── context.md        ← 사람이 읽는 참조용
    ├── data_helpers.py   ← 자격증명 주입
    ├── analyze.py        ← Claude가 생성
    └── output/
        ├── result.csv
        ├── chart.png
        └── report.html
```

---

### Phase 8 — Claude Service (턴 오케스트레이션)

**생성 파일**
- `app/src/main/services/claude-service.ts`

**턴 흐름**
1. `queryClaude()` async iterator로 스트리밍 수신
2. `claude:stream` IPC로 renderer에 실시간 push
3. 턴 완료 시 `analyze.py` 존재 확인 → AST 검증 → Python 실행
4. 결과를 `job:update`, `job:analyze_code` IPC로 push
5. `claude:done` / `claude:error`로 완료 신호

---

### Phase 9 — Renderer 기반 (Store + StartScreen)

**생성 파일**
- `app/src/renderer/src/store/appStore.ts` — Zustand 전역 상태
- `app/src/renderer/src/components/StartScreen.tsx`
- `app/src/renderer/src/App.tsx`
- `app/src/renderer/src/main.tsx`
- `app/src/renderer/src/styles/global.css`

**Store 핵심 상태**
```typescript
sources: DataSource[]
schemas: Map<string, DataSourceSchema>
jobs: Job[]
activeJobId: string | null
chatMessages: Map<string, ChatMessage[]>   // 잡별 채팅 내역
activeAnalyzeCode: string                  // Monaco에 표시할 코드
sourcePreview: { title, headers, rows } | null  // 소스 데이터 미리보기
streaming: { jobId, assistantMessageId } | null // 스트리밍 진행 상태
```

---

### Phase 10 — DataSourcePanel

**생성 파일**
- `app/src/renderer/src/components/DataSourcePanel.tsx`

**기능**
- MariaDB / CSV / JSON / JSONL 4가지 소스 타입 지원
- 파일 소스: Electron 네이티브 파일 선택 다이얼로그 → `workspaceRoot/data/`로 복사 (원본 이동 시에도 접근 가능)
- 연결 테스트 (`catalog:testConnection`)
- 스키마 조회 토글
- **미리보기 버튼**: 소스 데이터 최대 50행을 오른쪽 패널 "소스" 탭에 표시

---

### Phase 11 — ChatPanel (VibeHTML 스트리밍 패턴 포팅)

**생성 파일**
- `app/src/renderer/src/components/ChatPanel.tsx`

**VibeHTML 패턴 적용**
- **Fire-and-forget IPC**: `sendMessage()` 후 반환값 무시, 완료는 `claude:done`/`claude:error` 이벤트로 처리
- **streaming 상태 객체**: `{ jobId, assistantMessageId }` — ProjectWindow에서 이벤트 라우팅
- **스트리밍 불릿 표시**:
  - 🟠 주황 펄스 — 응답 생성 중 (`cld-bullet-live`)
  - 🟢 초록 — 완료 (`cld-bullet-done`)
  - 🔴 빨강 — 오류 (`cld-bullet-error`)
- **도구 호출 표시**: Read/Write/Edit 작업 파일명과 상태 실시간 표시
- **마크다운 렌더링**: `ReactMarkdown` + `remark-gfm`, 링크는 `target="_blank"`
- 작업 이력 탭 (최근 10개)

---

### Phase 12 — CodePanel + ProjectWindow

**생성 파일**
- `app/src/renderer/src/components/CodePanel.tsx` — Monaco Editor, 읽기 전용
- `app/src/renderer/src/components/ProjectWindow.tsx` — IPC 이벤트 수신 허브

**ProjectWindow IPC 이벤트 처리**
```typescript
claude:stream    → appendAssistantText / upsertToolCall
claude:done      → markAssistantDone
claude:error     → markAssistantError
job:update       → updateJob
job:analyze_code → setActiveCode (CodePanel 갱신)
```

---

### Phase 13 — ResultPanel + 소스 미리보기

**생성 파일**
- `app/src/renderer/src/components/ResultPanel.tsx`

**탭 구성**
| 탭 | 내용 |
|----|------|
| 파일 | 분석 결과 파일 목록 + 열기 |
| 표 | CSV 출력 파일 → 테이블 그리드 |
| 차트 | PNG 출력 파일 (base64 로드) |
| 소스 | DataSourcePanel "미리보기"로 불러온 원본 데이터 |

**TableView 그리드 특징**
- `position: sticky` 헤더 (수직 스크롤 시 고정)
- 행 번호 컬럼
- 교대 행 배경색
- `text-overflow: ellipsis` 긴 셀 내용 처리
- 내부 독립 스크롤 (`max-height: calc(100vh - 160px)`)

**PNG 로딩 방식**
- 개발 모드에서 renderer가 `http://localhost`이므로 `file://` URL 차단됨
- 해결: `files:readBase64` IPC → `data:image/png;base64,...` URL로 변환

---

### Phase 14 — 소스 데이터 미리보기 기능

**변경 파일**
- `schema-inspector.ts` — `previewData()` 함수 추가
- `main/index.ts` — `catalog:previewData` IPC 핸들러
- `preload/index.ts` — `catalog.previewData` 노출
- `appStore.ts` — `sourcePreview` 상태 + `setSourcePreview` 액션
- `DataSourcePanel.tsx` — "미리보기" 버튼
- `ResultPanel.tsx` — "소스" 탭

**소스별 미리보기 구현**
| 소스 | 방법 |
|------|------|
| CSV | PapaParse preview:50 |
| JSON (배열) | 첫 50개 항목 → 헤더/행 |
| JSON (객체) | 최상위 키/값 테이블 |
| JSONL | 첫 50줄 파싱 |
| MariaDB | 첫 번째 테이블 `SELECT * LIMIT 50` |

---

### Phase 16 — Claude 접속 확인 UI 이동

**변경 파일**
- `StartScreen.tsx` — probe 로직 완전 제거, "시작" 버튼 항상 활성화
- `ChatPanel.tsx` — `ClaudeConnectBar` 컴포넌트 추가 (채팅 창 상단)

**Before / After**
```
[이전] 시작 화면에서 probe → 성공해야 "시작" 버튼 활성화
[이후] 시작 화면 → 즉시 입장 가능
       채팅 창 상단 바 → "연결 확인" 버튼 온디맨드 probe
```

**ClaudeConnectBar 상태 흐름**
```
idle → (버튼 클릭) → checking → ok(초록) / error(빨강)
```

---

### Phase 17 — SQLite 중간 데이터 레이어

**설계 변경 배경**
- 기존 `data_helpers.py`에 DB 자격증명이 평문으로 삽입되는 구조
- MariaDB 비밀번호 등이 Claude가 읽을 수 있는 파이썬 파일에 노출되는 문제
- 여러 소스 간 SQL JOIN이 불가능한 구조적 한계

**새로운 데이터 흐름**
```
[소스 파일 / MariaDB]
       ↓  job 생성 시점에 Node.js가 일괄 로드
   data.db (SQLite, job 디렉터리 내)
       ↓  analyze.py에서 접근
   data_helpers.py (자격증명 없는 sqlite3 래퍼)
       ↓  분석 결과
   working 테이블 (data.db 내 신규 테이블)
```

**의존성 추가** (`package.json`)
```json
"better-sqlite3": "^11.0.0"
"@types/better-sqlite3": "^7.6.8"
"@electron/rebuild": "^3.7.1"
"postinstall": "electron-rebuild -f -w better-sqlite3"
```

**신규 파일**

`app/src/main/services/sqlite-loader.ts`
- `toTableName(sourceName)` — 소스명을 SQL 안전 테이블명으로 변환
- `loadSourceToDb(db, ds)` — 소스 유형별 분기하여 `data.db`에 로드
  - CSV/JSONL/JSON → `createAndInsert()` 트랜잭션 일괄 삽입
  - MariaDB → 해당 DB의 모든 테이블을 `<소스명>_<테이블명>` 형태로 복사
- `createAndInsert()` — `better-sqlite3` `db.transaction()` 활용, 빠른 벌크 삽입

**대폭 수정 파일**

`system-prompt.ts` — 스키마 기반에서 SQLite 테이블 기반으로 전면 재작성
```typescript
// 이전: DataSourceSchema[] (MariaDB 컬럼 메타데이터 포함)
// 이후: SourceTableMap[] (tableName + rowCount만)
export function buildSystemPrompt(sourceMaps: SourceTableMap[]): string
```
- `CLAUDE.md`에 테이블 목록, `data_helpers.py` API 사용법, 출력 규칙 기술

`python-runner.ts` — `buildDataHelpers()` 전면 교체
```python
# 이전: 소스별 load_csv_소스명(), load_mariadb_소스명() 함수 (자격증명 포함)
# 이후: 공통 SQLite 래퍼 (자격증명 없음)
def load(table: str) -> pd.DataFrame:   # 테이블 → DataFrame
def save(df, table, if_exists="replace"):# DataFrame → 워킹 테이블 저장
def query(sql: str) -> pd.DataFrame:    # 직접 SQL 실행 (JOIN 등)
def tables() -> list:                   # 현재 DB 테이블 목록
```

`job-service.ts` — job 생성 로직 전면 재작성
- `inspectSchema()` 호출 제거 (자격증명 접근 불필요)
- `Database` 인스턴스 생성 → `loadSourceToDb()` 반복 호출 → `db.close()`
- `CLAUDE.md` 기록 (시스템 프롬프트)
- `data_helpers.py` 기록 (SQLite 래퍼)
- 반환 타입: `{ job, systemPrompt }` → `{ job }`

**작업 공간 구조 변경**
```
job_<uuid>/
├── CLAUDE.md         ← 테이블 목록 + data_helpers API (시스템 프롬프트)
├── data.db           ← 모든 소스 데이터 + 워킹 테이블 (신규)
├── data_helpers.py   ← 자격증명 없는 sqlite3 래퍼 (신규 패턴)
├── analyze.py        ← Claude가 생성
└── output/
    ├── result.csv
    ├── chart.png
    └── report.html
```

**ResultPanel "DB" 탭 — SQLite 테이블 브라우저**
- 탭 이름 "소스" → "DB"로 변경
- job 활성 시: `data.db` 테이블 목록 표시 (소스 테이블 + 워킹 테이블)
  - 테이블 클릭 → 최대 200행 미리보기 (`TableView` 재사용)
  - "새로고침" 버튼 / job 완료 시 자동 갱신 (워킹 테이블 포착)
- job 없을 때: DataSourcePanel "미리보기" 결과 표시 (기존 동작 유지)

**신규 IPC 채널**

| 채널 | 방향 | 설명 |
|------|------|------|
| `db:listTables(jobId)` | invoke | job의 `data.db` 테이블 목록 반환 |
| `db:previewTable(jobId, tableName, limit?)` | invoke | 테이블 데이터 최대 N행 반환 |

---

### Phase 15 — 레이아웃 개편 + 시작 화면 개선

**변경 내용**

**레이아웃 변경** (`ProjectWindow.tsx`)
```
[이전] 왼쪽: DataSource | 가운데: Chat+Code | 오른쪽: Result
[이후] 왼쪽: DataSource | 가운데: Result(1fr) | 오른쪽: Chat+Code(380px)
```

**시작 화면 개선** (`StartScreen.tsx`)
- Claude probe 결과 무관하게 즉시 시작 가능 (버튼 항상 활성화)
- Probe 상태는 정보 표시용으로만 사용
- Claude 미연결 시 안내 메시지: "데이터 조회·미리보기는 Claude 없이도 사용 가능합니다"

---

### 버그 수정 이력

| 증상 | 원인 | 해결책 |
|------|------|--------|
| Claude가 "are"만 수신 | Windows cmd.exe의 `--append-system-prompt` 긴 인수 truncation | `CLAUDE.md`를 작업 디렉터리에 기록, Claude Code가 자동 로드 |
| `Autofill.enable failed` 콘솔 오류 | Electron DevTools Protocol 노이즈 | `app.commandLine.appendSwitch("disable-features", "AutofillServerCommunication")` |
| 개발 모드에서 PNG 표시 안 됨 | renderer(`http://localhost`)에서 `file://` URL 차단 | `files:readBase64` IPC로 base64 변환 후 `data:` URL 사용 |
| `job:update` 핸들러가 다른 잡 갱신 | activeJobId 비교 없음 | `if (updated.id === activeJobId)` 가드 추가 |
| 1KB 미만 파일 "0KB" 표시 | 단순 `Math.round(bytes/1024)` | `fmtSize()` 헬퍼: 1024 미만은 `${b} B` 표시 |
| 소스 전환 시 탭 상태 잔류 | `activeJobId` 변경 시 초기화 없음 | `useEffect` 의존성에 `activeJobId` 추가, 탭/데이터 리셋 |

---

### CSS 아키텍처

모든 컴포넌트 스타일은 `global.css`의 네임스페이스 프리픽스 클래스로 관리:

| 프리픽스 | 컴포넌트 |
|---------|---------|
| `.cld-*` | ChatPanel (VibeHTML 포팅) |
| `.chat-*` | ChatPanel 탭/탭버튼 |
| `.claude-bar*` | ChatPanel Claude 연결 상태 바 |
| `.rp-*` | ResultPanel (파일·표·차트·DB 탭) |
| `.rp-db-*` | ResultPanel SQLite DB 브라우저 |
| `.tv-*` | TableView 데이터 그리드 |
| `.ss-*` | StartScreen |
| `.pw-*` | ProjectWindow 레이아웃 |

---

---

### Phase 18 — CenterPanel (VSCode 스타일 탭) 도입

**배경**: ResultPanel은 파일·표·차트 탭 구조로 파일 기반 결과만 표시했으나, SQLite 테이블 직접 브라우징과 데이터 소스 미리보기를 함께 수용하기 어려워 전면 교체.

**신규 파일**
- `app/src/renderer/src/components/CenterPanel.tsx`

**삭제(미사용화)**
- `ResultPanel.tsx` — 코드는 유지되나 ProjectWindow에서 제거, CenterPanel로 완전 대체

**CenterPanel 구조**
```
┌─────────────────────────────────────────────┐
│ [탭1 ×] [탭2 ×] [탭3 ×]                   │  ← cp-tabbar (VSCode 스타일)
├─────────────────────────────────────────────┤
│ 제목 — N행 × M열    [표][차트][지도] [↓CSV][↓JSON] [+ 소스로 저장] │  ← cp-toolbar
├─────────────────────────────────────────────┤
│                                             │
│          TableView / TableChart / MapView   │  ← cp-content
│                                             │
└─────────────────────────────────────────────┘
```

**탭 오픈 경로**
| 경로 | sourceRef.kind |
|------|---------------|
| DataSourcePanel "미리보기" 버튼 | `catalog` |
| Job 완료 시 result 테이블 자동 로드 | `db` |

**주요 기능**
- 탭별 독립 뷰 상태 (표 / 차트 / 지도)
- "+ 500행 더 불러오기" 페이지네이션 (`fullyLoaded` 플래그)
- Job `status === "done"` 감지 → `db:listTables` + `db:previewTable` 자동 호출
- 차트: 막대·선·산점도·원그래프 4종 지원, 파이 차트는 레이블·값 컬럼 별도 선택
- 지도: `MapView` 컴포넌트 연동

**Store 변경** (`appStore.ts`)
```typescript
centerTabs: CenterTab[]
activeCenterTabId: string | null
// CenterTab
interface CenterTab {
  id: string; title: string;
  headers: string[]; rows: string[][];
  view: "table" | "chart" | "map";
  sourceRef?: { kind: "catalog"; sourceId: string }
            | { kind: "db"; jobId: string; tableName: string };
  fullyLoaded?: boolean;
}
```

---

### Phase 19 — 레이아웃 드래그 리사이즈

**변경 파일**: `ProjectWindow.tsx`

**3패널 드래그 리사이즈**
```
[DataSource: 240px↕] | [Center: 1fr] | [Chat+Code: 380px↕]
                                        [Chat: 55%↕         ]
                                        ─────── 가로 핸들 ───
                                        [Code: 45%↕         ]
```
- `VHandle` — 열 너비 드래그 (`colDrag`)
- `HHandle` — 오른쪽 패널 상하 비율 드래그 (`rowDrag`)
- CSS 변수로 크기 전달: `--pw-left-w`, `--pw-right-w`, `--pw-chat-ratio`
- 최소/최대 제한: 왼쪽 160–480px, 오른쪽 240–640px, 채팅 비율 20–85%

---

### Phase 20 — Claude SQL 품질 개선

**문제 증상**
- Claude가 "메시지가 미완성인 것 같습니다. 전체 데이터를 반환하는 쿼리를 작성했습니다." 로 응답
- WHERE 조건 없이 `SELECT *` 반환
- `--append-system-prompt` 인수에 한국어 포함 시 Windows cmd.exe에서 truncation 발생

**해결 1: 사용자 메시지를 파일로 전달** (`claude-service.ts`)
```typescript
// 이전: queryClaude({ prompt: userMessage }) — 한국어 직접 전달 → 깨짐
// 이후:
await fs.writeFile(path.join(job.workspaceDir, "request.md"), message, "utf-8");
queryClaude({ prompt: "Read request.md and write query.sql for the user request." })
```

**해결 2: 시스템 프롬프트 단순화** (`system-prompt.ts`)
- 한국어 장문 프롬프트 → 간결한 영문 SQLite 어시스턴트 프롬프트
- 항상 2~3개 SQL 옵션 생성, `-- [옵션 N] 제목` 마커로 구분
- 금지 규칙(INSERT/UPDATE/DELETE/DROP)만 명시

**해결 3: Opus 모델 고정** (`claude-bridge.ts`)
```typescript
const args = ["--model", "opus", ...];
```

**해결 4: 데이터 컨텍스트 자동 갱신** (`job-service.ts`, `main/index.ts`)
| 시점 | 동작 |
|------|------|
| 앱 시작 | 기존 모든 job의 `CLAUDE.md` 갱신 |
| 데이터 소스 추가 | 모든 job `refreshJobSources()` 호출 |
| SQL 실행 완료 | `updateClaudeMd(jobId)` → 신규 테이블 반영 |

**SQL 옵션 선택 UI** (`ChatPanel.tsx`)
- Claude 응답에서 `-- [옵션 N]` 마커 파싱 → 선택 버튼 렌더링
- 선택 시 `jobs:runSql` IPC → 해당 옵션 SQL 실행
- 실행 완료 후 CenterPanel에 결과 테이블 자동 표시

---

### Phase 21 — "소스로 저장" 기능

**목적**: 분석 결과 테이블(DB) 또는 기존 소스 데이터(catalog)를 새 이름의 CSV 데이터 소스로 등록

**신규 IPC 채널**
| 채널 | 설명 |
|------|------|
| `db:saveAsSource(jobId, tableName, sourceName)` | job DB 테이블 전체를 CSV 저장 후 카탈로그 등록 |
| `data:saveAsSource(sourceName, headers, rows)` | 임의 데이터(현재 탭)를 CSV 저장 후 카탈로그 등록 |

**저장 위치**: `workspaceRoot/data/<안전한이름>_<타임스탬프>.csv` (UTF-8 BOM 포함, Excel 한글 호환)

**UI 흐름**
1. CenterPanel 툴바 `+ 소스로 저장` 버튼 (탭에 sourceRef 있으면 항상 표시)
2. 팝업 모달: 소스 이름 입력 (Enter 저장 / Escape 닫기 / 배경 클릭 닫기)
3. 중복 이름 실시간 검증: 입력 중 기존 소스명과 충돌 시 빨간 테두리 + 오류 메시지 + 저장 버튼 비활성화
4. 저장 성공 시 1.2초 후 모달 자동 닫기, DataSourcePanel 목록에 즉시 반영

**소스 종류별 동작 차이**
| 탭 종류 | 동작 |
|---------|------|
| `sourceRef.kind === "db"` | `db:saveAsSource` — DB에서 전체 행 읽기 |
| `sourceRef.kind === "catalog"` | `data:saveAsSource` — 현재 탭 표시 행 사용 (미리보기 범위) |

---

## 현재 디렉터리 구조

```
c:\03_work\FW_AidClaude\
├── HISTORY.md                          ← 이 파일
├── test_data/
│   └── errors.csv
└── app/
    ├── package.json
    ├── electron.vite.config.ts
    ├── tsconfig*.json
    ├── dev.ps1 / build.ps1
    └── src/
        ├── shared/
        │   └── types.ts
        ├── main/
        │   ├── index.ts
        │   └── services/
        │       ├── claude-bridge.ts
        │       ├── claude-detector.ts
        │       ├── claude-service.ts
        │       ├── settings-service.ts
        │       ├── catalog-service.ts
        │       ├── schema-inspector.ts
        │       ├── ast-validator.ts
        │       ├── sqlite-loader.ts
        │       ├── python-runner.ts
        │       ├── job-service.ts
        │       └── system-prompt.ts
        ├── preload/
        │   └── index.ts
        └── renderer/
            ├── index.html
            └── src/
                ├── main.tsx
                ├── App.tsx
                ├── env.d.ts
                ├── store/
                │   └── appStore.ts
                ├── styles/
                │   └── global.css
                └── components/
                    ├── StartScreen.tsx
                    ├── ProjectWindow.tsx
                    ├── DataSourcePanel.tsx
                    ├── ChatPanel.tsx
                    ├── CodePanel.tsx
                    ├── CenterPanel.tsx      ← Phase 18 신규 (ResultPanel 대체)
                    ├── ResultPanel.tsx      ← 미사용 (레거시 보존)
                    └── MapView.tsx
```

---

## 주요 IPC 채널 목록

| 채널 | 방향 | 설명 |
|------|------|------|
| `settings:get/set` | invoke | 앱 설정 조회/저장 |
| `catalog:list/add/update/remove` | invoke | 데이터 소스 CRUD |
| `catalog:testConnection` | invoke | 연결 테스트 |
| `catalog:getSchema` | invoke | 스키마 조회 |
| `catalog:previewData` | invoke | 소스 데이터 미리보기 (최대 50행) |
| `db:listTables` | invoke | job의 `data.db` 테이블 목록 |
| `db:previewTable` | invoke | 테이블 데이터 최대 N행 조회 |
| `db:saveAsSource` | invoke | DB 테이블 전체를 CSV 소스로 저장 |
| `data:saveAsSource` | invoke | 임의 데이터(rows)를 CSV 소스로 저장 |
| `jobs:runSql` | invoke | 임의 SQL을 job의 data.db에 실행 |
| `export:saveText` | invoke | 네이티브 저장 다이얼로그 + 텍스트 파일 쓰기 |
| `export:saveBinary` | invoke | 네이티브 저장 다이얼로그 + 바이너리 파일 쓰기 |
| `claude:probe` | invoke | CLI 탐지 + 인증 확인 |
| `claude:sendMessage` | invoke | Claude 턴 시작 (fire-and-forget) |
| `claude:abort` | invoke | 진행 중 턴 중단 |
| `jobs:create/list` | invoke | 작업 생성/조회 |
| `files:open/readText/readBase64/copyToData` | invoke | 파일 유틸리티 |
| `dialog:openFile` | invoke | 네이티브 파일 선택 다이얼로그 |
| `claude:stream` | push (main→renderer) | 스트리밍 이벤트 |
| `claude:done` | push | 턴 완료 |
| `claude:error` | push | 턴 오류 |
| `job:update` | push | 잡 상태 변경 |
| `job:analyze_code` | push | analyze.py 내용 갱신 |
