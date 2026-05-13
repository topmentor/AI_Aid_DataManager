# Claude Code 기반 데이터 분석·시각화 MVP 설계서

## 1. 문서 목적

본 문서는 Claude Code가 생성한 쿼리 계획과 분석 코드를 이용하여, 다양한 데이터 소스에서 필요한 데이터를 추출하고, 조합·가공·시각화·파일화를 수행하는 MVP 시스템의 설계를 정의한다.

첫 번째 MVP에서는 데이터 소스를 다음 3가지로 제한한다.

```txt
1. MariaDB
2. JSON 파일
3. CSV 파일
```

본 MVP의 목표는 다음과 같다.

```txt
사용자 자연어 요청
  ↓
데이터 소스 구조 확인
  ↓
Claude Code가 Query Plan 생성
  ↓
서버가 Query Plan 검증
  ↓
MariaDB / JSON / CSV에서 데이터 추출
  ↓
Claude Code가 분석·시각화 Python 코드 생성
  ↓
샌드박스 Runner에서 코드 실행
  ↓
표, 차트, CSV, JSON, HTML 파일 생성
  ↓
사용자가 결과 확인 및 다운로드
```

---

## 2. MVP 핵심 개념

이 시스템은 단순한 코드 실행기가 아니다. 핵심은 다음 4개 계층을 분리하는 것이다.

| 계층 | 역할 |
|---|---|
| Data Source Layer | MariaDB, JSON, CSV 데이터 접근 |
| Planning Layer | 사용자 요청을 Query Plan으로 변환 |
| Execution Layer | 데이터 추출 및 분석 코드 실행 |
| Result Layer | 표, 차트, 파일, 리포트 제공 |



---

## 3. MVP 범위

### 3.1 포함 범위

| 구분 | 포함 내용 |
|---|---|
| 데이터 소스 | MariaDB, JSON, CSV |
| 사용자 입력 | 자연어 분석 요청 |
| 데이터 구조 탐색 | 테이블/컬럼/파일 스키마 조회 |
| 계획 생성 | Query Plan JSON 생성 |
| 계획 검증 | 읽기 전용, LIMIT, 허용 테이블/컬럼 검사 |
| 데이터 추출 | SQL SELECT, JSON Path, CSV 컬럼 선택 |
| 분석 코드 생성 | Python 기반 pandas/matplotlib 코드 생성 |
| 실행 | 샌드박스 Runner 실행 |
| 출력 | JSON, CSV, PNG, HTML |
| UI | 작업 요청, 실행 상태, 결과 목록, 다운로드 |
| 로그 | 실행 계획, 쿼리, 코드, 결과, 오류 로그 저장 |


단, 이후 확장 가능하도록 구조는 모듈화한다.

---

## 4. 전체 시스템 구조

```txt
[Web UI / Tomcat Servlet]
        ↓
[Analysis Orchestrator]
        ↓
[Data Source Catalog]
        ↓
[LLM Planner / Claude Code]
        ↓
[Query Plan Validator]
        ↓
[Data Extractor]
        ├─ MariaDB Extractor
        ├─ JSON Extractor
        └─ CSV Extractor
        ↓
[Dataset Store]
        ↓
[Code Generator / Claude Code]
        ↓
[Python Sandbox Runner]
        ↓
[Artifact Store]
        ├─ result.json
        ├─ result.csv
        ├─ chart.png
        └─ report.html
        ↓
[Result Viewer]
```

---

## 5. 주요 모듈 설계

## 5.1 Web UI

사용자가 자연어 분석 요청을 입력하고 결과를 확인하는 화면이다.

### 주요 기능

```txt
분석 요청 입력
데이터 소스 선택
실행 상태 확인
Query Plan 확인
생성 코드 확인
결과 테이블 보기
차트 보기
결과 파일 다운로드
오류 로그 확인
재실행
```

### 예시 요청

```txt
collectgrid 테이블에서 LTE RSRP가 -110보다 약한 격자를 찾아서 CSV와 막대 차트로 만들어 줘.
```

```txt
업로드한 CSV 파일에서 일자별 에러 건수를 집계해서 라인 차트로 보여 줘.
```

```txt
JSON 파일 안의 사용자 이벤트 로그를 이벤트 타입별로 집계해서 표와 차트로 만들어 줘.
```

---

## 5.2 Analysis Orchestrator

전체 작업 흐름을 관리하는 중심 모듈이다.

### 역할

```txt
Job 생성
사용자 요청 저장
데이터 소스 카탈로그 조회
Claude Code에 Query Plan 생성 요청
Query Plan 검증 요청
데이터 추출 실행
분석 코드 생성 요청
Runner 실행 요청
결과 Artifact 등록
실행 로그 저장
오류 처리
```

### 상태 흐름

```txt
CREATED
  ↓
PLANNING
  ↓
PLAN_VALIDATING
  ↓
EXTRACTING
  ↓
CODE_GENERATING
  ↓
RUNNING
  ↓
COMPLETED
```

오류 발생 시:

```txt
FAILED_PLANNING
FAILED_VALIDATION
FAILED_EXTRACTION
FAILED_CODE_GENERATION
FAILED_EXECUTION
```

---

## 5.3 Data Source Catalog

Claude Code가 데이터 구조를 이해할 수 있도록 데이터 소스의 메타데이터를 제공한다.

Claude Code에게는 실제 DB 접속 정보가 아니라, 안전하게 정리된 카탈로그만 제공한다.

### MariaDB Catalog 예시

```json
{
  "sourceId": "mariadb.collectdata",
  "type": "mariadb",
  "description": "Location collection database",
  "allowedOperations": ["select"],
  "schemas": [
    {
      "table": "collectgrid",
      "description": "Grid-level signal collection table",
      "columns": [
        { "name": "xId", "type": "int", "description": "Grid X index" },
        { "name": "yId", "type": "int", "description": "Grid Y index" },
        { "name": "minX", "type": "double", "description": "Minimum longitude" },
        { "name": "minY", "type": "double", "description": "Minimum latitude" },
        { "name": "maxX", "type": "double", "description": "Maximum longitude" },
        { "name": "maxY", "type": "double", "description": "Maximum latitude" },
        { "name": "lrsrp", "type": "varchar", "description": "LTE RSRP values" },
        { "name": "lrsrq", "type": "varchar", "description": "LTE RSRQ values" },
        { "name": "wmac", "type": "longtext", "description": "WiFi MAC CSV" },
        { "name": "blemac", "type": "longtext", "description": "BLE MAC CSV" }
      ]
    }
  ],
  "policies": {
    "readOnly": true,
    "requireLimit": true,
    "maxRows": 10000,
    "blockedColumns": [],
    "allowedSqlTypes": ["SELECT"]
  }
}
```

### CSV Catalog 예시

```json
{
  "sourceId": "csv.error_log_sample",
  "type": "csv",
  "fileId": "file_20260512_001",
  "description": "Uploaded error log CSV",
  "columns": [
    { "name": "timestamp", "type": "datetime" },
    { "name": "level", "type": "string" },
    { "name": "module", "type": "string" },
    { "name": "message", "type": "string" },
    { "name": "elapsed_ms", "type": "number" }
  ],
  "policies": {
    "maxRows": 100000,
    "readOnly": true
  }
}
```

### JSON Catalog 예시

```json
{
  "sourceId": "json.user_events",
  "type": "json",
  "fileId": "file_20260512_002",
  "description": "User event JSON dataset",
  "rootType": "array",
  "fields": [
    { "path": "$.eventTime", "type": "datetime" },
    { "path": "$.userId", "type": "string" },
    { "path": "$.eventType", "type": "string" },
    { "path": "$.payload.screen", "type": "string" }
  ],
  "policies": {
    "maxItems": 100000,
    "readOnly": true
  }
}
```

---

## 5.4 LLM Planner / Claude Code

사용자 요청과 Data Source Catalog를 입력받아 Query Plan JSON을 생성한다.

Claude Code는 다음 정보만 받는다.

```txt
사용자 자연어 요청
사용 가능한 데이터 소스 목록
각 데이터 소스의 스키마
허용된 연산
출력 가능 형식
제한 규칙
```

Claude Code는 다음 정보는 받지 않는다.

```txt
DB 접속 URL
DB 계정/비밀번호
서버 내부 경로
운영 파일 경로
환경변수
민감 설정값
```

---

## 5.5 Query Plan Validator

Claude Code가 생성한 Query Plan이 안전한지 검사한다.

### 검증 항목

| 검증 항목 | 설명 |
|---|---|
| 데이터 소스 검증 | 등록된 sourceId인지 확인 |
| 연산 검증 | SELECT, read_csv, read_json만 허용 |
| SQL 검증 | SELECT만 허용, DDL/DML 금지 |
| LIMIT 검증 | MariaDB 쿼리는 LIMIT 필수 |
| 컬럼 검증 | 허용된 컬럼만 조회 가능 |
| 테이블 검증 | 허용된 테이블만 조회 가능 |
| 행 수 제한 | maxRows 초과 금지 |
| 파일 접근 검증 | 등록된 fileId만 접근 가능 |
| 출력 형식 검증 | 허용된 output type만 생성 가능 |

### 금지 SQL 키워드

```txt
INSERT
UPDATE
DELETE
DROP
ALTER
TRUNCATE
CREATE
GRANT
REVOKE
LOAD DATA
INTO OUTFILE
CALL
EXECUTE
```

---

## 5.6 Data Extractor

검증된 Query Plan에 따라 데이터를 추출하고 Dataset Store에 저장한다.

### MariaDB Extractor

역할:

```txt
검증된 SELECT 쿼리 실행
PreparedStatement 사용
LIMIT 강제
결과를 CSV 또는 JSONL로 저장
실행 시간 측정
조회 행 수 기록
```

출력 예:

```txt
jobs/{jobId}/datasets/q1.csv
```

### CSV Extractor

역할:

```txt
등록된 CSV 파일 읽기
필요 컬럼 선택
필터 조건 적용
행 수 제한 적용
중간 CSV 저장
```

### JSON Extractor

역할:

```txt
등록된 JSON 파일 읽기
JSON Path 기반 필드 추출
배열 데이터 flatten
필터 조건 적용
중간 CSV 또는 JSONL 저장
```

---

## 5.7 Dataset Store

데이터 추출 결과를 저장하는 작업 단위 저장소이다.

### 디렉터리 구조

```txt
jobs/
  job_20260512_001/
    request.json
    plan.json
    validated-plan.json
    datasets/
      q1.csv
      q2.csv
      q3.jsonl
    code/
      main.py
    outputs/
      result.json
      result.csv
      chart.png
      report.html
    logs/
      extract.log
      runner.log
      error.log
```

---

## 5.8 Code Generator / Claude Code

데이터 추출이 완료되면, Claude Code는 중간 데이터셋을 대상으로 분석·시각화 코드를 생성한다.

### 입력 정보

```txt
사용자 요청
Query Plan
중간 데이터셋 목록
각 데이터셋의 컬럼 정보
샘플 데이터 10~30행
허용 라이브러리
출력 요구사항
코딩 규칙
```

### 허용 라이브러리 MVP

```txt
pandas
matplotlib
json
csv
pathlib
datetime
math
statistics
```

### 제한 라이브러리

```txt
os
subprocess
socket
requests
urllib
pymysql
sqlalchemy
psycopg2
paramiko
shutil
```

필요에 따라 `os`는 완전 금지하거나, 내부적으로 제한된 wrapper만 허용한다.

---

## 5.9 Python Sandbox Runner

Claude Code가 생성한 Python 코드를 격리 환경에서 실행한다.

### 실행 원칙

```txt
입력 디렉터리만 읽기 허용
출력 디렉터리만 쓰기 허용
네트워크 차단
DB 접속 차단
실행 시간 제한
메모리 제한
stdout/stderr 로그 저장
```

### Docker 실행 예시

```bash
docker run --rm \
  --network none \
  --memory 512m \
  --cpus 1.0 \
  -v /app/jobs/job_20260512_001/datasets:/workspace/inputs:ro \
  -v /app/jobs/job_20260512_001/outputs:/workspace/outputs:rw \
  -v /app/jobs/job_20260512_001/code:/workspace/code:ro \
  Claude Code-python-runner:0.1 \
  python /workspace/code/main.py
```

---

## 5.10 Artifact Store

분석 결과물을 저장하고 UI에서 접근할 수 있게 한다.

### 지원 파일 형식 MVP

| 형식 | 용도 |
|---|---|
| result.json | 실행 결과 메타데이터 |
| result.csv | 표 데이터 |
| chart.png | 차트 이미지 |
| report.html | HTML 리포트 |
| dataset.csv | 중간 데이터 다운로드 |

### result.json 예시

```json
{
  "jobId": "job_20260512_001",
  "status": "COMPLETED",
  "title": "Weak LTE Signal Analysis",
  "summary": "Found 324 weak signal grid cells.",
  "artifacts": [
    {
      "type": "table",
      "name": "Weak Signal Grid Table",
      "file": "result.csv"
    },
    {
      "type": "image",
      "name": "RSRP Distribution Chart",
      "file": "chart.png"
    },
    {
      "type": "html",
      "name": "Analysis Report",
      "file": "report.html"
    }
  ]
}
```

---

# 6. Query Plan JSON 스키마

## 6.1 기본 구조

```json
{
  "version": "1.0",
  "taskType": "data_analysis",
  "title": "Analysis title",
  "description": "Purpose of this analysis",
  "dataSources": [],
  "extracts": [],
  "transforms": [],
  "visualizations": [],
  "outputs": []
}
```

---

## 6.2 MariaDB Extract 예시

```json
{
  "extractId": "q1",
  "sourceId": "mariadb.collectdata",
  "type": "mariadb_select",
  "purpose": "Find weak LTE signal grid cells",
  "sql": "SELECT xId, yId, minX, minY, maxX, maxY, lrsrp FROM collectgrid WHERE lrsrp IS NOT NULL AND CAST(lrsrp AS SIGNED) < ? LIMIT ?",
  "params": [-110, 10000],
  "output": {
    "format": "csv",
    "file": "q1.csv"
  }
}
```

---

## 6.3 CSV Extract 예시

```json
{
  "extractId": "q2",
  "sourceId": "csv.error_log_sample",
  "type": "csv_extract",
  "purpose": "Extract error rows",
  "columns": ["timestamp", "level", "module", "message", "elapsed_ms"],
  "filters": [
    {
      "column": "level",
      "op": "in",
      "value": ["ERROR", "WARN"]
    }
  ],
  "limit": 50000,
  "output": {
    "format": "csv",
    "file": "q2.csv"
  }
}
```

---

## 6.4 JSON Extract 예시

```json
{
  "extractId": "q3",
  "sourceId": "json.user_events",
  "type": "json_extract",
  "purpose": "Extract user event type records",
  "rootPath": "$[*]",
  "fields": [
    { "name": "eventTime", "path": "$.eventTime" },
    { "name": "userId", "path": "$.userId" },
    { "name": "eventType", "path": "$.eventType" },
    { "name": "screen", "path": "$.payload.screen" }
  ],
  "filters": [
    {
      "field": "eventType",
      "op": "exists"
    }
  ],
  "limit": 50000,
  "output": {
    "format": "csv",
    "file": "q3.csv"
  }
}
```

---

## 6.5 Visualization 정의 예시

```json
{
  "visualizations": [
    {
      "vizId": "chart1",
      "type": "bar_chart",
      "input": "q1.csv",
      "title": "Weak Signal Count by RSRP Range",
      "x": "rsrp_range",
      "y": "count",
      "output": "chart.png"
    }
  ]
}
```

---

## 6.6 Output 정의 예시

```json
{
  "outputs": [
    {
      "type": "csv",
      "name": "Result Table",
      "file": "result.csv"
    },
    {
      "type": "image",
      "name": "Chart",
      "file": "chart.png"
    },
    {
      "type": "html",
      "name": "HTML Report",
      "file": "report.html"
    }
  ]
}
```

---

# 7. 생성 Python 코드 규약

Claude Code가 생성하는 Python 코드는 다음 규약을 따라야 한다.

## 7.1 고정 디렉터리

```python
from pathlib import Path

INPUT_DIR = Path("inputs")
OUTPUT_DIR = Path("outputs")
OUTPUT_DIR.mkdir(exist_ok=True)
```

## 7.2 입력 규칙

```txt
inputs/ 디렉터리 아래 파일만 읽는다.
DB에 직접 접속하지 않는다.
외부 URL을 호출하지 않는다.
환경변수를 읽지 않는다.
시스템 명령을 실행하지 않는다.
```

## 7.3 출력 규칙

```txt
outputs/result.json은 반드시 생성한다.
표 결과가 있으면 outputs/result.csv를 생성한다.
차트가 있으면 outputs/chart.png를 생성한다.
리포트가 있으면 outputs/report.html을 생성한다.
```

## 7.4 기본 코드 템플릿

```python
import json
from pathlib import Path
import pandas as pd
import matplotlib.pyplot as plt

INPUT_DIR = Path("inputs")
OUTPUT_DIR = Path("outputs")
OUTPUT_DIR.mkdir(exist_ok=True)

# Load datasets
df = pd.read_csv(INPUT_DIR / "q1.csv")

# Analysis
summary = df.describe(include="all")

# Save table
summary.to_csv(OUTPUT_DIR / "result.csv")

# Save chart
plt.figure(figsize=(10, 5))
# chart logic here
plt.tight_layout()
plt.savefig(OUTPUT_DIR / "chart.png")

# Save metadata
result = {
    "status": "success",
    "outputs": [
        "result.csv",
        "chart.png"
    ]
}

with open(OUTPUT_DIR / "result.json", "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False, indent=2)
```

---

# 8. 예시 시나리오

## 8.1 MariaDB 분석 예시

### 사용자 요청

```txt
collectgrid 테이블에서 LTE RSRP가 -110보다 약한 격자를 찾아서 상위 1000개를 CSV와 차트로 만들어 줘.
```

### 생성 Query Plan

```json
{
  "version": "1.0",
  "taskType": "data_analysis",
  "title": "Weak LTE Signal Grid Analysis",
  "extracts": [
    {
      "extractId": "q1",
      "sourceId": "mariadb.collectdata",
      "type": "mariadb_select",
      "sql": "SELECT xId, yId, minX, minY, maxX, maxY, lrsrp FROM collectgrid WHERE lrsrp IS NOT NULL AND CAST(lrsrp AS SIGNED) < ? LIMIT ?",
      "params": [-110, 1000],
      "output": {
        "format": "csv",
        "file": "q1.csv"
      }
    }
  ],
  "outputs": [
    { "type": "csv", "file": "result.csv" },
    { "type": "image", "file": "chart.png" },
    { "type": "html", "file": "report.html" }
  ]
}
```

### 결과

```txt
result.csv
chart.png
report.html
```

---

## 8.2 CSV 분석 예시

### 사용자 요청

```txt
업로드한 CSV에서 모듈별 ERROR 건수를 집계해서 막대 차트로 보여 줘.
```

### 처리 흐름

```txt
CSV Catalog 조회
  ↓
level = ERROR 필터
  ↓
module 기준 group by
  ↓
result.csv 생성
  ↓
chart.png 생성
  ↓
report.html 생성
```

---

## 8.3 JSON 분석 예시

### 사용자 요청

```txt
JSON 이벤트 로그에서 화면별 이벤트 수를 집계해서 표와 차트로 만들어 줘.
```

### 처리 흐름

```txt
JSON Path로 eventType, screen 추출
  ↓
screen 기준 group by
  ↓
event count 계산
  ↓
result.csv 생성
  ↓
chart.png 생성
```

---

# 9. API 설계

## 9.1 Job 생성

```http
POST /api/analysis/jobs
```

### Request

```json
{
  "title": "Weak LTE Signal Analysis",
  "requestText": "collectgrid 테이블에서 LTE RSRP가 -110보다 약한 격자를 찾아서 CSV와 차트로 만들어 줘.",
  "sourceIds": [
    "mariadb.collectdata"
  ]
}
```

### Response

```json
{
  "jobId": "job_20260512_001",
  "status": "CREATED"
}
```

---

## 9.2 Job 실행

```http
POST /api/analysis/jobs/{jobId}/run
```

### Response

```json
{
  "jobId": "job_20260512_001",
  "status": "PLANNING"
}
```

---

## 9.3 Job 상태 조회

```http
GET /api/analysis/jobs/{jobId}
```

### Response

```json
{
  "jobId": "job_20260512_001",
  "status": "COMPLETED",
  "title": "Weak LTE Signal Analysis",
  "createdAt": "2026-05-12T10:00:00+09:00",
  "completedAt": "2026-05-12T10:00:31+09:00"
}
```

---

## 9.4 Query Plan 조회

```http
GET /api/analysis/jobs/{jobId}/plan
```

---

## 9.5 생성 코드 조회

```http
GET /api/analysis/jobs/{jobId}/code
```

---

## 9.6 결과 Artifact 목록

```http
GET /api/analysis/jobs/{jobId}/artifacts
```

### Response

```json
{
  "jobId": "job_20260512_001",
  "artifacts": [
    {
      "artifactId": "a1",
      "type": "csv",
      "name": "Result CSV",
      "fileName": "result.csv",
      "downloadUrl": "/api/analysis/jobs/job_20260512_001/artifacts/a1/download"
    },
    {
      "artifactId": "a2",
      "type": "image",
      "name": "Chart",
      "fileName": "chart.png",
      "downloadUrl": "/api/analysis/jobs/job_20260512_001/artifacts/a2/download"
    }
  ]
}
```

---

## 9.7 Artifact 다운로드

```http
GET /api/analysis/jobs/{jobId}/artifacts/{artifactId}/download
```

---

# 10. Java 패키지 구조 예시

Tomcat Servlet 기반으로 시작할 경우 다음 구조를 추천한다.

```txt
src/main/java/com/company/analysis/
  servlet/
    AnalysisJobServlet.java
    AnalysisRunServlet.java
    AnalysisArtifactServlet.java

  orchestrator/
    AnalysisOrchestrator.java
    AnalysisJobService.java
    JobStatus.java

  catalog/
    DataSourceCatalogService.java
    DataSourceSchema.java
    MariaDbCatalogProvider.java
    CsvCatalogProvider.java
    JsonCatalogProvider.java

  planner/
    LlmPlannerClient.java
    QueryPlan.java
    QueryPlanParser.java

  validator/
    QueryPlanValidator.java
    SqlSafetyValidator.java
    FileAccessValidator.java

  extractor/
    DataExtractor.java
    MariaDbExtractor.java
    CsvExtractor.java
    JsonExtractor.java
    ExtractResult.java

  generator/
    AnalysisCodeGenerator.java
    PythonCodePromptBuilder.java

  runner/
    PythonRunnerClient.java
    DockerPythonRunner.java
    RunnerResult.java

  artifact/
    ArtifactService.java
    Artifact.java
    ArtifactType.java

  storage/
    JobStorageService.java
    FileJobStorageService.java

  audit/
    AuditLogService.java
```

---

# 11. 저장소 구조 예시

```txt
Claude Code-data-analysis-mvp/
  README.md
  pom.xml

  src/main/java/
    com/company/analysis/...

  src/main/webapp/
    WEB-INF/jsp/
      analysis/
        job-form.jsp
        job-detail.jsp
        result-view.jsp

  config/
    datasource-catalog.json
    runner-config.json
    policy.json

  jobs/
    .gitkeep

  runner/
    Dockerfile
    requirements.txt
    runner-entrypoint.sh

  samples/
    csv/error_log_sample.csv
    json/user_events.json
    plans/sample-plan.json
```

---

# 12. Runner Dockerfile 예시

```dockerfile
FROM python:3.11-slim

WORKDIR /workspace

RUN pip install --no-cache-dir \
    pandas \
    matplotlib

ENV MPLBACKEND=Agg

CMD ["python", "/workspace/code/main.py"]
```

---

# 13. Runner requirements.txt

```txt
pandas==2.2.2
matplotlib==3.9.0
```

MVP에서는 라이브러리를 최소화한다. 이후 필요 시 다음을 추가한다.

```txt
openpyxl
jinja2
numpy
```

---

# 14. 보안 정책

## 14.1 Query Plan 보안

```txt
MariaDB는 SELECT만 허용
LIMIT 필수
허용된 테이블/컬럼만 조회
차단 키워드 포함 시 실패
PreparedStatement 사용
최대 조회 행 수 제한
쿼리 실행 시간 제한
```

## 14.2 파일 보안

```txt
등록된 fileId만 접근
상대 경로 접근 금지
../ 경로 차단
업로드 디렉터리 외 접근 금지
파일 크기 제한
```

## 14.3 코드 실행 보안

```txt
Docker network none
메모리 제한
CPU 제한
실행 시간 제한
입력 read-only mount
출력 디렉터리만 writable
환경변수 최소화
DB 접속 정보 전달 금지
```

## 14.4 코드 정적 검사

생성 Python 코드에서 다음 패턴을 차단한다.

```txt
import os
import subprocess
import socket
import requests
import urllib
import pymysql
import sqlalchemy
open('/
open('..'
eval(
exec(
__import__
```

단, `open()`은 result.json 저장 때문에 필요할 수 있으므로, 실제 구현에서는 AST 기반 검사를 통해 `outputs/` 하위 파일 쓰기만 허용하는 방식이 바람직하다.

---

# 15. 실행 로그 설계

각 Job은 다음 로그를 남긴다.

```txt
request.json
plan.json
validated-plan.json
extract.log
generated-code/main.py
runner.log
error.log
artifact-index.json
```

### audit log 예시

```json
{
  "jobId": "job_20260512_001",
  "userId": "admin",
  "requestText": "collectgrid 테이블에서 LTE RSRP가 -110보다 약한 격자를 찾아줘.",
  "usedSources": ["mariadb.collectdata"],
  "executedQueries": ["q1"],
  "createdArtifacts": ["result.csv", "chart.png", "report.html"],
  "status": "COMPLETED",
  "createdAt": "2026-05-12T10:00:00+09:00",
  "completedAt": "2026-05-12T10:00:31+09:00"
}
```

---

# 16. 구현 단계

## Phase 1: 기본 실행 골격

```txt
Job 생성 API
Job 저장소
Data Source Catalog 정적 JSON
MariaDB Catalog 수동 등록
CSV/JSON 파일 등록
Query Plan 수동 입력 테스트
Plan Validator 기본 구현
Data Extractor 구현
Dataset Store 저장
```

## Phase 2: Claude Code 연동

```txt
사용자 요청 → Query Plan 생성
Query Plan JSON 파싱
검증 실패 사유 반환
분석 코드 생성 Prompt 작성
Python 코드 생성
생성 코드 저장
```

## Phase 3: Runner 연동

```txt
Python Docker Runner 이미지 생성
Job별 inputs/outputs mount
main.py 실행
result.json 파싱
Artifact 등록
UI 결과 표시
```

## Phase 4: UI 및 사용성 개선

```txt
분석 요청 화면
Job 목록 화면
상태 진행 표시
Query Plan 보기
생성 코드 보기
결과 테이블 미리보기
차트 보기
파일 다운로드
오류 로그 보기
```

## Phase 5: 안정화

```txt
SQL Safety 강화
Python AST 검사
실행 시간 제한
결과 크기 제한
재실행 기능
샘플 템플릿 추가
```

---

# 17. MVP 성공 기준

다음 3개 시나리오가 동작하면 첫 번째 MVP 성공으로 본다.

## 시나리오 1: MariaDB 분석

```txt
사용자 요청:
collectgrid에서 lrsrp가 약한 격자를 찾아 차트와 CSV로 만들어 줘.

결과:
q1.csv
result.csv
chart.png
report.html
```

## 시나리오 2: CSV 분석

```txt
사용자 요청:
업로드한 CSV에서 모듈별 ERROR 건수를 집계해 줘.

결과:
result.csv
chart.png
report.html
```

## 시나리오 3: JSON 분석

```txt
사용자 요청:
JSON 이벤트 로그에서 이벤트 타입별 발생 횟수를 집계해 줘.

결과:
result.csv
chart.png
report.html
```

---

# 18. 향후 확장 방향

MVP 이후 다음 기능을 추가할 수 있다.

```txt
Jira 데이터 소스 추가
Bitbucket 데이터 소스 추가
InfluxDB 데이터 소스 추가
로그 파일 데이터 소스 추가
XLSX 출력
PDF 리포트 출력
Leaflet 지도 시각화
GeoJSON 출력
스케줄 실행
자동 메일 발송
MCP Tool Server 연동
WASM 룰 플러그인 연동
권한별 데이터 접근 제어
결과 재현 기능
분석 템플릿 라이브러리
```

---

# 19. 최종 권장 구조 요약

첫 번째 MVP에서는 다음 구조가 가장 현실적이다.

```txt
Tomcat / Servlet UI
  ↓
Analysis Orchestrator
  ↓
Data Source Catalog
  ↓
Claude Code Query Planner
  ↓
Query Plan Validator
  ↓
MariaDB / JSON / CSV Extractor
  ↓
Dataset Store
  ↓
Claude Code Python Code Generator
  ↓
Python Docker Runner
  ↓
Artifact Store
  ↓
Result Viewer
```

핵심 원칙은 다음이다.

```txt
데이터 접근은 서버가 통제한다.
Claude Code는 계획과 분석 코드를 만든다.
생성 코드는 중간 데이터셋만 읽는다.
실행은 샌드박스에서 한다.
결과는 파일과 로그로 남긴다.
```

이 구조를 따르면 Claude Code의 분석 자유도와 운영 보안성을 함께 확보할 수 있다.
