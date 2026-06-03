# AidClaude 백엔드 SSF 전환 — 설계 문서

- 작성일: 2026-06-03
- 대상: AidClaude (Electron 데이터 분석·시각화 도구)
- 목표: Electron main(Node.js/TypeScript) 백엔드를 **SSF(Simple Spring-like Framework, Java/Embedded Tomcat 9)** 백엔드로 전환

---

## 1. 배경과 목표

현재 AidClaude는 Electron 앱이며 "백엔드"가 Electron main 프로세스(Node.js/TypeScript)
안에 IPC 핸들러 형태로 들어있다. 이를 SAF 레퍼런스와 동일한 **3-tier 아키텍처**로 전환한다.

```
┌──────────────┐   spawn(java -jar)   ┌─────────────────────────────┐
│  electron 셸  │ ───────────────────▶ │ SSF Java 백엔드             │
│  (main.js)   │                       │ Embedded Tomcat 9            │
│              │   /api/*.do (HTTP)    │ localhost:<port>/AidClaude   │
│  ├ Claude IPC│ ◀──── webapp ────────▶│ ├ Controller / DAO / Service │
│  ├ 네이티브   │                       │ ├ 중앙 app.db (SQLite)       │
│  │  다이얼로그 │                       │ └ job별 data.db (SQLite)     │
│  └ webapp 로드│                       └─────────────────────────────┘
└──────────────┘
        ▲  webapp (Vanilla TS + Vite) — HTTP fetch + Claude IPC
```

### 전환 범위 결정 (확정)

| 항목 | 결정 |
|---|---|
| 데이터 백엔드 | **SSF(Java)로 이동** — 설정/카탈로그/스키마/미리보기/SQLite적재/SQL실행/Python분석/백업/정리 |
| Claude CLI 스트리밍 | **Electron main 유지**(IPC) — `claude-bridge/service/detector` 그대로 |
| 네이티브 다이얼로그 | **Electron 유지**(IPC) — 파일 선택, 저장 다이얼로그 |
| 프론트엔드 | React → **Vanilla TS 재작성** (별도 하위 프로젝트 C) |
| 빌드 | **Maven**(pom-embedded) fat-jar → Electron이 `java -jar`로 기동 |
| 중앙 메타 저장소 | **중앙 SQLite**(app.db) — `ac_catalog`, `ac_settings`, `ac_jobs` 테이블 |
| job별 데이터 | **기존 job_<uuid>/data.db 모델 유지** |

### 하위 프로젝트 분해

본 문서는 **하위 프로젝트 A (SSF 데이터 백엔드)** 만 상세 설계한다. B(Electron 셸+Claude IPC),
C(webapp Vanilla TS), D(빌드/실행 스크립트)는 A 완료 후 각자 스펙→플랜→구현 사이클을 가진다.
A는 `/api/*.do` **계약(contract)** 을 정의하므로 B·C의 기반이다.

---

## 2. 기술적 사전 조사 결론 (ssf_skell)

- **프레임워크 베이스**: `javax.servlet`(Tomcat 9.0.98 embed, Servlet 4). **jakarta 아님.**
- **컨트롤러 등록**: `DispatcherServlet.init()`이 `com.ithows.controller` 패키지를 Reflections로 스캔
  → `@ControllerClassInfo`/`@ControllerMethodInfo` 자동 매핑. **XML 수정 불필요.**
- **호출 경로**: `*.do` → DispatcherServlet → `controllerPage`(예 `/api/_api.jsp`) → `PageManager.callController()`가
  `method(HttpSession, HttpServletRequest, HttpServletResponse, Object)`를 리플렉션 호출.
- **응답 렌더링**: 반환 문자열 → JSP 매핑
  - `RESULT_COMMON_JSON` → `commonResultJson.jsp` → `{result, msg, resultMap, count, resultList}`
  - `RESULT_SIMPLE_JSON` → `result` 속성(JSON 문자열) 그대로 출력
  - `NO_PAGE` → JSP forward 생략 (컨트롤러가 response에 직접 작성)
- **파라미터**: `HttpUtil.getParameterString/Int/...`, `HttpUtil.getBodyJson(request)` → `org.json.JSONObject`(null 가능)
- **DB**: `JdbcDao`는 **MariaDB 단일 중앙 DataSource에 하드와이어**(connpool.xml, soxuser/sox123). SQLite·멀티파일에 부적합.
- **Python**: `PythonCallUtil.callPython(script, JSONObject, timeoutSec)` → req/res 임시 JSON 파일 방식.
  `ProcessCall.normalCallCommand(String[])` → `ArrayList<String>`(stdout 라인).
- **설정**: `AppConfig.getConf(key)` ← `WEB-INF/classes/configplatform.xml`.
- **빌드 리스크**: `pom-embedded.xml`은 `packaging=war`이지만 `java -jar` 실행용 **Main-Class 매니페스트가 없음**.
  → 해결책 필요(아래 4장).

---

## 3. 모듈/디렉터리 구조

SSF 백엔드는 `ssf_skell/`을 베이스로 **`server/`** 모듈로 구성한다(스켈레톤은 참조 보존).

```
c:\03_work\FW_AidClaude\
├── server/                     ← 신규: SSF 백엔드 (ssf_skell 기반)
│   ├── pom.xml                 ← pom-embedded.xml 기반 + sqlite-jdbc + shade/assembly
│   ├── lib/                    ← 기존 system-scope jar (SOXGeoEngine 등)
│   ├── src/com/ithows/
│   │   ├── (프레임워크 base/util — 그대로)
│   │   ├── controller/         ← AidClaude 컨트롤러 신규
│   │   │   ├── HealthController.java        (기존)
│   │   │   ├── SettingsController.java      (신규)
│   │   │   ├── CatalogController.java       (신규)
│   │   │   ├── SchemaController.java        (신규)
│   │   │   ├── JobController.java           (신규)
│   │   │   ├── DbController.java            (신규)
│   │   │   └── FileController.java          (신규)
│   │   ├── dao/
│   │   │   ├── CatalogDAO.java              (신규, 중앙 app.db)
│   │   │   ├── SettingsDAO.java             (신규, 중앙 app.db)
│   │   │   └── JobDAO.java                  (신규, 중앙 app.db)
│   │   └── aidclaude/           ← AidClaude 전용 서비스 패키지 (신규)
│   │       ├── AcContext.java               ← 데이터 홈/경로/싱글턴
│   │       ├── AppDb.java                   ← 중앙 app.db 연결·스키마 init
│   │       ├── SqliteUtil.java              ← SQLite 연결/멀티스테이트먼트 헬퍼
│   │       ├── SchemaInspector.java         ← 5종 소스 스키마/미리보기
│   │       ├── SourceLoader.java            ← 소스 → job data.db 적재
│   │       ├── shapefile/DbfReader.java     ← .dbf 속성 읽기 (+ SOXGeoEngine geom)
│   │       ├── SqlRunner.java               ← job data.db SQL 실행
│   │       ├── PythonRunner.java            ← analyze.py 실행 + AST 검증
│   │       ├── BackupService.java           ← query.sql 히스토리 / result 백업
│   │       ├── SystemPromptBuilder.java     ← CLAUDE.md / data_helpers.py 생성
│   │       └── model/...                    ← DataSource 등 DTO
│   └── web/                     ← WEB-INF/jsp(result JSP들), classes/config
├── app/                         ← Electron (현재; B·C에서 셸+webapp로 재편)
└── docs/superpowers/specs/      ← 본 문서
```

> 프레임워크의 GIS/위치측위 잔여 코드(`com.sox.ltex.*` 등)는 컴파일만 되면 그대로 둔다(YAGNI: 제거 리팩터링은 범위 외). SOXGeoEngine은 Shapefile 처리에 활용 가능.

### 데이터 홈 (AcContext)

Electron userData 대신 SSF가 소유하는 **데이터 홈**을 둔다. 우선순위:
1. 시스템 프로퍼티 `-Daidclaude.home=<path>` (Electron이 기동 시 지정)
2. 환경변수 `AIDCLAUDE_HOME`
3. 기본값 `${user.home}/.aidclaude`

```
<home>/
├── app.db                 ← 중앙 메타 (ac_catalog, ac_settings, ac_jobs)
├── data/                  ← 소스 원본 복사본 (CSV 등)
└── jobs/job_<uuid>/
    ├── CLAUDE.md
    ├── data.db
    ├── data_helpers.py
    ├── request.md / query.sql / analyze.py
    ├── history/query_NNN.sql
    └── output/
```

`workspaceRoot` 설정값은 `<home>` 기본값과 동일하게 초기화하되 `ac_settings`로 변경 가능.

---

## 4. 빌드/실행 (sub-project A 범위 내 최소)

- `server/pom.xml` = `pom-embedded.xml` 기반에 추가:
  - `org.xerial:sqlite-jdbc:3.45.x` (신규 의존성)
  - `org.apache.commons:commons-csv:1.10.0` (CSV 파싱)
  - **실행 가능 jar** 생성: `maven-shade-plugin`으로 `Main-Class: com.ithows.EmbeddedApplication` + 의존성 포함 fat-jar.
    - WAR 정적 리소스(web/)는 `addWebapp`이 디렉터리를 읽으므로, 프로덕션 실행 시 `-Dwebapp.base`로 추출된 web 디렉터리를 지정하거나 fat-jar 옆 `web/`를 동봉.
    - 1순위 검증 방식: `mvn -f server/pom.xml clean package` → `java -Daidclaude.home=... -Dserver.port=... -jar server/target/aidclaude-server.jar` 가 뜨고 `/AidClaude/api/checkHealth.do` 200 응답.
  - 컴파일 타깃: 현 1.8 유지(스켈레톤 호환). Java 17 런타임에서 동작 확인됨.
- 컨텍스트 경로: 기본 `/AidClaude` (시스템 프로퍼티 `server.contextPath`로 오버라이드).
- 포트: 기본 8765(임의 고정) — Electron이 `-Dserver.port`로 지정, 헬스 체크로 기동 확인.
- 개발 모드: `-Dwebapp.base=server/web` + `target/classes`로 핫한 디렉터리 실행(스켈레톤 기존 지원).

> D(빌드 스크립트)에서 setup.ps1을 Maven 빌드 + jar 산출 + Electron 연동으로 확장하지만, A에서는 **서버 단독 빌드·실행·헬스 확인**까지만 책임진다.

---

## 5. SQLite 접근 계층 (JdbcDao 비사용)

`JdbcDao`(MariaDB 전용)는 그대로 두고, AidClaude는 자체 SQLite 헬퍼를 쓴다.

- `SqliteUtil.open(path)` → `DriverManager.getConnection("jdbc:sqlite:"+path)` (autoCommit=true 기본).
- `SqliteUtil.queryForMapList/queryForMap/update(conn, sql, params)` — ResultMap 재사용, `?` 파라미터.
- `SqliteUtil.execScript(conn, sqlText)` — 세미콜론 분리 멀티스테이트먼트 실행(문자열/주석 인지 분리).
- `AppDb`:
  - `AppDb.conn()` → 중앙 app.db 연결(경로 = `<home>/app.db`).
  - `AppDb.init()` — 기동 시 `CREATE TABLE IF NOT EXISTS`:
    - `ac_settings(key TEXT PRIMARY KEY, value TEXT)`
    - `ac_catalog(id TEXT PRIMARY KEY, name TEXT, type TEXT, config TEXT/*JSON*/, created_at INTEGER)`
    - `ac_jobs(id TEXT PRIMARY KEY, user_request TEXT, status TEXT, error_msg TEXT, workspace_dir TEXT, created_at INTEGER, updated_at INTEGER)`
  - 초기화 트리거: `SOXServletContextListener` 또는 `AppConfig.init()`에서 `AcContext.init()`+`AppDb.init()` 호출(전용 신규 ServletContextListener 추가가 더 깔끔 → `AidClaudeInitListener`).

DAO(`CatalogDAO`/`SettingsDAO`/`JobDAO`)는 SSF 규칙(static, try-catch→null/0, `?` 파라미터)을 따르되 `SqliteUtil`을 통해 app.db에 접근한다. job별 data.db는 `SqlRunner`/`SourceLoader`/`DbController`가 경로로 직접 연결한다.

---

## 6. API 계약 (IPC → /api/*.do) ★ 핵심

응답은 모두 SSF `RESULT_COMMON_JSON` 규약(`result`="OK"/"ERROR"/"NO", `msg`, `resultMap`, `resultList`).
컨텍스트 경로 `/AidClaude` 접두는 생략. **POST는 JSON 바디**, GET은 쿼리 파라미터.

### 6.1 Settings — `SettingsController`
| 기존 IPC | 신규 엔드포인트 | M | 입력 | 출력(resultMap) |
|---|---|---|---|---|
| settings:get | `/api/getSettings.do` | GET | — | 설정 전체 |
| settings:set | `/api/setSettings.do` | POST | 부분 설정 JSON | 병합된 설정 전체 |

### 6.2 Catalog — `CatalogController`
| 기존 IPC | 신규 엔드포인트 | M | 입력 | 출력 |
|---|---|---|---|---|
| catalog:list | `/api/listSources.do` | GET | — | resultList: 소스[] |
| catalog:add | `/api/addSource.do` | POST | {name,type,config} | resultMap: 추가된 소스 |
| catalog:update | `/api/updateSource.do` | POST | 소스 전체 | resultMap |
| catalog:remove | `/api/removeSource.do` | POST | {id} | result |
| catalog:testConnection | `/api/testConnection.do` | POST | {id} | resultMap:{ok,error?} |

> `addSource` 후 "기존 job들의 data.db/CLAUDE.md 갱신"은 비동기 사이드이펙트였음. A에서는 `refreshAllJobSources()`를 동기 호출(또는 백그라운드 스레드)로 보존.

### 6.3 Schema/Preview — `SchemaController`
| 기존 IPC | 신규 엔드포인트 | M | 입력 | 출력 |
|---|---|---|---|---|
| catalog:getSchema | `/api/getSchema.do` | POST | {id} | resultMap: 스키마(rowCount 포함) |
| catalog:previewData | `/api/previewData.do` | POST | {id, limit?} | resultMap:{title,headers,rows} |

5종 소스 지원: CSV(commons-csv), JSON(배열/객체), JSONL, MariaDB(information_schema + LIMIT), Shapefile(.dbf 속성 + rowCount).

### 6.4 Jobs — `JobController`
| 기존 IPC | 신규 엔드포인트 | M | 입력 | 출력 |
|---|---|---|---|---|
| jobs:list | `/api/listJobs.do` | GET | — | resultList |
| jobs:create | `/api/createJob.do` | POST | {userRequest, sourceIds[]} | resultMap: job(workspaceDir 포함) |
| jobs:refreshSources | `/api/refreshJobSources.do` | POST | {jobId} | result |
| jobs:runSql | `/api/runJobSql.do` | POST | {jobId, sql} | resultMap:{ok,error?} |
| jobs:runAnalysis | `/api/runJobAnalysis.do` | POST | {jobId} | resultMap:{ok,error?} (query.sql 실행) |
| jobs:getSqlOptions | `/api/getSqlOptions.do` | POST | {jobId} | resultList: SQL 옵션 |
| jobs:listQueryHistory | `/api/listQueryHistory.do` | POST | {jobId} | resultList |
| jobs:listAllOrphanTables | `/api/listOrphanTables.do` | GET | — | resultList:[{jobId,jobLabel,tables[]}] |
| jobs:dropAllOrphanTables | `/api/dropOrphanTables.do` | POST | — | resultMap:{dropped} |

- `createJob`: 워크스페이스 생성 → `SourceLoader`로 data.db 적재 → `data_helpers.py`/`CLAUDE.md` 기록 → app.db `ac_jobs` insert. **Claude 호출 없음**(Electron이 workspaceDir로 Claude 세션 cwd 설정).
- `runJobSql`/`runJobAnalysis`: 동기 실행. 실행 전 result 테이블 백업 → 실행 → `ac_jobs.status` 갱신 → CLAUDE.md 갱신. 결과는 HTTP 응답으로 반환(기존 `job:update` push 대체).
- Python 분석 흐름은 `runJobAnalysis`가 `analyze.py` 존재 시 AST 검증 후 실행하도록 확장(아래 8장). SQL 옵션/쿼리 흐름은 `query.sql` 기준 유지.

### 6.5 DB 브라우저 / 소스 저장 — `DbController`
| 기존 IPC | 신규 엔드포인트 | M | 입력 | 출력 |
|---|---|---|---|---|
| db:listTables | `/api/listTables.do` | POST | {jobId} | resultList: 테이블명[] |
| db:previewTable | `/api/previewTable.do` | POST | {jobId, tableName, limit?} | resultMap:{title,headers,rows} |
| db:saveAsSource | `/api/saveTableAsSource.do` | POST | {jobId,tableName,sourceName} | resultMap: 소스 |
| data:saveAsSource | `/api/saveDataAsSource.do` | POST | {sourceName,headers[],rows[][]} | resultMap: 소스 |

### 6.6 Files — `FileController`
| 기존 IPC | 신규 엔드포인트 | M | 비고 |
|---|---|---|---|
| files:readText | `/api/readText.do` | POST {path} | 데이터 홈 하위 경로로 제한(경로 탈출 방지) |
| files:writeText | `/api/writeText.do` | POST {path,content} | 동일 제한 |
| files:readLines | `/api/readLines.do` | POST {path,count} | 동일 |
| files:readBase64 | `/api/readBase64.do` | POST {path} | PNG 등 base64(차트 표시용) |
| files:copyToData | `/api/copyToData.do` | POST {srcPath} | Electron이 준 경로를 data/로 복사 |
| files:copyShapefile | `/api/copyShapefile.do` | POST {srcShpPath} | .shp/.dbf/.shx/.prj/.cpg 동반 복사 + 인코딩 감지 |

**Electron 잔류(이 계약에 없음)**: `claude:*`, `dialog:openFile`, `export:saveText/saveBinary`, `files:open`(shell open).
이들은 B(Electron)에서 IPC로 유지. webapp은 파일 선택 시 Electron IPC로 경로를 받아 `copyToData.do`/`copyShapefile.do` 호출.

---

## 7. 소스 적재 / 스키마 / SQL (서비스 상세)

- **SchemaInspector** (소스별):
  - CSV: commons-csv로 헤더+샘플; rowCount는 전체 라인 카운트.
  - JSON: 배열이면 첫 N항목·length, 객체면 키/값 테이블.
  - JSONL: 앞 N줄 파싱 + 전체 줄 수.
  - MariaDB: 저장된 자격증명으로 접속 → `information_schema.COLUMNS/TABLES`(TABLE_ROWS) → 테이블별 컬럼·rowCount, 미리보기는 첫 테이블 `SELECT * LIMIT N`.
  - Shapefile: `DbfReader`로 .dbf 필드/레코드 수, 미리보기는 앞 N레코드. (인코딩: .cpg→euc-kr 기본)
- **SourceLoader**: 소스→job data.db.
  - CSV/JSONL/JSON(배열): 컬럼 추론 → `CREATE TABLE` → 배치 INSERT(트랜잭션).
  - JSON(객체): key/value 2열 테이블.
  - MariaDB: 모든 테이블을 `<source>_<table>`로 복제(타입은 TEXT/NUMERIC 단순 매핑).
  - Shapefile: 속성 컬럼 + (선택) `geom_wkt` 컬럼(SOXGeoEngine/JTS로 WKT 직렬화).
  - 테이블명 규칙: 기존 `toTableName()`과 동일(SQL-safe 변환).
- **SqlRunner**: `execScript`로 멀티스테이트먼트 실행, 실행 전 `BackupService.backupResultTable`.
- **SystemPromptBuilder**: 기존 `system-prompt.ts`/`python-runner.ts`의 산출물과 동등한 `CLAUDE.md`(테이블 목록 + data_helpers API + 출력 규칙)와 `data_helpers.py`(자격증명 없는 sqlite3 래퍼: load/save/query/tables)를 생성.

---

## 8. Python 분석 + AST 검증

- `analyze.py`는 Claude가 워크스페이스에 생성(Electron 경유). SSF는 실행만 담당.
- **AST 검증**: 번들 파이썬 스크립트 `validators/ast_validate.py`(server/python/에 동봉)를
  `ProcessCall.normalCallCommand(["python", ast_validate.py, analyze.py])`로 호출 → 차단 목록(os/subprocess/socket/requests/eval/exec/절대경로 open 등) 위반 시 비-0 종료 + 사유 출력.
- **실행**: 검증 통과 시 워크스페이스 cwd에서 `python analyze.py` 실행(ProcessCall), stdout/stderr 수집, 산출물은 `output/`.
- python 명령은 `configplatform.xml`의 `python_command`(기본 `python`) 사용.
- 검증/실행 실패는 `runJobAnalysis` 응답의 `result=ERROR`+`msg`로 반환.

> 위험: Shapefile(SOXGeoEngine 연동)과 Python 분석은 가장 불확실. 구현 플랜에서 **별도 후순위 증분**으로 두고, 핵심(설정/카탈로그/CSV·JSON·JSONL 스키마·적재·SQL·DB브라우저)을 먼저 완성·검증한다.

---

## 9. 보안/규약

- 컨트롤러: no-arg 생성자, 4-인자 시그니처, `@ControllerMethodInfo(id="/api/xxx.do")`, `@ApiInfo`.
- 입력은 `HttpUtil.getParameterXxx`/`getBodyJson`만 사용. SQL은 `?` 파라미터(사용자 SQL 실행 엔드포인트 제외 — 그건 의도된 임의 SQL).
- 파일 엔드포인트는 데이터 홈 하위로 경로 제한(`canonicalPath.startsWith(home)`), 경로 탈출 차단.
- 로컬 단독 실행이므로 loginRequired=false, requiredSecurityLevel=0. (Rate limit은 위치측위 API 한정이라 무관.)
- CORS: webapp이 동일 오리진(Electron이 SSF가 서빙하는 정적 자원 로드) 또는 Vite dev 서버(다른 포트)일 수 있음 → dev 편의를 위해 `*.do` 응답에 `Access-Control-Allow-Origin: http://localhost:<vitePort>` 허용 옵션(개발 모드). 운영은 동일 오리진.

---

## 10. 검증 전략 (A 완료 기준)

A는 webapp 없이 **독립 검증** 가능해야 한다.

1. `mvn -f server/pom.xml clean package` 성공 + fat-jar 생성.
2. `java -Daidclaude.home=<tmp> -Dserver.port=8765 -jar ...` 기동 → `/AidClaude/api/checkHealth.do` 200.
3. curl/PowerShell로 계약 검증(핵심 happy-path):
   - 설정 get/set 왕복.
   - CSV 소스 add → listSources → getSchema → previewData → createJob → listTables → previewTable.
   - runJobSql(`CREATE TABLE result AS SELECT ...`) → previewTable("result").
   - saveTableAsSource → listSources에 신규 반영.
4. (후순위 증분) JSON/JSONL/MariaDB/Shapefile, Python 분석, 백업/고아정리 각각 검증.
5. `/docs/` Swagger UI에 신규 엔드포인트 노출 확인.

---

## 11. 명시적 비범위 (YAGNI)

- 프론트엔드 재작성(C), Electron 셸/Claude IPC 이식(B), 통합 빌드 스크립트(D)는 본 문서 범위 외.
- `com.sox.ltex.*` 등 프레임워크 잔여 기능 제거/리팩터링.
- 인증/멀티유저/원격 접근(로컬 단독 가정).
- MariaDB 소스의 타입 정밀 매핑(단순 TEXT/NUMERIC로 충분).

---

## 12. 미해결/리스크 정리

| 리스크 | 대응 |
|---|---|
| 실행 가능 fat-jar(war→jar) | shade-plugin으로 Main-Class+의존성 포함, 초기 증분에서 헬스로 검증 |
| SOXGeoEngine Shapefile API 미상 | 우선 `DbfReader`(속성/행수)만으로 스키마·적재, geom은 후속·선택 |
| sqlite-jdbc 멀티스테이트먼트 | `execScript` 수동 분리 구현 + 테스트 |
| 워크스페이스 경로 공유(Electron↔SSF) | 동일 머신·동일 데이터 홈, 절대경로로 합의(B 단계에서 `-Daidclaude.home` 전달) |
| 한글/인코딩(CSV/DBF) | UTF-8 기본, DBF는 .cpg→euc-kr; 저장 CSV는 UTF-8 BOM |
