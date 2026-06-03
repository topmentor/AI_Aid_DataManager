# SSF 웹 애플리케이션 개발 매뉴얼

> SSF (Simple Spring-like Framework) 를 이용해 웹 애플리케이션을 처음부터 만들어가는 실전 매뉴얼입니다.
> 프로젝트 복제 → 설정 → 첫 API → DAO → 보안 → 빌드/실행까지의 전체 흐름을 다룹니다.

관련 문서:
- [../README.md](../README.md) — 프로젝트 개요
- [../SSF_Framework_RAG_Document.md](../SSF_Framework_RAG_Document.md) — 프레임워크 상세 레퍼런스
- [Embedded_Tomcat_Guide.md](Embedded_Tomcat_Guide.md) — Embedded Tomcat 빌드 가이드

---

## 목차

1. [시작하기 전에](#1-시작하기-전에)
2. [개발 환경 구성](#2-개발-환경-구성)
3. [새 프로젝트 생성](#3-새-프로젝트-생성)
4. [기본 설정](#4-기본-설정)
5. [TutorialController — 패턴 카탈로그](#5-tutorialcontroller--패턴-카탈로그)
6. [Step 1 — 첫 컨트롤러 만들기 (Hello World)](#step-1--첫-컨트롤러-만들기-hello-world)
7. [Step 2 — DAO 레이어 작성](#step-2--dao-레이어-작성)
8. [Step 3 — 요청 파라미터 처리](#step-3--요청-파라미터-처리)
9. [Step 4 — 응답 형식과 JSP 뷰](#step-4--응답-형식과-jsp-뷰)
10. [Step 5 — 보안 적용 (로그인, API Key)](#step-5--보안-적용-로그인-api-key)
11. [Step 6 — API 문서화 (Swagger)](#step-6--api-문서화-swagger)
12. [Step 7 — 빌드 및 실행](#step-7--빌드-및-실행)
13. [Step 8 — 파이썬 스크립트 연동](#step-8--파이썬-스크립트-연동)
14. [트러블슈팅](#트러블슈팅)
15. [체크리스트](#체크리스트)

---

## 1. 시작하기 전에

### 대상 독자
- Java 웹 애플리케이션을 개발해본 경험이 있는 개발자
- Servlet / JSP / JDBC 기본 개념을 알고 있는 사람
- Spring 을 써봤다면 SSF 의 경량 구조가 친숙하게 느껴질 것

### SSF 의 설계 방향
- **어노테이션 기반 컨트롤러 라우팅** — `@ControllerClassInfo`, `@ControllerMethodInfo`
- **명시적 JDBC** — `JdbcDao.queryForXxx(sql, params)` 로 파라미터화 쿼리만 사용
- **단순한 반환 규약** — 컨트롤러 메서드가 JSP 경로 또는 특수 상수(`RESULT_COMMON_JSON`)를 문자열로 반환
- **선언적 보안** — 어노테이션으로 로그인/권한/API Key 를 선언하면 `DispatcherServlet` 이 자동 검증

---

## 2. 개발 환경 구성

| 항목 | 버전 / 요건 |
|------|-------------|
| JDK | Java 8+ |
| 빌드 | Maven 3.6+ (또는 Ant — `build.xml` 사용 시) |
| DB | MariaDB / MySQL |
| IDE | IntelliJ IDEA, NetBeans, VSCode 등 |
| 실행 | 외부 Tomcat 9 또는 Embedded Tomcat (내장) |

JDK, Maven, DB 설치 후 아래 명령으로 환경을 확인합니다.

```bash
java -version       # Java 8 이상
mvn -v              # Maven 설치 확인
```

---

## 3. 새 프로젝트 생성

### 3.1 프로젝트 복제

이 저장소를 복제한 뒤 새 폴더에 복사합니다.

```powershell
# PowerShell 예시
Copy-Item -Recurse C:\03_work\SSF2026 C:\03_work\MyApp
cd C:\03_work\MyApp
```

### 3.2 프로젝트 이름 일괄 변경

루트에 포함된 [../rename-project.ps1](../rename-project.ps1) 스크립트로 모든 소스/설정에 하드코딩된 `SSF2026` 식별자를 새 이름으로 일괄 변경합니다.

```powershell
# 1. 변경 미리보기
.\rename-project.ps1 -NewName MyApp -DryRun

# 2. 실제 적용
.\rename-project.ps1 -NewName MyApp
```

처리 후 수동으로 할 일:
- 프로젝트 루트 **디렉토리 이름** 변경 (예: `SSF2026` → `MyApp`)
- IDE 캐시 재로드 (`.idea/`, `nbproject/`)
- 기존 빌드 산출물 정리 — `mvn clean` 또는 `target/`, `build/`, `out/`, `dist/` 수동 삭제
- `configplatform.xml` 의 절대경로(`C:\01_project\...`, `/usr/share/tomcat/...`) 는 배포 환경에 맞게 별도 조정

### 3.3 처음부터 맨손으로 만드는 경우 (대안)

기존 프로젝트를 복제하는 방식을 권장하지만, 맨손으로 구성한다면 최소한 아래 파일들이 필요합니다.

```
MyApp/
├── pom.xml                              # Maven WAR 빌드
├── src/com/
│   ├── <group>/                         # 애플리케이션 코드
│   │   ├── controller/                  # 컨트롤러 패키지 (@ControllerClassInfo 스캔 대상)
│   │   └── dao/
│   └── ithows/base/                     # 프레임워크 코어 (DispatcherServlet 등)
├── web/
│   ├── WEB-INF/
│   │   ├── web.xml                      # DispatcherServlet, SecurityFilter 매핑
│   │   ├── dispatcher-servlet.xml       # 템플릿 정의
│   │   ├── classes/
│   │   │   ├── configplatform.xml       # 앱 설정
│   │   │   ├── connpool.xml             # DB 커넥션 풀
│   │   │   └── log4j.properties
│   │   └── jsp/                         # JSP 뷰
│   └── index.jsp
└── lib/                                 # 의존 JAR (Ant 빌드 시)
```

---

## 4. 기본 설정

### 4.1 DB 연결 (`web/WEB-INF/classes/connpool.xml`)

프로젝트의 DB 호스트, 스키마, 계정 정보를 지정합니다. `JdbcDao` 가 이 파일을 읽어 커넥션 풀을 초기화합니다.

```xml
<properties>
    <entry key="url">jdbc:mariadb://localhost:3306/myapp?useUnicode=true&amp;characterEncoding=UTF-8</entry>
    <entry key="username">myapp</entry>
    <entry key="password">secret</entry>
    <entry key="driver">org.mariadb.jdbc.Driver</entry>
    <entry key="maxActive">100</entry>
    <entry key="maxIdle">30</entry>
    <entry key="minIdle">10</entry>
</properties>
```

> **두 번째 DB**가 있으면 `JdbcDao2` 가 같은 폴더의 `connpool2.xml` (또는 비슷한 이름) 을 읽도록 되어 있습니다. 기존 `JdbcDao2.java` 코드에서 실제 파일명을 확인하세요.

### 4.2 앱 설정 (`web/WEB-INF/classes/configplatform.xml`)

애플리케이션에서 사용하는 공통 설정값을 등록합니다.

```xml
<properties>
    <entry key="site_domain">MyApp Service</entry>
    <entry key="context_path">MyApp/</entry>
    <entry key="temp_dir">temp/</entry>
    <entry key="config_devlog_path">devlogs/</entry>
    <entry key="config_errorlog_path">errorlogs/</entry>

    <!-- OS 별 컨텍스트 루트 — AppConfig.getContextPath() 가 OS 에 따라 자동 분기.
         파이썬 연동 등 컨텍스트 루트 기준 상대경로를 쓰는 곳에서 사용됨. -->
    <entry key="context_win_dir">C:\03_work\MyApp\build\web\</entry>
    <entry key="context_dir">/locationService/tomcat/tomcatweb/webapps/MyApp/</entry>

    <!-- @ApiKeyRequired 검증에 사용되는 공통 API Key -->
    <entry key="common_api_key">여러분의_API_키</entry>
</properties>
```

값은 코드에서 `AppConfig.getConf("context_path")` 로 읽습니다. OS 별 컨텍스트 루트는 `AppConfig.getContextPath()` 한 메소드로 가져옵니다 (Windows → `context_win_dir`, 그 외 → `context_dir`).

### 4.3 서블릿 등록 (`web/WEB-INF/web.xml`)

최소 다음 구성이 필요합니다.

```xml
<!-- 1. 앱 설정 로더 (가장 먼저) -->
<servlet>
    <servlet-name>AppConfig</servlet-name>
    <servlet-class>com.ithows.AppConfig</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>

<!-- 2. DispatcherServlet - 모든 *.do 요청 처리 -->
<servlet>
    <servlet-name>DispatcherServlet</servlet-name>
    <servlet-class>com.ithows.base.DispatcherServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>DispatcherServlet</servlet-name>
    <url-pattern>*.do</url-pattern>
</servlet-mapping>

<!-- 3. UTF-8 인코딩 필터 -->
<filter>
    <filter-name>Character Encoding</filter-name>
    <filter-class>com.ithows.CharacterEncodingFilter</filter-class>
    <init-param>
        <param-name>encoding</param-name>
        <param-value>UTF-8</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>Character Encoding</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>

<!-- 4. 보안 필터 (XSS, CSRF, Rate Limit, HTTP 헤더) -->
<filter>
    <filter-name>SecurityFilter</filter-name>
    <filter-class>com.ithows.base.SecurityFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>SecurityFilter</filter-name>
    <url-pattern>*.do</url-pattern>
</filter-mapping>

<!-- 5. Swagger UI (선택) -->
<servlet>
    <servlet-name>SwaggerServlet</servlet-name>
    <servlet-class>com.ithows.base.SwaggerServlet</servlet-class>
    <load-on-startup>2</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>SwaggerServlet</servlet-name>
    <url-pattern>/docs/*</url-pattern>
</servlet-mapping>

<session-config>
    <session-timeout>180</session-timeout>
</session-config>
```

---

## 5. TutorialController — 패턴 카탈로그

이 프로젝트에는 SSF 의 핵심 패턴을 한 클래스에 모아 둔 [TutorialController](../src/com/ithows/controller/TutorialController.java) 가 이미 작성되어 있습니다. 아래 Step 1 ~ 8 의 모든 패턴이 그 안에 동작하는 코드로 들어 있으니, **새로 무언가를 만들기 전에 매핑되는 엔드포인트 코드를 먼저 열어보는 것**을 권장합니다.

### 5.1 전제 테이블

```sql
CREATE TABLE tutorial_item (
    itemId      VARCHAR(32)   PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    category    VARCHAR(50),
    price       INT           DEFAULT 0,
    active      TINYINT       DEFAULT 1,
    createTime  DATETIME      DEFAULT CURRENT_TIMESTAMP
);
```

### 5.2 엔드포인트 목록

| # | URL | Method | 패턴 | 다루는 Step |
|---|-----|--------|------|------------|
| 1 | `/tutorial/ping.do`              | GET  | 헬스체크 / `RESULT_PAGE_JSON`              | Step 1 |
| 2 | `/tutorial/echo.do`              | GET  | `HttpUtil.getParameterXxx` 타입별 파라미터 추출 | Step 3 |
| 3 | `/tutorial/getItem.do`           | GET  | 단건 조회 — `JdbcDao.queryForMap`           | Step 2 |
| 4 | `/tutorial/getList.do`           | GET  | 목록 조회 — `resultList` 응답               | Step 2 / 4 |
| 5 | `/tutorial/searchItems.do`       | GET  | 검색 + 페이지네이션 (total / pageNo / pageSize) | Step 3 |
| 6 | `/tutorial/createItem.do`        | POST | JSON body 로 INSERT                         | Step 3 |
| 7 | `/tutorial/updateItem.do`        | POST | JSON body 로 UPDATE                         | Step 3 |
| 8 | `/tutorial/deleteItem.do`        | POST | soft delete (`active=0`) + GET/POST 동시 지원 | Step 3 |
| 9 | `/tutorial/createItemsBatch.do`  | POST | 트랜잭션 — `insertItemsAtomic` 일괄 삽입     | Step 2 |
| 10 | `/tutorial/exportItems.do`      | GET  | DB → 임시 JSON 파일 → `sendBinaryFileToClient` | Step 4 |
| 11 | `/tutorial/uploadFile.do`       | POST | multipart/form-data 단일 파일 업로드 + 안전 파일명 | — |
| 12 | `/tutorial/downloadFile.do`     | GET  | 업로드된 파일 다운로드 + path traversal 방어    | — |
| 13 | `/tutorial/callPython.do`       | POST | 파이썬 스크립트 대리 호출 (`PythonCallUtil`)   | Step 8 |
| 13b | `/tutorial/pythonInfo.do`      | GET  | 파이썬 환경 진단 (`getDiagnosticInfo`)        | Step 8 |
| 14 | `/tutorial/secureApi.do`        | GET  | `@ApiKeyRequired` — `X-API-Key` 헤더 인증     | Step 5 |
| 15 | `/tutorial/adminPing.do`        | GET  | 로그인 + `requiredSecurityLevel = 1`          | Step 5 |

### 5.3 활용 가이드

- **Step 1 ~ 8 학습 중에 막히면** — 위 표에서 같은 패턴을 가진 엔드포인트를 찾아 [TutorialController.java](../src/com/ithows/controller/TutorialController.java) 의 해당 메서드를 참조하세요. 모든 메서드에 의도/주의사항 주석이 달려 있습니다.
- **자체 컨트롤러를 만들 때** — TutorialController 의 가까운 메서드를 복사해 시작하면 어노테이션·반환값·에러 처리 패턴이 자연스럽게 따라옵니다.
- **Swagger 문서 확인** — 이 모든 엔드포인트는 `@ApiInfo` 가 붙어 있어, 서버 실행 후 [/docs/](http://localhost:8088/SSF2026/docs/) 에서 인터랙티브하게 호출해볼 수 있습니다 (Step 6 참조).

---

## Step 1 — 첫 컨트롤러 만들기 (Hello World)

### 1.1 컨트롤러 클래스

`src/com/ithows/controller/HelloController.java` 를 새로 만듭니다.

```java
package com.ithows.controller;

import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class HelloController {

    @ControllerMethodInfo(id = "/api/hello.do")
    @ApiInfo(summary = "Hello World", tag = "Sample", method = "GET")
    public String hello(HttpSession session, HttpServletRequest request,
                        HttpServletResponse response, Object command) {

        request.setAttribute("result", "OK");
        request.setAttribute("msg",    "Hello, SSF!");
        request.setAttribute("data",   "Hello World");

        return "RESULT_PAGE_JSON";
    }
}
```

### 1.2 동작 원리

1. 앱 시작 시 [DispatcherServlet.init()](../src/com/ithows/base/DispatcherServlet.java) 이 `com.ithows.controller` 패키지를 스캔
2. `@ControllerClassInfo` 가 붙은 클래스와 `@ControllerMethodInfo` 메서드를 찾아 URL(`id`) → 메서드 매핑을 `PageBeanContainer` 에 등록
3. 요청이 들어오면 URL 로 PageBean 을 조회해 리플렉션으로 해당 메서드를 호출
4. 메서드가 반환한 문자열로 JSP 를 forward

### 1.3 표준 메서드 시그니처

모든 컨트롤러 메서드는 다음 4개 파라미터를 받아야 합니다.

```java
public String methodName(
    HttpSession session,
    HttpServletRequest request,
    HttpServletResponse response,
    Object command            // Command 바인딩 미사용 시에도 선언 필수
)
```

### 1.4 반환값 의미

| 반환 문자열 | 의미 | 실제 JSP |
|-------------|------|----------|
| `"/any/path.jsp"` | 임의 JSP 경로 | `/WEB-INF/jsp/any/path.jsp` |
| `"RESULT_COMMON_JSON"` | 공통 JSON 응답 | `commonResultJson.jsp` |
| `"RESULT_PAGE_JSON"` | 단순 JSON 응답 | `simpleResultJson.jsp` |
| `"RESULT_LIST_JSON"` | 리스트 JSON | `commonListJson.jsp` |
| `"RESULT_RAW_JSON"` | 이스케이프 없는 JSON | `commonResultRawJson.jsp` |
| `"NO_PAGE"` | JSP forward 없음 | (응답을 직접 작성한 경우) |
| `"redirect:/xxx.do"` | 리다이렉트 | — |

### 1.5 호출 테스트

서버를 실행하고 (Step 7 참조) 아래 URL을 호출합니다.

```bash
curl http://localhost:8088/MyApp/api/hello.do
```

예상 응답:
```json
{
    "result": "OK",
    "msg":    "Hello, SSF!",
    "data":   "Hello World"
}
```

---

## Step 2 — DAO 레이어 작성

### 2.1 테이블 예시

```sql
CREATE TABLE product (
    productId   VARCHAR(32)  PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    price       INT          NOT NULL,
    active      TINYINT      DEFAULT 1,
    createTime  DATETIME     DEFAULT CURRENT_TIMESTAMP
);
```

### 2.2 DAO 클래스

`src/com/ithows/dao/ProductDAO.java`:

```java
package com.ithows.dao;

import com.ithows.JdbcDao;
import com.ithows.ResultMap;
import java.sql.SQLException;
import java.util.List;

public class ProductDAO {

    // SELECT 목록
    public static List<ResultMap> getActiveProducts() {
        String sql = "SELECT productId, name, price, createTime " +
                     "FROM product WHERE active = ? ORDER BY createTime DESC";
        try {
            return JdbcDao.queryForMapList(sql, new Object[]{1});
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // SELECT 단건
    public static ResultMap getProduct(String productId) {
        String sql = "SELECT * FROM product WHERE productId = ?";
        try {
            return JdbcDao.queryForMap(sql, new Object[]{productId});
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // INSERT
    public static int insertProduct(String productId, String name, int price) {
        String sql = "INSERT INTO product (productId, name, price) VALUES (?, ?, ?)";
        try {
            return JdbcDao.update(sql, new Object[]{productId, name, price});
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // UPDATE
    public static int updatePrice(String productId, int newPrice) {
        String sql = "UPDATE product SET price = ? WHERE productId = ?";
        try {
            return JdbcDao.update(sql, new Object[]{newPrice, productId});
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // DELETE (soft delete)
    public static int deactivate(String productId) {
        String sql = "UPDATE product SET active = 0 WHERE productId = ?";
        try {
            return JdbcDao.update(sql, new Object[]{productId});
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // COUNT
    public static int count() {
        try {
            return JdbcDao.queryForInt("SELECT COUNT(*) FROM product WHERE active = 1", new Object[]{});
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
```

### 2.3 규칙 — 반드시 지킬 것

1. **파라미터화 쿼리만 사용** — 문자열 연결로 SQL 을 만들지 말 것
   ```java
   // 금지
   String sql = "SELECT * FROM product WHERE name = '" + name + "'";

   // OK
   String sql = "SELECT * FROM product WHERE name = ?";
   JdbcDao.queryForMap(sql, new Object[]{name});
   ```

2. **DAO 는 정적 메서드(static) 로 작성** — 프레임워크 관행
3. **예외는 DAO 에서 삼키고(print + return null/0), 컨트롤러에서 결과값 null 여부로 판단**

### 2.4 ResultMap — 타입 안전 조회

```java
ResultMap p = ProductDAO.getProduct("P001");
if (p != null) {
    String name   = p.getString("name");
    int    price  = p.getInt("price", 0);       // 기본값 지원
    long   ts     = p.getLong("createTime");
}
```

### 2.5 트랜잭션

여러 DAO 호출을 원자적으로 묶어야 할 때:

```java
try {
    JdbcDao.beginTransaction();

    ProductDAO.insertProduct(id, name, price);
    InventoryDAO.initStock(id, 0);
    HistoryDAO.logCreation(id);

    JdbcDao.commit();
} catch (Exception e) {
    JdbcDao.rollback();
    throw e;
}
```

---

## Step 3 — 요청 파라미터 처리

### 3.1 GET 쿼리 파라미터

```java
String q    = HttpUtil.getParameterString(request, "q", "");
int    page = HttpUtil.getParameterInt(request,    "page", 1);
int    size = HttpUtil.getParameterInt(request,    "size", 20);
```

`HttpUtil.getParameterXxx(request, key, default)` 의 두 번째/세 번째 인자로 키와 기본값을 줍니다. 기본값이 있으니 누락되어도 NPE 가 발생하지 않습니다.

### 3.2 POST JSON Body

```java
JSONObject jParam;
try {
    jParam = HttpUtil.getBodyJson(request);
} catch (Exception e) {
    jParam = null;
}

if (jParam == null) {
    request.setAttribute("result", "ERROR");
    request.setAttribute("msg",    "Invalid JSON body");
    return "RESULT_COMMON_JSON";
}

String name  = jParam.getString("name");
int    price = jParam.getInt("price");
```

### 3.3 GET / POST 동시 지원

```java
JSONObject jParam = null;

if ("get".equalsIgnoreCase(request.getMethod())) {
    String raw = HttpUtil.getParameterString(request, "param", "");
    if (!raw.isEmpty()) {
        try { jParam = new JSONObject(raw); } catch (Exception e) { jParam = null; }
    }
} else {
    try { jParam = HttpUtil.getBodyJson(request); } catch (Exception e) { jParam = null; }
}
```

### 3.4 Command 객체 (선택)

매번 같은 파라미터 세트를 받는 API 가 있다면 Command 클래스를 만들어 자동 바인딩할 수 있습니다. `@ControllerMethodInfo(commandClass = "...", commandName = "...")` 로 지정하면 `CommandManager` 가 Request → Command 로 변환해 `request.setAttribute(commandName, obj)` 로 넘겨줍니다. (간단한 API 에서는 `HttpUtil` 직접 사용 권장)

---

## Step 4 — 응답 형식과 JSP 뷰

### 4.1 응답 Attribute 규약

| 키 | 타입 | 용도 |
|----|------|------|
| `result` | String | 결과 코드 — `OK` / `NO` / `ERROR` / `ERR` |
| `msg` | String | 사람이 읽는 메시지 |
| `restime` | String | 응답 시각 (`DateTimeUtils.getTimeDateNow()`) |
| `resultMap` | `ResultMap` | 단건 데이터 |
| `resultList` | `List<ResultMap>` | 목록 데이터 |
| `data` | String/임의 | 단순 데이터 (`RESULT_PAGE_JSON` 전용) |

### 4.2 `RESULT_COMMON_JSON` — 표준 JSON 응답

```java
request.setAttribute("result",     "OK");
request.setAttribute("msg",        "Success");
request.setAttribute("restime",    DateTimeUtils.getTimeDateNow());
request.setAttribute("resultList", list);
return "RESULT_COMMON_JSON";
```

응답 구조:
```json
{
    "result":     "OK",
    "msg":        "Success",
    "restime":    "2026-04-21 10:30:00",
    "resultMap":  {...},
    "count":      10,
    "resultList": [{...}, ...]
}
```

### 4.3 에러 처리 패턴 (권장 템플릿)

```java
try {
    // 파라미터 검증
    if (jParam == null) {
        request.setAttribute("result", "ERROR");
        request.setAttribute("msg",    "No parameter");
        return "RESULT_COMMON_JSON";
    }

    // 비즈니스 로직
    List<ResultMap> list = ProductDAO.getActiveProducts();

    // 결과 검증
    if (list == null || list.isEmpty()) {
        request.setAttribute("result", "NO");
        request.setAttribute("msg",    "No data");
        return "RESULT_COMMON_JSON";
    }

    // 성공 응답
    request.setAttribute("result",     "OK");
    request.setAttribute("resultList", list);

} catch (Exception e) {
    e.printStackTrace();
    request.setAttribute("result", "ERROR");
    request.setAttribute("msg",    "Server error");
}

return "RESULT_COMMON_JSON";
```

### 4.4 커스텀 JSP 뷰

HTML 페이지를 렌더링하려면 JSP 를 `/WEB-INF/jsp/` 하위에 만들고 경로를 반환합니다.

```java
@ControllerMethodInfo(id = "/product/list.do")
public String productList(HttpSession s, HttpServletRequest req, HttpServletResponse res, Object c) {
    req.setAttribute("resultList", ProductDAO.getActiveProducts());
    return "/product/list.jsp";   // → /WEB-INF/jsp/product/list.jsp
}
```

---

## Step 5 — 보안 적용 (로그인, API Key)

### 5.1 세션 로그인 강제

```java
@ControllerMethodInfo(
    id = "/admin/dashboard.do",
    loginRequired = true
)
public String dashboard(...) { ... }
```

미로그인 시 `DispatcherServlet` 이 자동으로 `/login.do` 로 리다이렉트 (컨트롤러는 호출되지 않음).

### 5.2 보안 레벨

```java
@ControllerMethodInfo(
    id = "/admin/manageUsers.do",
    loginRequired = true,
    requiredSecurityLevel = 3   // Admin 전용
)
```

| 레벨 | 설명 |
|:----:|------|
| 0 | 모두 |
| 1 | General 이상 |
| 2 | Super 이상 |
| 3 | Admin 전용 |

레벨 미달 시 `/accessDenied.do` 로 리다이렉트.

### 5.3 API Key 인증 (`@ApiKeyRequired`)

외부에서 호출하는 공개 API 에 사용합니다. 로그인/세션과 **독립적**으로 동작.

```java
import com.ithows.base.ApiKeyRequired;

@ControllerMethodInfo(id = "/api/getData.do")
@ApiKeyRequired
public String getData(HttpSession s, HttpServletRequest req, HttpServletResponse res, Object c) {
    // X-API-Key 헤더가 유효할 때만 여기 도달
    ...
    return "RESULT_COMMON_JSON";
}
```

**설정**: `configplatform.xml` 에 키 등록
```xml
<entry key="common_api_key">발급할_API_KEY</entry>
```

**호출**:
```bash
curl -H "X-API-Key: 발급할_API_KEY" \
     http://localhost:8088/MyApp/api/getData.do
```

키 누락/불일치 시 `{"result":"ERROR","msg":"Missing API Key"}` 또는 `"Invalid API Key"` 응답이 자동으로 반환되고 `ACCESS_DENIED` 감사 로그가 남습니다.

### 5.4 실행 순서

`DispatcherServlet` 이 요청을 라우팅할 때:

```
1. @ApiKeyRequired 체크 (로그인과 독립, 먼저 실행)
2. loginRequired / requiredSecurityLevel 체크
3. 컨트롤러 메서드 실행
```

---

## Step 6 — API 문서화 (Swagger)

### 6.1 `@ApiInfo` 추가

```java
@ControllerMethodInfo(id = "/api/createProduct.do")
@ApiKeyRequired
@ApiInfo(
    summary     = "상품 등록",
    description = "새 상품을 등록한다.",
    tag         = "Product API",
    method      = "POST",
    parameters  = {
        @ApiInfo.Param(name = "productId", type = "string", required = true, description = "상품 ID"),
        @ApiInfo.Param(name = "name",      type = "string", required = true, description = "상품명"),
        @ApiInfo.Param(name = "price",     type = "number", required = true, description = "가격", example = "19900")
    },
    responseDescription = "등록 결과"
)
public String createProduct(...) { ... }
```

### 6.2 확인

서버 실행 후:
- Swagger UI: `http://localhost:8088/MyApp/docs/`
- OpenAPI JSON: `http://localhost:8088/MyApp/docs/api-docs`

`@ApiInfo` 가 없어도 `@ControllerMethodInfo` 만으로 기본 문서가 자동 생성되지만, 파라미터/설명은 직접 기입해야 사용자 친화적인 문서가 됩니다.

---

## Step 7 — 빌드 및 실행

### 7.1 WAR 빌드 (외부 Tomcat)

```bash
mvn clean package
# → target/MyApp-1.0-SNAPSHOT.war
```

생성된 WAR 를 Tomcat `webapps/` 에 복사하면 `http://호스트:포트/MyApp-1.0-SNAPSHOT/...` 로 접근 가능.

### 7.2 Embedded Tomcat 실행 (개발/단독 실행)

별도 Tomcat 설치 없이 실행:

```powershell
# Windows
.\embedded-build.bat        # 빌드
.\embedded-run.bat          # 실행
```

```bash
# Linux/macOS
./embedded-build.sh
./embedded-run.sh
```

기본 포트 `8088`, 기본 컨텍스트 `/MyApp`. 옵션은 [Embedded_Tomcat_Guide.md](Embedded_Tomcat_Guide.md) 참고.

### 7.3 로컬 확인 체크

실행 후 아래 엔드포인트로 정상 동작을 확인합니다.

```bash
curl http://localhost:8088/MyApp/api/checkHealth.do
curl http://localhost:8088/MyApp/api/checkDB.do
```

DB 연결까지 성공하면 `{"status":"OK","db1":"OK","db2":"OK"}` 응답이 나옵니다.

---

## Step 8 — 파이썬 스크립트 연동

자바로 구현하기 번거로운 작업(수치 계산·텍스트 분석·ML 추론·외부 CLI 도구 호출 등)을 파이썬에 위임하고 결과만 받아오고 싶을 때 사용하는 패턴입니다.

### 8.1 설계 원칙

**파일 기반 IPC** — stdin/stdout 대신 요청/응답 JSON 파일을 주고받습니다.

- **이유 1.** 대용량 데이터도 안전 (OS 버퍼 한계·stream deadlock 회피)
- **이유 2.** 바이너리/멀티라인 데이터 전달에도 안전
- **이유 3.** 파이썬 쪽에서 `print` 로 디버깅 로그를 찍어도 응답 포맷이 깨지지 않음

호출 흐름:

```
[Java]  임시 요청 JSON 파일 작성  (py_req_{guid}.json)
   │
   ▼
[Python]  python script.py --request <req.json> --response <res.json>
   │    · 요청 파일 읽기
   │    · 로직 처리
   │    · 응답 파일 쓰기
   ▼
[Java]  응답 JSON 파일 읽기 → 파싱
   │
   ▼
  임시 파일 자동 삭제 (try/finally)
```

### 8.2 Java 쪽 — [PythonCallUtil](../src/com/ithows/util/PythonCallUtil.java)

```java
import com.ithows.util.PythonCallUtil;
import org.json.JSONObject;

JSONObject request = new JSONObject();
request.put("text", "분석할 문장");
request.put("topN", 5);

JSONObject response = PythonCallUtil.callPython(
    "tutorial_text_stats.py",   // python_script_dir 아래의 파일명
    request,
    30                           // 타임아웃(초) — 초과 시 프로세스 강제 종료
);

if ("OK".equals(response.optString("result"))) {
    // 성공 처리
} else {
    // 실패 - response.getString("msg") 에 원인
}
```

**반환 규약:**
- 파이썬이 정상 종료 + 응답 파일을 남기면 → 그 JSON 을 그대로 반환
- 실패(스크립트 없음 / 타임아웃 / 응답 파일 없음 / 잘못된 JSON) → `{"result":"ERROR", "msg":"<원인>"}` 반환
- **항상 non-null** 이므로 호출부는 `result` 필드로 성공/실패를 판단

### 8.3 Python 쪽 스크립트 규약

모든 파이썬 스크립트는 다음 두 인자를 처리해야 합니다:

```bash
python <script>.py --request <req.json> --response <res.json>
```

**최소 템플릿** ([../python_process/tutorial_echo.py](../python_process/tutorial_echo.py) 참고):

```python
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import argparse, json, sys, traceback

def process(req: dict) -> dict:
    # 실제 로직
    return {"result": "OK", "msg": "done", "echo": req}

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--request",  required=True)
    parser.add_argument("--response", required=True)
    args = parser.parse_args()

    with open(args.request, encoding="utf-8") as f:
        req = json.load(f)

    res = process(req)

    with open(args.response, "w", encoding="utf-8") as f:
        json.dump(res, f, ensure_ascii=False, indent=2)

if __name__ == "__main__":
    # 에러가 나도 Java 쪽이 읽을 수 있도록 응답 파일에 기록
    response_path = None
    if "--response" in sys.argv:
        try:
            response_path = sys.argv[sys.argv.index("--response") + 1]
        except Exception:
            pass

    try:
        main()
    except Exception as e:
        err = {"result": "ERROR", "msg": str(e), "trace": traceback.format_exc()}
        if response_path:
            try:
                with open(response_path, "w", encoding="utf-8") as f:
                    json.dump(err, f, ensure_ascii=False, indent=2)
            except Exception:
                pass
        sys.exit(1)
```

**핵심 규칙:**
- 인코딩은 반드시 **UTF-8** (한글 처리)
- 예외가 나도 **응답 파일에 ERROR JSON 을 기록**해야 Java 쪽 진단이 가능
- stdout 으로 `print` 하는 디버그 로그는 무시됨 (Java 쪽은 응답 파일만 읽음)
- 파이썬 인터프리터는 **서버에 미리 설치되어 있어야 함**, 스크립트는 **수동으로 `python_process/` 폴더에 복사** (Java 쪽에 업로드 기능은 제공하지 않음 — 코드 실행 권한 상승 위험)

### 8.4 컨트롤러에서 API로 노출

```java
@ControllerMethodInfo(id = "/api/analyzeText.do")
@ApiInfo(summary = "텍스트 분석", tag = "Analytics", method = "POST")
public String analyzeText(HttpSession session, HttpServletRequest request,
                          HttpServletResponse response, Object command) {

    JSONObject body;
    try { body = HttpUtil.getBodyJson(request); } catch (Exception e) { body = null; }

    if (body == null) {
        request.setAttribute("result", "ERROR");
        request.setAttribute("msg",    "Invalid JSON body");
        return "RESULT_COMMON_JSON";
    }

    JSONObject pyResult = PythonCallUtil.callPython("tutorial_text_stats.py", body, 30);

    // 방법 A. 파이썬 응답을 그대로 클라이언트에 전달 (RESULT_RAW_JSON 사용)
    request.setAttribute("result",    pyResult.optString("result", "ERROR"));
    request.setAttribute("msg",       pyResult.optString("msg",    ""));
    request.setAttribute("resultMap", pyResult.toString());
    return "RESULT_RAW_JSON";

    // 방법 B. 파이썬 응답에서 필요한 값만 추려 ResultMap 에 담기
    // ResultMap map = new ResultMap();
    // map.put("wordCount", pyResult.getJSONObject("counts").getInt("words"));
    // request.setAttribute("resultMap", map);
    // return "RESULT_COMMON_JSON";
}
```

`RESULT_RAW_JSON` 은 [commonResultRawJson.jsp](../web/WEB-INF/jsp/commonResultRawJson.jsp) 로 forward 되어 `resultMap` 문자열을 JSON 그대로 출력합니다 — 파이썬이 돌려준 중첩 JSON 구조를 그대로 전달할 때 편리합니다.

### 8.5 환경 설정 — [configplatform.xml](../web/WEB-INF/classes/configplatform.xml)

```xml
<!-- 파이썬 실행 명령 (OS 무관 단일 키) -->
<entry key="python_command">C:\Python310\python.exe</entry>

<!-- 디렉토리는 OS 별 컨텍스트 루트(context_win_dir / context_dir) 하위의 "상대 경로" 로 지정.
     실제 절대 경로 = AppConfig.getContextPath() + 아래 값 -->
<entry key="python_script_dir">python_process/</entry>
<entry key="python_temp_dir">python_process/temp/</entry>

<!-- python --version 프로브 타임아웃(초). 미설정 시 5초. -->
<entry key="python_version_timeout_sec">5</entry>
```

| 키 | 기본값 | 설명 |
|----|--------|------|
| `python_command` | `python` | 파이썬 실행 명령. PATH에 없으면 **절대경로** 권장 |
| `python_script_dir` | `$user.dir/python_process` | 스크립트 폴더. 상대경로면 `AppConfig.getContextPath()` 와 결합 |
| `python_temp_dir` | `java.io.tmpdir` | 요청/응답 임시 파일 폴더. 상대경로면 컨텍스트 루트와 결합 |
| `python_version_timeout_sec` | `5` | `pythonInfo.do` 가 `python --version` 을 호출할 때의 타임아웃 |

### 8.6 OS 별 경로 자동 분기

`python_script_dir` / `python_temp_dir` 의 값이 **상대 경로**이면 [PythonCallUtil](../src/com/ithows/util/PythonCallUtil.java) 가 [AppConfig.getContextPath()](../src/com/ithows/AppConfig.java) 와 결합해 절대 경로로 만듭니다. `getContextPath()` 는 OS 에 따라 자동 분기됩니다:

- Windows → `context_win_dir`
- Linux → `context_dir`

따라서 같은 `configplatform.xml` 한 벌로 양 OS 에 배포해도 동작합니다:

```xml
<!-- 컨텍스트 루트 (이미 존재하는 키) -->
<entry key="context_win_dir">C:\03_work\SSF2026\build\web\</entry>
<entry key="context_dir">/locationService/tomcat/tomcatweb/webapps/SSF2026/</entry>

<!-- 파이썬은 상대 경로로 한 번만 -->
<entry key="python_script_dir">python_process/</entry>
<entry key="python_temp_dir">python_process/temp/</entry>
```

| OS | 결합 결과 (예: `python_script_dir`) |
|----|-------------------------------------|
| Windows | `C:\03_work\SSF2026\build\web\python_process\` |
| Linux   | `/locationService/tomcat/tomcatweb/webapps/SSF2026/python_process/` |

**절대 경로로 override 하고 싶을 때**는 그대로 절대 경로를 넣어주면 컨텍스트 루트와 결합하지 않고 그 값이 그대로 사용됩니다.

> 💡 외부 Tomcat 의 `user.dir` 은 보통 `{CATALINA_HOME}\bin` 이지만, 위 방식은 컨텍스트 루트(`AppConfig.getContextPath()`) 기준으로 해석하므로 영향을 받지 않습니다. `user.dir` 은 컨텍스트 루트와 설정값이 **둘 다 비어 있을 때만** 마지막 fallback 으로 쓰입니다.

### 8.7 진단 — 동작이 이상할 때

[TutorialController](../src/com/ithows/controller/TutorialController.java)에 설정 진단용 엔드포인트가 있습니다.

```
GET /tutorial/pythonInfo.do
```

응답:
```json
{
    "python_command":    "C:\\Python310\\python.exe",
    "python_script_dir": "C:\\03_work\\SSF2026\\build\\web\\python_process",
    "python_temp_dir":   "C:\\03_work\\SSF2026\\build\\web\\python_process\\temp",
    "context_path":      "C:\\03_work\\SSF2026\\build\\web\\",
    "user.dir":          "C:\\Tomcat9\\bin",
    "os.name":           "Windows 11",
    "script_dir_exists": true,
    "scripts":           ["tutorial_echo.py", "tutorial_text_stats.py"],
    "python_version":    "Python 3.13.2"
}
```

체크 포인트:
- `context_path` 가 비어있거나 잘못됨 → `context_win_dir` / `context_dir` 키 누락 또는 오타
- `script_dir_exists: false` → 컨텍스트 루트 + `python_script_dir` 결합 결과에 폴더가 없음
- `scripts: []` → 폴더는 존재하지만 `.py` 파일이 없음 (수동 복사 누락)
- `python_version` 에 `(failed to run ...)` → `python_command` 경로 오류
- `python_version` 에 `(timed out after Ns)` → 파이썬 콜드스타트가 느린 환경 → `python_version_timeout_sec` 값을 늘릴 것

> ⚠️ **운영 배포 시** `/tutorial/pythonInfo.do` 는 서버 내부 경로를 노출하므로 **제거하거나 `loginRequired=true` + `requiredSecurityLevel=3` (Admin 전용)** 으로 보호하세요.

### 8.8 새 파이썬 스크립트 추가 절차

**Java 재빌드 불필요** — 매 요청마다 파이썬을 새로 실행하므로 스크립트만 교체하면 즉시 반영됩니다.

1. 스크립트 작성 (§8.3 템플릿 사용)
2. `python_script_dir` 로 지정된 폴더에 `.py` 파일 **수동 복사**
3. 필요한 파이썬 패키지가 있으면 서버의 파이썬에 `pip install` 로 미리 설치
4. 같은 `/api/callPython.do` (또는 만들어 둔 래퍼 API) 에서 `script` 파라미터를 새 파일명으로 지정해 호출

### 8.9 보안 체크리스트

- [ ] `PythonCallUtil` 은 `FilenameUtils.getName()` 으로 basename만 추출해 path traversal 을 막지만, **사용자 입력을 `script` 파라미터로 그대로 넘기지 말 것** — 화이트리스트 검증 권장
- [ ] 파이썬 스크립트에 `os.system()` 등 **셸 호출이 있으면 주입 공격 가능** — 입력 검증을 파이썬 쪽에서도 수행
- [ ] 임시 파일은 `python_temp_dir` 의 용량을 차지함 → 정상 종료 시 자동 삭제되지만 JVM crash 시 남을 수 있으므로 주기적 정리 필요
- [ ] 타임아웃은 반드시 지정 — 무한 루프 스크립트가 자원을 점유하지 않게
- [ ] 운영에서는 파이썬 실행 전용 계정으로 Tomcat 을 실행해 권한을 최소화

---

## 트러블슈팅

| 증상 | 원인 / 대응 |
|------|-------------|
| `404 Not Found` on `*.do` | `web.xml` 의 `DispatcherServlet` 매핑(`*.do`) 확인, `@ControllerClassInfo` 어노테이션 누락 여부 확인 |
| 컨트롤러 메서드가 호출되지 않음 | 컨트롤러 패키지가 `com.ithows.controller` 아래에 있는지 확인 (DispatcherServlet 이 이 패키지를 스캔) |
| `NullPointerException` — `AppConfig.getConf()` | `AppConfig` 서블릿이 먼저 로드되었는지 확인 (`<load-on-startup>1</load-on-startup>`) |
| DB 연결 실패 | `connpool.xml` 의 url/username/password, MariaDB 드라이버 JAR 존재 여부 확인 |
| JSP `${...}` 가 그대로 출력됨 | `<%@ page isELIgnored="false" %>` 또는 JSTL 선언 (`<%@ taglib prefix="c" uri="..." %>`) 누락 |
| `@ApiKeyRequired` 가 동작하지 않음 | `configplatform.xml` 의 `common_api_key` 엔트리 등록 여부, 헤더명(`X-API-Key`) 정확히 입력했는지 |
| 프로젝트 이름 변경 후 빌드 오류 | `mvn clean` 실행, IDE 재로드, `target/`, `build/`, `out/`, `dist/` 삭제 |
| 한글이 `?` 로 깨짐 | `CharacterEncodingFilter` 매핑(`/*`) 확인, DB 연결 URL 에 `useUnicode=true&characterEncoding=UTF-8` |
| Swagger UI 빈 화면 | `SwaggerServlet` 매핑(`/docs/*`) 확인, 브라우저에서 `/docs/api-docs?refresh=true` 로 스펙 재생성 |
| `PythonCallUtil` 이 "script not found" 에러 | `context_win_dir` / `context_dir` 가 정확히 설정되어 있는지 + `python_script_dir` 의 상대 경로가 그 하위에 실제 존재하는지 확인. `/tutorial/pythonInfo.do` 의 `context_path` / `script_dir_exists` 로 진단 (§8.6 ~ §8.7) |
| 파이썬 호출 시 `(failed to run 'python' ...)` | `python` 이 PATH 에 없음. `python_command` 에 절대경로 (예: `C:\Python310\python.exe`) 지정 |
| 파이썬 진단이 `(timed out after 5s)` 로 실패 | `python --version` 이 5초 안에 끝나지 않음. `configplatform.xml` 의 `python_version_timeout_sec` 값을 늘릴 것 |
| 소스의 `configplatform.xml` 수정이 Tomcat 에 반영 안 됨 | Tomcat 은 `webapps/<app>/WEB-INF/classes/configplatform.xml` 을 읽음. 배포된 파일을 직접 수정하거나 WAR 재배포 후 webapp 재시작 |

---

## 체크리스트

새 프로젝트를 시작했다면 아래 항목을 순서대로 확인하세요.

- [ ] `rename-project.ps1` 로 프로젝트 이름 변경 완료
- [ ] `connpool.xml` 의 DB 접속 정보 수정
- [ ] `configplatform.xml` 의 `context_path`, `common_api_key`, 절대경로 수정
- [ ] `/api/checkHealth.do`, `/api/checkDB.do` 호출 시 `OK` 응답 확인
- [ ] 첫 컨트롤러(`HelloController`) 정상 동작 확인
- [ ] DB 스키마 생성 및 DAO 1개 작성·테스트
- [ ] `@ApiKeyRequired` 로 보호할 엔드포인트와 세션 로그인으로 보호할 엔드포인트 구분 정리
- [ ] Swagger UI(`/docs/`) 에서 문서가 렌더링되는지 확인
- [ ] `mvn clean package` 로 WAR 빌드 성공
- [ ] (선택) Embedded Tomcat 으로 단독 실행 성공
- [ ] (선택) 파이썬 연동 사용 시 — `context_win_dir` / `context_dir` 가 정확히 설정되어 있고, `python_script_dir` 이 그 하위 상대 경로에 존재. `/tutorial/pythonInfo.do` 로 `script_dir_exists: true`, `context_path` 정상 확인

---

## 다음 단계

- 프레임워크 내부 구조가 궁금하다면 — [../SSF_Framework_RAG_Document.md](../SSF_Framework_RAG_Document.md)
- Spring 과의 차이점 — [../SSF_vs_Spring_Analysis.md](../SSF_vs_Spring_Analysis.md)
- Embedded Tomcat 상세 옵션 — [Embedded_Tomcat_Guide.md](Embedded_Tomcat_Guide.md)
