# 서블릿 웹 애플리케이션을 Embedded Tomcat으로 빌드하기

## 개요

기존에 외부 WAS(Tomcat 등)에 WAR로 배포하던 서블릿 웹 애플리케이션을 **Embedded Tomcat** 방식으로 변환하여, 별도의 WAS 설치 없이 `java -cp` 명령 하나로 실행할 수 있도록 하는 과정을 정리합니다.

이 문서는 OmniFLService 프로젝트를 Embedded Tomcat으로 전환하면서 실제로 겪은 오류와 해결 과정을 바탕으로 작성되었습니다.

---

## 목차

1. [전환 전략](#1-전환-전략)
2. [필요한 파일 구성](#2-필요한-파일-구성)
3. [Step 1: Maven POM 작성](#step-1-maven-pom-작성-pom-embeddedxml)
4. [Step 2: Application 메인 클래스 작성](#step-2-application-메인-클래스-작성)
5. [Step 3: 빌드 스크립트 작성](#step-3-빌드-스크립트-작성)
6. [Step 4: 실행 스크립트 작성](#step-4-실행-스크립트-작성)
7. [전환 시 반드시 확인해야 할 체크리스트](#전환-시-반드시-확인해야-할-체크리스트)
8. [실제 발생한 오류와 해결 과정](#실제-발생한-오류와-해결-과정)
9. [빌드 및 실행 명령어 요약](#빌드-및-실행-명령어-요약)

---

## 1. 전환 전략

기존 코드를 전혀 수정하지 않고, **별도의 POM 파일**과 **메인 클래스**만 추가하여 두 가지 빌드를 공존시킵니다.

```
기존 WAR 빌드:     mvn package              → OmniFLService.war      (외부 Tomcat에 배포)
Embedded 빌드:     mvn -f pom-embedded.xml   → OmniFLService-embedded.war (단독 실행)
```

| 항목 | WAR 배포 (기존) | Embedded Tomcat |
|------|----------------|-----------------|
| WAS 필요 여부 | Tomcat 별도 설치 필요 | 불필요 (내장) |
| 빌드 결과물 | `OmniFLService.war` | `OmniFLService-embedded.war` |
| 실행 방법 | WAS에 배포 | 스크립트 또는 `java -cp` |
| 빌드 파일 | `pom.xml` | `pom-embedded.xml` |
| 기존 코드 수정 | - | 없음 |

---

## 2. 필요한 파일 구성

기존 프로젝트에 다음 파일들만 추가합니다:

```
프로젝트 루트/
├── pom-embedded.xml                         # Embedded 전용 Maven POM
├── src/com/ithows/EmbeddedApplication.java  # Embedded Tomcat 메인 클래스
├── embedded-build.bat (.sh)                 # 빌드 스크립트
└── embedded-run.bat (.sh)                   # 실행 스크립트
```

---

## Step 1: Maven POM 작성 (pom-embedded.xml)

기존 `pom.xml`을 수정하지 않고, 별도의 `pom-embedded.xml`을 생성합니다.

### 1-1. Embedded Tomcat 의존성 추가

```xml
<properties>
    <tomcat.version>9.0.98</tomcat.version>
</properties>

<dependencies>
    <!-- Embedded Tomcat 코어 -->
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-core</artifactId>
        <version>${tomcat.version}</version>
    </dependency>

    <!-- JSP 컴파일 지원 -->
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-jasper</artifactId>
        <version>${tomcat.version}</version>
    </dependency>

    <!-- WebSocket 지원 -->
    <dependency>
        <groupId>org.apache.tomcat.embed</groupId>
        <artifactId>tomcat-embed-websocket</artifactId>
        <version>${tomcat.version}</version>
    </dependency>
</dependencies>
```

### 1-2. Tomcat JDBC/DBCP (코드에서 사용하는 경우)

기존 WAR 배포에서 `provided` scope였던 Tomcat 라이브러리는 **compile scope**로 변경합니다:

```xml
<!-- 기존 pom.xml에서는 provided scope였으나, embedded에서는 compile -->
<dependency>
    <groupId>org.apache.tomcat</groupId>
    <artifactId>tomcat-jdbc</artifactId>
    <version>${tomcat.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.tomcat</groupId>
    <artifactId>tomcat-dbcp</artifactId>
    <version>${tomcat.version}</version>
</dependency>
```

### 1-3. Servlet/JSP API는 반드시 provided scope

> **핵심 주의사항**: Servlet API JAR이 WAR에 포함되면 Tomcat 내장 API와 충돌합니다.

```xml
<!-- Tomcat 9 = Servlet 4.0 내장. 반드시 provided로 설정 -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>javax.servlet.jsp</groupId>
    <artifactId>javax.servlet.jsp-api</artifactId>
    <version>2.3.3</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
    <scope>provided</scope>
</dependency>
```

### 1-4. Maven Central에 없는 로컬 JAR 처리

Maven Central에 없는 JAR은 `system` scope로 선언합니다:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-local-lib</artifactId>
    <version>1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/my-local-lib.jar</systemPath>
</dependency>
```

**system scope JAR은 WAR에 자동 포함되지 않습니다.** `maven-war-plugin`의 `webResources`로 명시적 복사가 필요합니다:

```xml
<plugin>
    <artifactId>maven-war-plugin</artifactId>
    <configuration>
        <webResources>
            <resource>
                <directory>lib</directory>
                <targetPath>WEB-INF/lib</targetPath>
                <includes>
                    <include>*.jar</include>
                </includes>
            </resource>
        </webResources>
    </configuration>
</plugin>
```

### 1-5. 정적 리소스 패키징 설정

> **핵심 주의사항**: `packagingIncludes` (화이트리스트) 대신 `packagingExcludes` (블랙리스트)를 사용합니다.

```xml
<!-- BAD: 화이트리스트 - 명시하지 않은 확장자(ttf, woff, eot, svg, zip, json 등)가 누락됨 -->
<packagingIncludes>
    **/*.jsp, **/*.html, **/*.css, **/*.js, **/*.png, **/*.jpg
</packagingIncludes>

<!-- GOOD: 블랙리스트 - 불필요한 파일만 제외, 나머지 모두 포함 -->
<packagingExcludes>
    **/thumbs.db,
    **/.DS_Store
</packagingExcludes>
```

---

## Step 2: Application 메인 클래스 작성

```java
package com.ithows;

import java.io.File;
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

public class EmbeddedApplication {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(
            System.getProperty("server.port", "8080"));
        String contextPath = System.getProperty(
            "server.contextPath", "/MyApp");
        String webappBase = System.getProperty("webapp.base");

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();  // Tomcat 9에서 필수

        // 개발 모드 판별
        boolean devMode = "web".equals(webappBase)
            && new File("web/WEB-INF/web.xml").exists();

        // 웹앱 디렉토리 결정
        String webappDir;
        if (webappBase != null && new File(webappBase).exists()) {
            webappDir = new File(webappBase).getAbsolutePath();
        } else {
            webappDir = new File("web").getAbsolutePath();
        }

        Context ctx = tomcat.addWebapp(contextPath, webappDir);

        // 개발 모드에서만 추가 리소스 등록
        if (devMode) {
            File classesDir = new File("target/classes");
            if (classesDir.exists()) {
                WebResourceRoot resources = new StandardRoot(ctx);
                resources.addPreResources(new DirResourceSet(
                    resources, "/WEB-INF/classes",
                    classesDir.getAbsolutePath(), "/"));
                ctx.setResources(resources);
            }
        }

        tomcat.start();
        tomcat.getServer().await();
    }
}
```

### 개발 모드 vs 프로덕션 모드

| 항목 | 개발 모드 (`-Dwebapp.base=web`) | 프로덕션 모드 (추출된 WAR) |
|------|-------------------------------|--------------------------|
| 웹앱 디렉토리 | 소스의 `web/` 디렉토리 | 추출된 WAR 디렉토리 |
| classes | `target/classes` 를 추가 등록 | WAR 내 `WEB-INF/classes` 사용 |
| lib | 프로젝트 `lib/` 디렉토리 추가 | WAR 내 `WEB-INF/lib` 사용 |
| JSP 수정 | 서버 재시작 없이 반영 | WAR 재추출 필요 |

> **핵심 주의사항**: 프로덕션 모드에서 `target/classes`나 `lib/`를 추가 리소스로 등록하면, `Class.getResource("/")`가 `target/classes/`를 반환하게 되어 설정 파일 경로 해석이 깨집니다. **반드시 개발 모드에서만** 추가 리소스를 등록해야 합니다.

---

## Step 3: 빌드 스크립트 작성

### WAR 구조에서 `java -jar`가 불가능한 이유

WAR 파일은 `WEB-INF/classes/`와 `WEB-INF/lib/*.jar`에 코드가 위치하지만, `java -jar`는 이 경로를 클래스패스로 인식하지 않습니다. 따라서 **WAR를 추출한 뒤 직접 클래스패스를 구성**하는 방식을 사용합니다.

```
빌드:  mvn -f pom-embedded.xml package → target/OmniFLService-embedded.war

실행:  WAR 추출 → WEB-INF/classes + WEB-INF/lib/*.jar 로 클래스패스 구성 → java -cp 실행
```

### 빌드 스크립트 (embedded-build.bat)

```bat
@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set WAR_FILE=target\OmniFLService-embedded.war
set EXTRACT_DIR=target\embedded-webapp

REM 이전 추출 디렉토리 제거 (서버 실행 중이면 파일 잠김 방지)
if exist "%EXTRACT_DIR%" rmdir /s /q "%EXTRACT_DIR%" 2>nul

call mvn -f pom-embedded.xml clean package -DskipTests
```

> **주의**: `clean` 전에 추출 디렉토리를 먼저 삭제해야 합니다. 서버 실행 후 추출된 JAR 파일이 프로세스에 의해 잠겨 있으면 Maven clean이 실패합니다.

---

## Step 4: 실행 스크립트 작성

### 실행 스크립트 (embedded-run.bat)

핵심 로직은 다음과 같습니다:

```bat
REM 1) WAR 추출
mkdir "%EXTRACT_DIR%"
pushd "%EXTRACT_DIR%"
jar xf "..\OmniFLService-embedded.war"
popd

REM 2) 클래스패스 구성
set CP=%EXTRACT_DIR%\WEB-INF\classes
for %%f in (%EXTRACT_DIR%\WEB-INF\lib\*.jar) do set "CP=!CP!;%%f"

REM 3) 실행 (-Dwebapp.base를 추출 디렉토리로 지정)
java -Dserver.port=8080 ^
     -Dserver.contextPath=/OmniFLService ^
     -Dwebapp.base=%EXTRACT_DIR% ^
     -Dfile.encoding=UTF-8 ^
     -cp "%CP%" ^
     com.ithows.EmbeddedApplication
```

---

## 전환 시 반드시 확인해야 할 체크리스트

### POM 설정

- [ ] **Embedded Tomcat 의존성 추가** (`tomcat-embed-core`, `tomcat-embed-jasper`, `tomcat-embed-websocket`)
- [ ] **Servlet/JSP API는 반드시 `provided` scope** — WAR에 포함되면 Tomcat 내장 API와 충돌하여 `NoSuchMethodError` 발생
- [ ] **Servlet API 버전을 Tomcat에 맞게 변경** — Tomcat 9 = Servlet 4.0 (`javax.servlet-api:4.0.1`), Tomcat 10+ = Jakarta Servlet 5.0+
- [ ] **기존 `provided` scope였던 Tomcat 라이브러리를 `compile` scope로 변경** — `tomcat-jdbc`, `tomcat-dbcp` 등 코드에서 직접 사용하는 라이브러리
- [ ] **Maven Central에 없는 JAR은 `system` scope + `webResources`로 WAR에 포함** — system scope JAR은 자동으로 WAR에 들어가지 않음
- [ ] **`packagingExcludes` (블랙리스트) 사용** — `packagingIncludes`를 쓰면 폰트(ttf, woff), 데이터(zip, json) 등이 누락됨

### Application 메인 클래스

- [ ] **`tomcat.getConnector()` 호출** — Tomcat 9에서 기본 커넥터가 자동 생성되지 않으므로 명시적 호출 필수
- [ ] **개발/프로덕션 모드 분리** — 프로덕션에서 `target/classes`를 추가 리소스로 등록하면 `getResource("/")` 경로가 꼬여서 설정 파일을 못 찾음
- [ ] **web.xml은 기존 것 그대로 사용** — `tomcat.addWebapp()`이 자동으로 web.xml을 읽어 서블릿/필터 등록

### 빌드 스크립트

- [ ] **Windows batch 파일에 한글 주석 금지** — cmd.exe는 UTF-8을 CP949로 해석하여 파싱 오류 발생
- [ ] **빌드 전 추출 디렉토리 삭제** — 이전 실행에서 JAR 파일이 잠겨있으면 Maven clean 실패
- [ ] **`java -jar` 대신 WAR 추출 + `java -cp` 방식 사용** — WAR 구조에서 `java -jar`는 `WEB-INF/classes`를 클래스패스로 인식하지 못함

### 실행 스크립트

- [ ] **`-Dfile.encoding=UTF-8` 지정** — Windows에서 한글 깨짐 방지
- [ ] **`-Dwebapp.base`를 추출 디렉토리로 지정** — 메인 클래스가 웹앱 위치를 정확히 인식
- [ ] **서버 종료 후 빌드** — 실행 중 빌드하면 파일 잠김으로 clean 실패

---

## 실제 발생한 오류와 해결 과정

### 오류 1: Maven Central에서 라이브러리를 찾을 수 없음

```
Could not find artifact com.kitfox.svg:svg-salamander:jar:1.1.1 in central
```

**원인**: 기존 `pom.xml`에서 일반 의존성으로 선언했으나, 실제로는 Maven Central에 없는 로컬 JAR.

**해결**: `system` scope로 변경하여 `lib/` 디렉토리의 JAR을 직접 참조.

```xml
<dependency>
    <groupId>com.kitfox.svg</groupId>
    <artifactId>svg-salamander</artifactId>
    <version>1.1.1</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/svgSalamander-v1.1.1.jar</systemPath>
</dependency>
```

---

### 오류 2: Tomcat JDBC 패키지를 찾을 수 없음

```
package org.apache.tomcat.jdbc.pool does not exist
```

**원인**: 기존 `pom.xml`에서 `tomcat-jdbc`가 `provided` scope(외부 Tomcat이 제공)였는데, embedded POM에서도 그대로 복사.

**해결**: `compile` scope로 변경 (embedded 환경에서는 Tomcat이 내장이므로 직접 포함해야 함).

---

### 오류 3: 컴파일 오류 — 예외 처리 누락

```
unreported exception java.net.MalformedURLException; must be caught or declared to be thrown
```

**원인**: `EmbeddedApplication` 메인 클래스에서 `URL.toURL()` 호출 시 checked exception 미처리.

**해결**: 메서드 시그니처에 `throws Exception` 추가.

---

### 오류 4: Windows batch 파일 인코딩 오류

```
'뚮뱶'은(는) 내부 또는 외부 명령... 이 아닙니다
```

**원인**: batch 파일이 UTF-8로 저장되었으나, cmd.exe는 시스템 기본 인코딩(CP949)으로 해석하여 한글 주석이 명령어로 파싱됨.

**해결**: batch 파일에서 한글 주석을 모두 영문으로 교체.

---

### 오류 5: ClassNotFoundException

```
java.lang.ClassNotFoundException: com.ithows.EmbeddedApplication
```

**원인**: `java -jar file.war`로 실행했으나, WAR 구조에서 `java -jar`의 클래스로더는 `WEB-INF/classes/`를 탐색하지 않음. MANIFEST.MF에 Main-Class를 설정해도 실제 클래스 로딩 불가.

**해결**: `java -jar` 방식을 포기하고, WAR를 추출한 뒤 직접 클래스패스를 구성하는 방식으로 변경.

```bat
REM WAR 추출
jar xf OmniFLService-embedded.war

REM 클래스패스 직접 구성
set CP=WEB-INF\classes
for %%f in (WEB-INF\lib\*.jar) do set "CP=!CP!;%%f"

REM 실행
java -cp "%CP%" com.ithows.EmbeddedApplication
```

---

### 오류 6: HTTP 500 — NoSuchMethodError (Servlet API 충돌)

```
java.lang.NoSuchMethodError:
  'javax.servlet.http.HttpServletMapping
   javax.servlet.http.HttpServletRequest.getHttpServletMapping()'
```

**원인**: `javax.servlet-api:3.1.0` (Servlet 3.1)이 WAR의 `WEB-INF/lib/`에 포함되어 Tomcat 9 내장 Servlet 4.0 API보다 먼저 로딩됨. `getHttpServletMapping()`은 Servlet 4.0에서 추가된 메서드.

**해결**: Servlet/JSP API를 `provided` scope로 변경하여 WAR에서 제외.

```xml
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>    <!-- 이것이 핵심 -->
</dependency>
```

---

### 오류 7: HTTP 500 — 설정 파일 경로 오류 (getResource 문제)

```
java.io.FileNotFoundException:
  C:\project\target\classes (액세스가 거부되었습니다)
```

**원인**: `EmbeddedApplication`이 프로덕션 모드에서도 `target/classes`를 `addPreResources()`로 등록. 이로 인해 `AppConfig.class.getResource("/")`가 추출된 WAR의 `WEB-INF/classes/` 대신 `target/classes/`를 반환하여 `configplatform.xml` 경로 해석 실패.

**해결**: 개발 모드(`-Dwebapp.base=web`)에서만 추가 리소스 등록. 프로덕션 모드에서는 추출된 WAR에 이미 모든 리소스가 포함되어 있으므로 추가 등록 불필요.

```java
boolean devMode = "web".equals(webappBase);

if (devMode) {
    // target/classes, lib/ 등록
}
// else: 추출된 WAR의 WEB-INF/classes, WEB-INF/lib 그대로 사용
```

---

### 오류 8: 폰트/아이콘 등 정적 리소스 누락

```
아이콘 폰트 로딩 실패, resource/ 디렉토리 파일 미포함
```

**원인**: `maven-war-plugin`의 `packagingIncludes`가 화이트리스트 방식으로 `*.jsp, *.html, *.css, *.js, *.png, *.jpg, *.gif, *.ico`만 지정. `*.ttf, *.woff, *.eot, *.svg, *.zip, *.json` 등의 확장자가 모두 제외됨.

**해결**: `packagingIncludes`(화이트리스트)를 `packagingExcludes`(블랙리스트)로 변경.

```xml
<!-- 불필요한 파일만 제외, 나머지 모두 포함 -->
<packagingExcludes>
    **/thumbs.db,
    **/.DS_Store
</packagingExcludes>
```

---

### 오류 9: Maven clean 실패 — 파일 잠김

```
Failed to clean project: Failed to delete target\embedded-webapp\WEB-INF\lib\zip4j-2.11.5.jar
```

**원인**: Embedded Tomcat 서버가 실행 중인 상태에서 빌드를 시도. 추출된 WAR의 JAR 파일이 Java 프로세스에 의해 잠겨 있어 삭제 불가.

**해결**: 빌드 스크립트에서 Maven clean 전에 추출 디렉토리를 먼저 삭제하도록 변경. 서버 실행 중이면 종료 후 빌드.

```bat
if exist "%EXTRACT_DIR%" rmdir /s /q "%EXTRACT_DIR%" 2>nul
call mvn -f pom-embedded.xml clean package
```

---

## 빌드 및 실행 명령어 요약

### 빌드

```bash
# Windows
embedded-build.bat

# Linux/Mac
./embedded-build.sh

# Maven 직접
mvn -f pom-embedded.xml clean package -DskipTests
```

### 실행

```bash
# Windows
embedded-run.bat
embedded-run.bat --port 9090
embedded-run.bat --port 9090 --context /myapp

# Linux/Mac
./embedded-run.sh
./embedded-run.sh --port 9090

# 개발 모드 (소스 기반, JSP 즉시 반영)
embedded-build.bat dev
```

### JVM 메모리 설정

```bash
# Windows
set JAVA_OPTS=-Xms512m -Xmx2048m
embedded-run.bat

# Linux/Mac
JAVA_OPTS="-Xms512m -Xmx2048m" ./embedded-run.sh
```

---

## Tomcat 버전과 Servlet API 대응표

| Tomcat 버전 | Servlet API | JSP API | Java 최소 | 네임스페이스 |
|------------|-------------|---------|----------|------------|
| 8.5.x | 3.1 | 2.3 | Java 7 | `javax.servlet` |
| **9.0.x** | **4.0** | **2.3** | **Java 8** | **`javax.servlet`** |
| 10.0.x | 5.0 | 3.0 | Java 8 | `jakarta.servlet` |
| 10.1.x | 6.0 | 3.1 | Java 11 | `jakarta.servlet` |
| 11.0.x | 6.1 | 4.0 | Java 17 | `jakarta.servlet` |

> 기존 `javax.servlet` 기반 코드는 **Tomcat 9.x**가 최대 호환 버전입니다.
> Tomcat 10 이상은 `jakarta.servlet`으로 패키지명이 변경되어 기존 코드 수정이 필요합니다.
