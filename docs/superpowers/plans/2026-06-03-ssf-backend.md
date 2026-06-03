# SSF 백엔드 전환 (하위 프로젝트 A) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** AidClaude의 데이터 백엔드를 SSF(Java/Embedded Tomcat 9) `/api/*.do` HTTP 서버로 전환하고, 서버 단독으로 빌드·기동·계약 검증이 가능하게 만든다.

**Architecture:** `ssf_skell/`을 베이스로 `server/` 모듈을 구성한다. 프레임워크의 컨트롤러 자동등록(Reflections)·응답 JSP 규약을 그대로 쓰되, MariaDB 전용 `JdbcDao` 대신 자체 SQLite 접근 계층(`AppDb`/`SqliteUtil`)으로 중앙 app.db와 job별 data.db를 다룬다. Claude CLI·네이티브 다이얼로그는 Electron에 잔류(범위 외).

**Tech Stack:** Java 8(컴파일)/17(런타임), Maven, Embedded Tomcat 9 (javax.servlet), org.xerial sqlite-jdbc, commons-csv, org.json, mariadb-java-client, maven-shade-plugin.

**참조:** 설계 = `docs/superpowers/specs/2026-06-03-ssf-backend-conversion-design.md`. 기존 TS 구현 = `app/src/main/services/*.ts`, `app/src/main/index.ts`.

**검증 단위 규약:** Java 단위 테스트 프레임워크가 없으므로 각 증분은 (1) `mvn -f server/pom.xml -q clean package` 성공, (2) 서버 기동, (3) PowerShell `Invoke-RestMethod`/`curl`로 엔드포인트 happy-path 확인을 "테스트"로 삼는다. 검증 스크립트는 `server/smoke/*.ps1`로 보존한다.

---

## 마일스톤 개요

- **M0** — `server/` 스캐폴드 + 실행 가능 fat-jar + 헬스 확인
- **M1** — AcContext/데이터 홈 + SQLite 계층(AppDb/SqliteUtil) + app.db 스키마 + 초기화 리스너
- **M2** — Settings (getSettings/setSettings)
- **M3** — Catalog (list/add/update/remove/testConnection)
- **M4** — Schema/Preview: CSV
- **M5** — Jobs: createJob + SourceLoader(CSV) + SystemPromptBuilder
- **M6** — Jobs SQL 실행(runJobSql/runJobAnalysis 일부) + DbController(listTables/previewTable/saveAsSource) + BackupService
- **M7** — 나머지 소스(JSON/JSONL/MariaDB/Shapefile) + getSqlOptions/queryHistory/orphan + Python 분석(runJobAnalysis)

각 마일스톤 끝에서 커밋한다.

---

## Task 0.1: server/ 모듈 스캐폴드 (ssf_skell 복제)

**Files:**
- Create: `server/` (= `ssf_skell/` 복제, `.git`/`target`/`nbproject/private` 제외)

- [ ] **Step 1: ssf_skell을 server로 복제**

PowerShell:
```powershell
$src="c:\03_work\FW_AidClaude\ssf_skell"; $dst="c:\03_work\FW_AidClaude\server"
robocopy $src $dst /E /XD target build .git nbproject\private /XF *.class | Out-Null
Write-Host "copied"
```

- [ ] **Step 2: 복제 확인**

Run: `Test-Path c:\03_work\FW_AidClaude\server\src\com\ithows\base\DispatcherServlet.java`
Expected: `True`

- [ ] **Step 3: 커밋**

```powershell
cd c:\03_work\FW_AidClaude
git add server
git commit -m "chore(server): scaffold SSF backend from ssf_skell"
```

---

## Task 0.2: pom.xml — sqlite/csv 의존성 + 실행 가능 fat-jar

**Files:**
- Create: `server/pom.xml` (= `server/pom-embedded.xml` 기반)

- [ ] **Step 1: pom-embedded.xml을 pom.xml로 복사 후 수정**

`server/pom.xml`을 `pom-embedded.xml` 내용으로 생성하고 아래를 반영:
1. `<artifactId>aidclaude-server</artifactId>`, `<finalName>aidclaude-server</finalName>`
2. `<packaging>jar</packaging>` 로 변경(war→jar; web/는 런타임 디렉터리로 동봉).
3. 의존성 추가:
```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.3.0</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.10.0</version>
</dependency>
```
4. JSP 렌더링을 위해 `tomcat-embed-jasper`는 유지(이미 있음). `maven-war-plugin` 제거.
5. shade 플러그인 추가(Main-Class + 의존성 병합):
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.5.1</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals><goal>shade</goal></goals>
      <configuration>
        <transformers>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
            <mainClass>com.ithows.EmbeddedApplication</mainClass>
          </transformer>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
        </transformers>
        <filters>
          <filter><artifact>*:*</artifact>
            <excludes><exclude>META-INF/*.SF</exclude><exclude>META-INF/*.DSA</exclude><exclude>META-INF/*.RSA</exclude></excludes>
          </filter>
        </filters>
      </configuration>
    </execution>
  </executions>
</plugin>
```
6. system-scope jar(SOXGeoEngine 등)는 shade에 포함되지 않으므로, 실행 시 `web/WEB-INF/lib`로 동봉되어 Tomcat 클래스로더가 로드(개발 모드 `-Dwebapp.base`). M7 Shapefile 전까지는 무관.

- [ ] **Step 2: 빌드**

Run: `mvn -f server/pom.xml -q clean package -DskipTests`
Expected: BUILD SUCCESS, `server/target/aidclaude-server.jar` 생성. (실패 시 1.8 소스 호환/의존성 충돌 수정)

- [ ] **Step 3: 커밋**

```powershell
git add server/pom.xml
git commit -m "build(server): executable fat-jar pom with sqlite-jdbc + commons-csv"
```

---

## Task 0.3: EmbeddedApplication — 컨텍스트/포트/web 디렉터리 + 헬스 기동

**Files:**
- Modify: `server/src/com/ithows/EmbeddedApplication.java` (기본 contextPath `/AidClaude`)
- Create: `server/smoke/health.ps1`

- [ ] **Step 1: 기본 컨텍스트 경로 변경**

`EmbeddedApplication.java`의 `DEFAULT_CONTEXT_PATH`를 `/AidClaude`로 변경. 나머지 로직 유지.

- [ ] **Step 2: 헬스 스모크 스크립트 작성**

`server/smoke/health.ps1`:
```powershell
param([int]$Port=8765)
$home = Join-Path $env:TEMP ("aidclaude_smoke_" + [guid]::NewGuid().ToString("N"))
$jar = "c:\03_work\FW_AidClaude\server\target\aidclaude-server.jar"
$p = Start-Process java -PassThru -ArgumentList @(
  "-Daidclaude.home=$home","-Dserver.port=$Port","-Dwebapp.base=c:\03_work\FW_AidClaude\server\web","-jar",$jar)
try {
  for ($i=0; $i -lt 30; $i++) {
    Start-Sleep 1
    try { $r = Invoke-RestMethod "http://localhost:$Port/AidClaude/api/checkHealth.do"; if ($r) { break } } catch {}
  }
  $r | ConvertTo-Json
} finally { Stop-Process -Id $p.Id -Force }
```

- [ ] **Step 3: 빌드 + 헬스 확인**

Run: `mvn -f server/pom.xml -q clean package -DskipTests; powershell -File server/smoke/health.ps1`
Expected: `{"type":"service","status":"OK"}` 형태 JSON 출력.

> 문제 진단: 404면 web 디렉터리/JSP 누락(개발 모드 `-Dwebapp.base` 확인), 기동 실패면 shade 매니페스트/포트 확인.

- [ ] **Step 4: 커밋**

```powershell
git add server/src/com/ithows/EmbeddedApplication.java server/smoke/health.ps1
git commit -m "feat(server): default /AidClaude context + health smoke test"
```

---

## Task 1.1: AcContext — 데이터 홈/경로 해석

**Files:**
- Create: `server/src/com/ithows/aidclaude/AcContext.java`

- [ ] **Step 1: 구현**

```java
package com.ithows.aidclaude;
import java.io.File;
public final class AcContext {
    private static File home;
    private AcContext(){}
    public static synchronized File home() {
        if (home == null) {
            String p = System.getProperty("aidclaude.home");
            if (p == null || p.isEmpty()) p = System.getenv("AIDCLAUDE_HOME");
            if (p == null || p.isEmpty()) p = new File(System.getProperty("user.home"), ".aidclaude").getAbsolutePath();
            home = new File(p); home.mkdirs();
            new File(home, "data").mkdirs();
            new File(home, "jobs").mkdirs();
        }
        return home;
    }
    public static File appDbFile() { return new File(home(), "app.db"); }
    public static File dataDir()    { return new File(home(), "data"); }
    public static File jobsDir()    { return new File(home(), "jobs"); }
    public static File jobDir(String jobId) { return new File(jobsDir(), "job_" + jobId); }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `mvn -f server/pom.xml -q -o compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: 커밋** — `git commit -m "feat(server): AcContext data-home resolver"`

---

## Task 1.2: SqliteUtil — SQLite 연결/쿼리/멀티스테이트먼트

**Files:**
- Create: `server/src/com/ithows/aidclaude/SqliteUtil.java`

- [ ] **Step 1: 구현**

핵심 메서드(ResultMap 재사용; `com.ithows.ResultMap`):
```java
package com.ithows.aidclaude;
import com.ithows.ResultMap;
import java.sql.*;
import java.util.*;
public final class SqliteUtil {
    static { try { Class.forName("org.sqlite.JDBC"); } catch (Exception ignore) {} }
    private SqliteUtil(){}
    public static Connection open(String path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path);
    }
    public static List<ResultMap> queryForMapList(Connection c, String sql, Object[] p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            try (ResultSet rs = ps.executeQuery()) { return mapRows(rs); }
        }
    }
    public static ResultMap queryForMap(Connection c, String sql, Object[] p) throws SQLException {
        List<ResultMap> l = queryForMapList(c, sql, p); return l.isEmpty()? null : l.get(0);
    }
    public static int update(Connection c, String sql, Object[] p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, p); return ps.executeUpdate(); }
    }
    public static void execScript(Connection c, String sqlText) throws SQLException {
        for (String stmt : splitStatements(sqlText)) {
            String s = stmt.trim(); if (s.isEmpty()) continue;
            try (Statement st = c.createStatement()) { st.execute(s); }
        }
    }
    public static List<String> listTables(Connection c) throws SQLException {
        List<String> out = new ArrayList<>();
        for (ResultMap m : queryForMapList(c,
            "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null))
            out.add(String.valueOf(m.get("name")));
        return out;
    }
    private static void bind(PreparedStatement ps, Object[] p) throws SQLException {
        if (p != null) for (int i=0;i<p.length;i++) ps.setObject(i+1, p[i]);
    }
    private static List<ResultMap> mapRows(ResultSet rs) throws SQLException {
        List<ResultMap> list = new ArrayList<>(); ResultSetMetaData md = rs.getMetaData();
        while (rs.next()) { ResultMap m = new ResultMap();
            for (int i=1;i<=md.getColumnCount();i++) m.put(md.getColumnLabel(i), rs.getObject(i));
            list.add(m);
        } return list;
    }
    // 따옴표/주석 인지 세미콜론 분리
    static List<String> splitStatements(String sql) { /* 구현: '...' "..." -- /*...*/ 무시하고 ; 로 분리 */ }
}
```
`splitStatements`는 단일/이중 따옴표 문자열과 `--`/`/* */` 주석 안의 `;`를 무시하고 분리하도록 상태머신으로 구현한다.

- [ ] **Step 2: 컴파일 확인** — `mvn -f server/pom.xml -q -o compile` → SUCCESS
- [ ] **Step 3: 커밋** — `git commit -m "feat(server): SqliteUtil (connect/query/multi-statement)"`

---

## Task 1.3: AppDb + 초기화 리스너 (app.db 스키마)

**Files:**
- Create: `server/src/com/ithows/aidclaude/AppDb.java`
- Create: `server/src/com/ithows/aidclaude/AidClaudeInitListener.java`
- Modify: `server/web/WEB-INF/web.xml` (listener 등록)

- [ ] **Step 1: AppDb 구현**

```java
package com.ithows.aidclaude;
import java.sql.Connection;
public final class AppDb {
    private AppDb(){}
    public static Connection conn() throws java.sql.SQLException { return SqliteUtil.open(AcContext.appDbFile().getAbsolutePath()); }
    public static void init() throws java.sql.SQLException {
        try (Connection c = conn()) {
            SqliteUtil.execScript(c,
              "CREATE TABLE IF NOT EXISTS ac_settings(key TEXT PRIMARY KEY, value TEXT);" +
              "CREATE TABLE IF NOT EXISTS ac_catalog(id TEXT PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL, config TEXT, created_at INTEGER);" +
              "CREATE TABLE IF NOT EXISTS ac_jobs(id TEXT PRIMARY KEY, user_request TEXT, status TEXT, error_msg TEXT, workspace_dir TEXT, created_at INTEGER, updated_at INTEGER);");
        }
    }
}
```

- [ ] **Step 2: ServletContextListener 구현**

```java
package com.ithows.aidclaude;
import javax.servlet.*;
public class AidClaudeInitListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent e) {
        try { AcContext.home(); AppDb.init(); System.out.println("[AidClaude] home="+AcContext.home()); }
        catch (Exception ex) { ex.printStackTrace(); }
    }
    public void contextDestroyed(ServletContextEvent e) {}
}
```

- [ ] **Step 3: web.xml에 listener 등록**

`server/web/WEB-INF/web.xml`의 `<web-app>` 안에 추가:
```xml
<listener><listener-class>com.ithows.aidclaude.AidClaudeInitListener</listener-class></listener>
```

- [ ] **Step 4: 빌드 + 기동 → app.db 생성 확인**

Run: `mvn -f server/pom.xml -q clean package -DskipTests; powershell -File server/smoke/health.ps1`
Expected: 헬스 OK + 콘솔에 `[AidClaude] home=...` 출력. (스모크의 임시 home에 app.db 생성)

- [ ] **Step 5: 커밋** — `git commit -m "feat(server): central app.db schema + init listener"`

---

## Task 2.1: Settings 도메인 + SettingsDAO

**Files:**
- Create: `server/src/com/ithows/dao/SettingsDAO.java`

설계: settings는 key/value 저장. 기본값(`workspaceRoot` = AcContext.home, 그 외 기존 AppSettings 필드). DAO:
```java
public static java.util.Map<String,String> getAll();        // ac_settings 전체 → 기본값 머지
public static int put(String key, String value);            // upsert
public static int putAll(org.json.JSONObject patch);        // 부분 병합 upsert
```
upsert SQL: `INSERT INTO ac_settings(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value`.

- [ ] **Step 1: 구현** (static, try-catch→null/0, SqliteUtil 사용, AppDb.conn())
- [ ] **Step 2: 컴파일** — SUCCESS
- [ ] **Step 3: 커밋** — `git commit -m "feat(server): SettingsDAO (app.db key/value)"`

## Task 2.2: SettingsController

**Files:**
- Create: `server/src/com/ithows/controller/SettingsController.java`

- [ ] **Step 1: 구현**

```java
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class SettingsController {
  @ControllerMethodInfo(id="/api/getSettings.do")
  @ApiInfo(summary="설정 조회", tag="Settings", method="GET")
  public String getSettings(HttpSession s, HttpServletRequest req, HttpServletResponse res, Object cmd) {
    req.setAttribute("result","OK");
    req.setAttribute("resultMap", SettingsDAO.getAll());
    return "RESULT_COMMON_JSON";
  }
  @ControllerMethodInfo(id="/api/setSettings.do")
  @ApiInfo(summary="설정 저장", tag="Settings", method="POST")
  public String setSettings(HttpSession s, HttpServletRequest req, HttpServletResponse res, Object cmd) {
    JSONObject body = HttpUtil.getBodyJson(req);
    if (body==null){ req.setAttribute("result","ERROR"); req.setAttribute("msg","요청 본문이 필요합니다"); return "RESULT_COMMON_JSON"; }
    SettingsDAO.putAll(body);
    req.setAttribute("result","OK"); req.setAttribute("resultMap", SettingsDAO.getAll());
    return "RESULT_COMMON_JSON";
  }
}
```

- [ ] **Step 2: 빌드 + 스모크 확장**

`server/smoke/settings.ps1`: getSettings 호출 → setSettings `{"workspaceRoot":"X"}` → getSettings에 반영 확인.
Run 후 Expected: 두 번째 getSettings의 resultMap.workspaceRoot == "X".

- [ ] **Step 3: 커밋** — `git commit -m "feat(server): Settings endpoints"`

---

## Task 3.1: DataSource 모델 + CatalogDAO

**Files:**
- Create: `server/src/com/ithows/aidclaude/model/DataSource.java` (id,name,type,config(JSON String))
- Create: `server/src/com/ithows/dao/CatalogDAO.java`

CatalogDAO(static): `list()→List<ResultMap>`(config는 JSON 문자열 그대로; 응답 시 파싱), `add(name,type,configJson)→생성 id`(UUID), `update(id,name,type,configJson)→int`, `remove(id)→int`, `get(id)→ResultMap`.
- [ ] **Step 1: 구현** (created_at = `System.currentTimeMillis()/1000`)
- [ ] **Step 2: 컴파일** — SUCCESS
- [ ] **Step 3: 커밋** — `git commit -m "feat(server): CatalogDAO + DataSource model"`

## Task 3.2: CatalogController + testConnection

**Files:**
- Create: `server/src/com/ithows/controller/CatalogController.java`
- Create: `server/src/com/ithows/aidclaude/ConnTester.java` (소스별 연결 테스트)

엔드포인트: listSources(GET), addSource/updateSource/removeSource/testConnection(POST). config는 응답에서 `org.json`으로 파싱해 객체로 내려보냄(resultMap/resultList의 각 항목에 `config`를 JSON 객체로).
testConnection: CSV/JSON/JSONL=파일 존재+읽기 가능, MariaDB=DriverManager 접속 후 `SELECT 1`.
- [ ] **Step 1: 구현**
- [ ] **Step 2: 빌드 + 스모크** `server/smoke/catalog.ps1`: addSource(csv, 임시 csv 경로) → listSources에 1건 → testConnection ok=true → removeSource → listSources 0건.
- [ ] **Step 3: 커밋** — `git commit -m "feat(server): Catalog endpoints + connection test"`

---

## Task 4.1: SchemaInspector (CSV) + SchemaController

**Files:**
- Create: `server/src/com/ithows/aidclaude/SchemaInspector.java`
- Create: `server/src/com/ithows/controller/SchemaController.java`

CSV: commons-csv로 헤더 추출, 미리보기 N행, rowCount=전체 데이터행 수. 반환 구조:
- getSchema → resultMap `{type, columns:[{name}], rowCount}`
- previewData → resultMap `{title, headers:[], rows:[[]]}`
- [ ] **Step 1: SchemaInspector.inspectCsv / previewCsv 구현** (다른 타입은 M7에서 추가; switch에서 미지원 타입은 ERROR)
- [ ] **Step 2: SchemaController 두 엔드포인트 구현** (id로 CatalogDAO.get → config 파싱 → filePath)
- [ ] **Step 3: 빌드 + 스모크** `server/smoke/schema.ps1`: csv 소스 add → getSchema(columns/rowCount) → previewData(headers/rows).
- [ ] **Step 4: 커밋** — `git commit -m "feat(server): CSV schema + preview"`

---

## Task 5.1: SourceLoader (CSV) + toTableName

**Files:**
- Create: `server/src/com/ithows/aidclaude/SourceLoader.java`
- Create: `server/src/com/ithows/aidclaude/Names.java` (`toTableName(String)` — 기존 sqlite-loader.ts 규칙 포팅)

`Names.toTableName`: 소문자/영숫자·언더스코어 외 `_`, 숫자 시작 시 `t_` 접두, 빈문자열 처리(기존 TS와 동일 결과).
`SourceLoader.loadCsv(Connection jobDb, String tableName, File csv)`: 헤더로 `CREATE TABLE "<t>"(col TEXT,...)`, 트랜잭션 배치 INSERT.
`SourceLoader.load(Connection jobDb, DataSource ds)`: type 분기(현재 CSV만; 나머지 M7).
- [ ] **Step 1: 구현**
- [ ] **Step 2: 컴파일** — SUCCESS
- [ ] **Step 3: 커밋** — `git commit -m "feat(server): SourceLoader (CSV) + table naming"`

## Task 5.2: SystemPromptBuilder (CLAUDE.md + data_helpers.py)

**Files:**
- Create: `server/src/com/ithows/aidclaude/SystemPromptBuilder.java`

`buildClaudeMd(List<SourceTable>)`, `buildDataHelpers()` — 기존 `system-prompt.ts` / `python-runner.ts buildDataHelpers()` 산출물과 동등(테이블 목록 + load/save/query/tables API + 출력 규칙, sqlite3 래퍼).
- [ ] **Step 1: 구현** (문자열 리소스로 작성, data.db 경로는 상대 `data.db`)
- [ ] **Step 2: 컴파일** — SUCCESS
- [ ] **Step 3: 커밋** — `git commit -m "feat(server): CLAUDE.md + data_helpers.py builder"`

## Task 5.3: JobDAO + JobController.createJob/listJobs/refreshSources

**Files:**
- Create: `server/src/com/ithows/dao/JobDAO.java`
- Create: `server/src/com/ithows/controller/JobController.java`

createJob: UUID → jobDir 생성 → `data.db` 생성 + 선택 소스(sourceIds) `SourceLoader.load` → `CLAUDE.md`/`data_helpers.py`/`output/` 기록 → `ac_jobs` insert → resultMap에 job(workspaceDir 포함) 반환.
listJobs: ac_jobs 전체. refreshJobSources: 해당 job data.db 재적재 + CLAUDE.md 갱신.
- [ ] **Step 1: 구현**
- [ ] **Step 2: 빌드 + 스모크** `server/smoke/job.ps1`: csv 소스 add → createJob([sourceId]) → workspaceDir/data.db 존재 + CLAUDE.md 존재 확인 → listJobs 1건.
- [ ] **Step 3: 커밋** — `git commit -m "feat(server): createJob + job workspace/data.db loading"`

---

## Task 6.1: BackupService

**Files:**
- Create: `server/src/com/ithows/aidclaude/BackupService.java`

`backupQuerySql(File workspaceDir)`(query.sql→history/query_NNN.sql, 최대 20 FIFO), `backupResultTable(Connection jobDb)`(result→result_bak_NNN, 최대 10 FIFO), `listQueryHistory(File)`(역순 목록).
- [ ] **Step 1: 구현** + **Step 2: 컴파일** + **Step 3: 커밋** `git commit -m "feat(server): BackupService (query history + result backup)"`

## Task 6.2: SqlRunner + JobController.runJobSql/runJobAnalysis(SQL)

**Files:**
- Create: `server/src/com/ithows/aidclaude/SqlRunner.java`
- Modify: `server/src/com/ithows/controller/JobController.java`

SqlRunner.runSql(jobId, sql): open data.db → backupResultTable → execScript → 성공 시 ac_jobs.status=done, 실패 시 error. runJobAnalysis: workspace의 `query.sql` 읽어 동일 실행(없으면 ERROR). (analyze.py Python 실행은 M7.)
- [ ] **Step 1: 구현**
- [ ] **Step 2: 빌드 + 스모크** `server/smoke/sql.ps1`: createJob → runJobSql(`CREATE TABLE result AS SELECT 1 AS a, 2 AS b`) → ok.
- [ ] **Step 3: 커밋** — `git commit -m "feat(server): runJobSql/runJobAnalysis (SQL exec + backup)"`

## Task 6.3: DbController (listTables/previewTable/saveAsSource)

**Files:**
- Create: `server/src/com/ithows/controller/DbController.java`

listTables/previewTable(job data.db), saveTableAsSource(테이블→CSV(UTF-8 BOM)→data/→CatalogDAO.add), saveDataAsSource(headers/rows→CSV→소스).
- [ ] **Step 1: 구현**
- [ ] **Step 2: 빌드 + 스모크** `server/smoke/db.ps1`: 앞 sql.ps1 흐름 + listTables에 `result` 포함 → previewTable("result") headers/rows → saveTableAsSource → listSources 신규 반영.
- [ ] **Step 3: 커밋** — `git commit -m "feat(server): DB browser + save-as-source endpoints"`

---

## Task 7.1: SchemaInspector/SourceLoader — JSON/JSONL

**Files:**
- Modify: `SchemaInspector.java`, `SourceLoader.java`

JSON(배열: 첫 N항목·length; 객체: key/value), JSONL(앞 N줄/전체 줄 수). 적재: 배열→행, 객체→2열, JSONL→행.
- [ ] **Step 1: 구현** + **Step 2: 스모크**(json/jsonl 소스 add→schema/preview→createJob→previewTable) + **Step 3: 커밋** `git commit -m "feat(server): JSON/JSONL schema/preview/load"`

## Task 7.2: SchemaInspector/SourceLoader/ConnTester — MariaDB

**Files:**
- Modify: 위 3파일

저장된 자격증명으로 mariadb-java-client 접속. 스키마=information_schema(컬럼+TABLE_ROWS), 미리보기=첫 테이블 LIMIT N, 적재=모든 테이블 `<source>_<table>` 복제(TEXT/NUMERIC 매핑), testConnection=`SELECT 1`.
- [ ] **Step 1: 구현** + **Step 2: 스모크**(MariaDB 가용 시 조건부; 없으면 SKIP 로그) + **Step 3: 커밋** `git commit -m "feat(server): MariaDB schema/preview/load"`

## Task 7.3: Shapefile (DbfReader)

**Files:**
- Create: `server/src/com/ithows/aidclaude/shapefile/DbfReader.java`
- Modify: `SchemaInspector.java`, `SourceLoader.java`, `FileController`(copyShapefile)

DBF 헤더 파싱(필드 정의, 레코드 수 = 헤더 byte4 readUInt32LE), 레코드 읽기(인코딩 .cpg→euc-kr 기본). 스키마/미리보기/적재(속성 컬럼). geom은 후속·선택(SOXGeoEngine 확인 후). copyShapefile: .shp/.dbf/.shx/.prj/.cpg 동반 복사 + 인코딩 감지.
- [ ] **Step 1: DbfReader 구현** + **Step 2: 스모크**(test 데이터의 shp 소스 add→schema/preview) + **Step 3: 커밋** `git commit -m "feat(server): Shapefile DBF schema/preview/load"`

## Task 7.4: getSqlOptions / listQueryHistory / orphan tables

**Files:**
- Modify: `JobController.java`

getSqlOptions(query.sql의 `-- [옵션 N]` 마커 파싱 → 옵션 목록), listQueryHistory(BackupService), listOrphanTables/dropOrphanTables(전체 job DB 스캔; 보존규칙=result/result_bak_N/카탈로그 소스 테이블; `Names.toTableName` + MariaDB는 접두 매칭).
- [ ] **Step 1: 구현** + **Step 2: 스모크** + **Step 3: 커밋** `git commit -m "feat(server): SQL options + query history + orphan cleanup"`

## Task 7.5: FileController + PythonRunner(analyze.py)

**Files:**
- Create: `server/src/com/ithows/controller/FileController.java`
- Create: `server/src/com/ithows/aidclaude/PythonRunner.java`
- Create: `server/python/ast_validate.py`
- Modify: `JobController.runJobAnalysis` (analyze.py 분기)

FileController: readText/writeText/readLines/readBase64/copyToData/copyShapefile(데이터 홈 경로 제한). PythonRunner: `ProcessCall.normalCallCommand(["python", ast_validate.py, analyze.py])`로 AST 검증 → 통과 시 워크스페이스 cwd `python analyze.py` 실행, stdout/stderr 수집. python 명령은 AppConfig `python_command`(없으면 `python`). runJobAnalysis는 analyze.py 존재 시 이 경로, 아니면 query.sql 경로.
- [ ] **Step 1: ast_validate.py 작성**(차단 목록: os/subprocess/socket/requests/urllib/eval/exec/__import__/절대경로 open → 위반 시 exit 2 + 사유)
- [ ] **Step 2: FileController/PythonRunner 구현**
- [ ] **Step 3: 빌드 + 스모크** `server/smoke/files.ps1`(readText/writeText 왕복, copyToData) + python 가용 시 analyze 실행.
- [ ] **Step 4: 커밋** — `git commit -m "feat(server): File endpoints + Python analysis (AST-validated)"`

---

## Task 8: 통합 스모크 + 문서

**Files:**
- Create: `server/smoke/all.ps1` (전체 happy-path 순차 실행)
- Modify: `HISTORY.md` (SSF 전환 섹션 추가)

- [ ] **Step 1: all.ps1로 M2~M7 핵심 경로 일괄 검증** → 전부 OK.
- [ ] **Step 2: HISTORY.md에 "백엔드 SSF 전환(서버 A)" 섹션 추가** (엔드포인트 계약 표 포함).
- [ ] **Step 3: 커밋** — `git commit -m "docs: record SSF backend conversion (sub-project A)"`

---

## Self-Review 결과 (작성자 점검)

- **스펙 커버리지**: 설계 6장 계약의 모든 엔드포인트가 Task에 매핑됨(Settings=2.2, Catalog=3.2, Schema=4.1/7.x, Jobs=5.3/6.2/7.4, Db=6.3, Files=7.5). 빌드 리스크(fat-jar)=0.2, SQLite계층=1.x, Python=7.5, Shapefile=7.3.
- **플레이스홀더**: `SqliteUtil.splitStatements`는 본문에서 "상태머신 구현" 명시(구현 시 따옴표/주석 처리). 그 외 시그니처·SQL 구체화.
- **타입 일관성**: `ResultMap`/`SqliteUtil`/`AcContext`/`Names.toTableName`/`AppDb.conn` 명칭이 전 Task에서 일치.
- **검증 가능성**: 각 마일스톤이 스모크 스크립트로 독립 검증.

## 후속(범위 외, 별도 플랜)
- B: Electron 셸 + Claude IPC 이식, webapp 로드, `-Daidclaude.home`/포트 전달, 헬스 대기.
- C: webapp(Vanilla TS) — React 컴포넌트 8종 재작성 + HTTP API 클라이언트.
- D: setup.ps1 → Maven 빌드 + jar 복사 + Electron 실행/패키징 통합.
