# AidClaude — Electron 셸 + 프론트 전송 전환 + 빌드 통합 (B·C·D) 설계

- 작성일: 2026-06-03
- 선행: 하위 프로젝트 A(SSF 데이터 백엔드, `server/`) 완료
- 결정: **C = React 유지 + 전송만 HTTP** / **동적 빈 포트** / Claude·네이티브 다이얼로그는 Electron 잔류

---

## 1. 목표 아키텍처 (통합)

```
Electron main (app/src/main)
  ├ 시작 시 빈 포트 탐색 → java -jar aidclaude-server.jar (-Daidclaude.home, -Dserver.port, -Dwebapp.base)
  │   → /api/checkHealth.do 200 대기 → BrowserWindow 생성
  ├ serverUrl 을 renderer에 전달 (webPreferences.additionalArguments)
  ├ Claude IPC (claude:probe/sendMessage/abort) — 턴 전후 SSF에 HTTP 위임
  ├ 네이티브 IPC (dialog:openFile, export:saveText/saveBinary, files:open)
  └ 종료 시 java 프로세스 정리

preload (app/src/preload)
  ├ window.aidclaude.{settings,catalog,jobs,db,data,files(read*/copy*)} → HTTP fetch(serverUrl + /api/*.do) + 언랩
  └ window.aidclaude.{claude,dialog,export,files.open,on,off} → 기존 IPC 유지

renderer (React) — 거의 불변 (window.aidclaude 표면 동일 유지)
SSF server/ — A에서 완성
```

## 2. B — Electron 셸

- `app/src/main/services/server-launcher.ts` (신규, 테스트 가능):
  - `findFreePort()` — net.Server로 0 바인딩 후 포트 회수
  - `startServer({jar, web, home, port})` → child_process.spawn(java ...), stdout/stderr 로깅
  - `waitHealthy(serverUrl, timeoutMs)` — `/api/checkHealth.do` 폴링
  - `stopServer()` — kill
  - jar/web 경로: 개발=`server/target/aidclaude-server.jar`+`server/web`, 배포=`process.resourcesPath`
  - home = `app.getPath("userData")/aidclaude`
- `app/src/main/services/ssf-client.ts` (신규): main→SSF HTTP 호출 (getSettings, getJob(listJobs 필터), refreshJobSources, getSqlOptions, runJobAnalysis). Node 18+ 전역 fetch 사용.
- `claude-service.ts` 재작성: DB/job-service/backup 의존 제거. 흐름:
  1. `ssfClient.getJob(jobId)` → workspaceDir, `getSettings().claudeBin`
  2. `ssfClient.refreshJobSources(jobId)` (턴 전 소스 갱신)
  3. request.md 기록 → chokidar로 query.sql 감시 → `job:analyze_code` push
  4. `queryClaude(cwd=workspaceDir)` 스트리밍 → `claude:stream`
  5. 완료: `claude:done`; `getSqlOptions` 조회 → 옵션<2면 `runJobAnalysis` 호출(SSF가 SQL/백업 실행) → `job:update`(SSF getJob + status) push
  6. 오류/중단: `claude:error` / idle
- `main/index.ts` 재작성: 시작 시 서버 기동, **데이터 IPC 핸들러 전부 제거**, Claude/dialog/export/files.open 핸들러 유지(claude는 serverUrl 사용), serverUrl을 additionalArguments로 전달.
- 제거(미사용): `settings-service`, `catalog-service`, `schema-inspector`, `sqlite-loader`, `job-service`, `python-runner`, `ast-validator`, `system-prompt`, `backup-service`. → `better-sqlite3`/`mysql2`/`papaparse`/`shapefile`/`@types/*`/`electron-rebuild`/postinstall 제거(네이티브 빌드 불필요).

## 3. C — preload 전송 어댑터 (API 클라이언트)

- `app/src/preload/api-client.ts` (신규, 테스트 가능, electron 비의존): `serverUrl` 받아 각 메서드를 fetch+언랩으로 구현. SSF 봉투 `{result, resultMap, resultList, count, msg}` → 기존 IPC 반환 형태로 매핑:
  - settings.get/set → resultMap; catalog.list → resultList, add/getSchema/previewData → resultMap; testConnection → resultMap
  - jobs.create/list → Job 매핑(`createdAt`=String, `outputFiles`=[]); runAnalysis/runSql → resultMap; getSqlOptions/listQueryHistory/listAllOrphanTables → resultList; dropAllOrphanTables → `{ok:true, dropped}`
  - db.listTables → resultList, previewTable → resultMap, saveAsSource → `{ok:true, source:resultMap}`; data.saveAsSource → 동일
  - files.readText → resultMap.content, readLines → resultList, readBase64 → resultMap.base64, copyToData → resultMap.path, copyShapefile → resultMap
  - 오류(`result!=="OK"`)는 throw 또는 해당 메서드 규약(ok:false)로 변환
- `preload/index.ts`: 데이터 네임스페이스는 api-client 위임, claude/dialog/export/files.open/on/off는 IPC 유지. serverUrl은 `process.argv`의 `--server-url=` 파싱.
- SSF 보강: `SchemaController.getSchema`가 resultMap에 `sourceId`,`sourceName` 추가(DataSourceSchema 호환).

## 4. D — 빌드/실행 스크립트

- `setup.ps1` 확장: (1) Java≥17/Maven 확인, (2) `server/build.ps1 -SkipTests`로 jar 생성, (3) 기존 Node/npm 흐름. `-Dev`: jar 준비 후 `npm run dev`(main이 jar 기동). `-Build`: jar + `npm run build` + electron 패키징(jar+web를 resources에 포함).
- `app/package.json`: 네이티브 의존성 제거, postinstall 삭제, electron-builder `extraResources`에 `server/target/aidclaude-server.jar` + `server/web` 추가(배포 시).

## 5. 검증 전략

GUI 직접 구동은 본 환경에서 제한적 → 다음으로 검증:
- `server-launcher`/`api-client`를 Node 스크립트로 **라이브 SSF에 대해 테스트**(스폰·헬스·각 메서드 왕복).
- `npm run typecheck`(tsc) — 형상 불일치 검출.
- `npm run build`(electron-vite) — 컴파일 검증.
- 기존 `server/smoke/*` 회귀.

## 6. 리스크
- Job/DataSourceSchema 형상 미세 불일치 → typecheck + api-client 노드 테스트로 차단.
- 동적 포트를 preload가 알아야 함 → additionalArguments 전달.
- Claude 턴 상태(planning) 동기화 → main이 job:update로 status 직접 보고(SSF getJob + override).
- 배포 패키징(jar 동봉 + JRE) → D에서 다루되, JRE 번들은 후속(개발은 시스템 java).
