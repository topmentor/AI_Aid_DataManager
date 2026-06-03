# AIDA — AI Data Analyst

> 데이터 소스(CSV/JSON/JSONL/MariaDB/Shapefile)를 불러와 **Claude Code / Codex 터미널**과 **SQL**로 조회·분석하고, 결과를 표·차트·지도로 보는 Electron 데스크톱 도구.

AI 에이전트가 작업 공간(워크스페이스)에서 적재된 데이터(`data.db`)를 직접 다루며 분석을 돕고, SQL 패널로 즉석 조회한 결과를 임시 테이블(`result`)로 만들어 시각화합니다.

---

## 아키텍처

SAF 레퍼런스와 동일한 **3-tier** 구조입니다.

```
┌──────────────┐  spawn(java -jar)   ┌─────────────────────────────┐
│ Electron 셸   │ ──────────────────▶ │ SSF 백엔드 (Java)            │
│ (app, main)  │                      │ Embedded Tomcat 9 / SQLite   │
│  ├ 백엔드 기동 │   /api/*.do (HTTP)   │  localhost:<동적포트>/AIDA   │
│  ├ Agent PTY │ ◀── React 렌더러 ──▶ │  ├ Controller / DAO / Service│
│  │  (WS)     │   ws /ws/agent-pty   │  ├ 중앙 app.db (SQLite)      │
│  └ 네이티브   │                      │  └ job별 data.db (SQLite)    │
│    다이얼로그 │                      └─────────────────────────────┘
└──────────────┘
```

- **Electron 셸** (`app/`) — 시작 시 빈 포트를 찾아 SSF 백엔드 jar를 기동하고, React UI를 로드합니다. claude/codex CLI는 백엔드가 PTY로 실행하고 xterm.js 터미널이 WebSocket으로 연결합니다. 파일·폴더 선택, 내보내기 등 네이티브 다이얼로그만 Electron IPC로 처리합니다.
- **SSF 백엔드** (`server/`) — Java(Embedded Tomcat 9, `javax.servlet`)로 데이터 백엔드(`/api/*.do`)를 제공합니다. 설정/카탈로그/스키마/미리보기, job별 `data.db` 적재, SQL 실행, Python 분석, SQL 히스토리, Agent PTY를 담당합니다. 자립형 fat-jar로 빌드됩니다.
- **React 렌더러** — 데이터 소스 패널 / 결과 패널(표·차트·지도·DB 브라우저) / Agent 터미널 / SQL 에디터로 구성됩니다. 데이터는 HTTP로, 터미널은 WebSocket으로 백엔드와 통신합니다.

---

## 주요 기능

- **데이터 소스**: CSV · JSON · JSONL · MariaDB · Shapefile(.shp/.dbf) 등록 → job 생성 시 `data.db`로 적재
- **AI 터미널**: Claude Code / Codex CLI를 작업 공간에서 실행(PTY + xterm.js). CLI 미설치 시 설정 명령으로 **터미널에서 설치** 가능
- **SQL 에디터**: 입력 조회를 `result` 임시 테이블로 만들어 결과 탭 표시. **쿼리 히스토리 저장/불러오기**, 자동 개행
- **결과 보기**: 표(정렬·검색·셀 상세) · 차트(막대/선/산점도/원) · 지도(MapLibre) · DB 브라우저, "소스로 저장"
- **설정창**: 애플리케이션 기본 경로(작업 공간), AI 터미널 폰트(시스템 폰트 콤보박스 + 크기)
- **Python 분석**: 에이전트가 만든 `analyze.py`를 AST 검증 후 실행(`pandas`/`matplotlib`)

---

## 디렉터리 구조

```
AIDA/
├── run.ps1            # 실행(개발) — 의존성 확인 + 백엔드 jar 준비 + 앱 실행
├── build.ps1          # 빌드 — fat-jar + electron-builder 인스톨러
├── stop.ps1           # 정지 — 백엔드(java)·Electron 프로세스 종료 + 임시폴더 정리
├── tools/_common.ps1  # 스크립트 공통(의존성 확인 등)
├── app/               # Electron 셸 + React 렌더러 (electron-vite)
│   ├── electron-builder.yml
│   └── src/{main, preload, renderer, shared}
├── server/            # SSF Java 백엔드 (Maven)
│   ├── pom.xml        # 자립형 fat-jar (Maven Central + sqlite-jdbc/commons-csv/pty4j)
│   ├── build.ps1      # jar 빌드(+스모크)
│   ├── smoke/         # PowerShell 계약 스모크 테스트
│   ├── src/com/ithows/{aida, controller, dao, base, ...}
│   └── web/WEB-INF/   # web.xml, JSP(결과 렌더링)
└── docs/superpowers/  # 설계(spec)·구현 계획(plan) 문서
```

---

## 요구 사항

| 항목 | 버전 | 용도 |
|---|---|---|
| Node.js | ≥ 20 | Electron / 렌더러 빌드 |
| Java (JDK) | ≥ 17 | SSF 백엔드 |
| Maven | 3.9+ | 백엔드 빌드 |
| Python | 3.8+ (선택) | `analyze.py` 분석 (`pandas`, `matplotlib`) |
| Claude Code / Codex CLI | (선택) | AI 터미널 — 없으면 앱 내 "설치 실행"으로 설치 가능 |

> Windows 기준 PowerShell 스크립트를 제공합니다.

---

## 빠른 시작

```powershell
# 개발 실행 (의존성 확인 + 백엔드 jar 빌드 + 앱 실행)
.\run.ps1

# 배포 빌드 — 인스톨러 생성 (app\dist\AIDA-<버전>-setup.exe)
.\build.ps1

# 실행 중인 백엔드/앱 프로세스 정리
.\stop.ps1
```

옵션:
- `.\run.ps1 -Rebuild` — 백엔드 jar를 새로 빌드한 뒤 실행
- `.\build.ps1 -NoPackage` — 인스톨러 없이 jar + 렌더러 빌드까지만
- `.\build.ps1 -SkipSmoke` — jar 빌드 시 스모크 생략

> 패키지 앱은 시스템 **Java 17**이 필요합니다(JRE 미번들). 백엔드 jar와 web 리소스는 인스톨러의 `resources/server/`에 동봉됩니다.

---

## 사용 흐름

1. 좌측 **데이터 소스 패널**에서 소스를 등록합니다(파일 선택/연결 정보 입력).
2. 우측 상단 **AI 터미널**에서 에이전트(Claude/Codex)를 선택하고 **터미널 시작** → 선택 소스를 `data.db`로 적재한 작업 공간에서 CLI가 실행됩니다.
3. 터미널에서 에이전트와 대화하며 분석하거나, 우측 하단 **SQL 패널**에 조회 SQL을 입력하고 **실행**(Ctrl+Enter) → 결과가 `result` 테이블 + 가운데 **result 탭**으로 표시됩니다.
4. 결과 탭에서 표/차트/지도 전환, CSV·JSON 내보내기, "소스로 저장"이 가능합니다.

---

## 데이터 저장 위치

- 기본 데이터 홈: `%USERPROFILE%\.aida` (또는 `-Daida.home` / `AIDA_HOME`).
  - `app.db` — 설정/카탈로그/작업 메타/SQL 히스토리 (중앙 SQLite)
  - `data/` — 등록 소스 복사본
  - `jobs/job_<uuid>/` — 작업별 `data.db`, `CLAUDE.md`, `data_helpers.py`, `output/`
- 작업 공간 경로(작업 데이터 위치)는 **설정창**에서 변경할 수 있습니다(이후 새 작업부터 적용).

---

## 주요 API (백엔드 `/AIDA/api/*.do`)

| 영역 | 엔드포인트 |
|---|---|
| Settings | getSettings, setSettings |
| Catalog | listSources, addSource, updateSource, removeSource, testConnection |
| Schema | getSchema, previewData |
| Jobs | listJobs, createJob, refreshJobSources, runJobSql, runJobAnalysis, getSqlOptions, listOrphanTables, dropOrphanTables |
| DB | listTables, previewTable, saveTableAsSource, saveDataAsSource |
| Files | readText, writeText, readLines, readBase64, copyToData, copyShapefile |
| SQL | listSqlHistory, addSqlHistory, clearSqlHistory |
| Agent PTY | (HTTP) `/api/agent-pty/local` start·kill, `/api/agent-pty/check`, `/api/agent-pty/install` · (WS) `/ws/agent-pty/local/{id}` |

> Swagger UI: 백엔드 기동 후 `/AIDA/docs/`

---

## 개발 / 검증

```powershell
# 백엔드 계약 스모크 (서버 기동 + HTTP/WS 검증)
powershell -File server\smoke\all.ps1

# 프론트 타입체크 / 통합(실제 api-client ↔ 라이브 백엔드)
cd app; npm run typecheck; npm run test:integration
```

설계·구현 기록은 [HISTORY.md](HISTORY.md), 상세 설계 문서는 [docs/superpowers/specs/](docs/superpowers/specs/)에 있습니다.

---

## 기술 스택

- **백엔드**: Java 17, Embedded Tomcat 9(`javax.servlet`), SQLite(`sqlite-jdbc`), commons-csv, Jackson, pty4j, Maven(shade fat-jar)
- **프론트**: Electron 35, electron-vite, React 18 + TypeScript, Zustand, Monaco Editor, xterm.js, Chart.js, MapLibre GL
- **패키징**: electron-builder (NSIS)
