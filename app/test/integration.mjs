// 통합 런타임 테스트: 실제 SSF jar 기동 + 실제 api-client.ts(esbuild 번들) 왕복 검증.
// 실행: node app/test/integration.mjs   (사전: server jar 빌드, esbuild로 api-client 번들 생성)
import { spawn } from "node:child_process";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(__dirname, "..", "..");
const jar = path.join(repo, "server", "target", "aida-server.jar");
const web = path.join(repo, "server", "web");
const port = 8790;
const base = `http://localhost:${port}/AIDA`;
const home = mkdtempSync(path.join(os.tmpdir(), "acint_"));

let fail = 0;
const check = (n, c) => { console.log(`  ${c ? "PASS" : "FAIL"}  ${n}`); if (!c) fail++; };

const proc = spawn("java", [
  `-Daida.home=${home}`, `-Dserver.port=${port}`, `-Dwebapp.base=${web}`, "-jar", jar,
], { stdio: "ignore" });

async function waitHealthy() {
  for (let i = 0; i < 40; i++) {
    await new Promise((r) => setTimeout(r, 700));
    try { const res = await fetch(`${base}/api/checkHealth.do`); if (res.ok) return true; } catch {}
  }
  return false;
}

try {
  if (!(await waitHealthy())) throw new Error("server not healthy");
  console.log("[integration] server up:", base);

  const { createApiClient } = await import(pathToFileURL(path.join(__dirname, ".apiclient.mjs")).href);
  const api = createApiClient(base);

  // settings
  const s = await api.settings.get();
  check("settings.get claudeBin", s.claudeBin === "claude");
  const s2 = await api.settings.set({ pythonBin: "py" });
  check("settings.set roundtrip", s2.pythonBin === "py");

  // catalog (한글 + 콤마 셀 CSV)
  const csv = path.join(home, "d.csv");
  writeFileSync(csv, "id,name\n1,Alice\n2,\"Bob, Jr.\"\n3,홍길동\n", "utf-8");
  const added = await api.catalog.add({ name: "샘플", type: "csv", config: { filePath: csv } });
  check("catalog.add id", !!added.id);
  check("catalog.add config", added.config.filePath === csv);
  const list = await api.catalog.list();
  check("catalog.list 1", list.length === 1 && list[0].name === "샘플");
  const tc = await api.catalog.testConnection(added.id);
  check("testConnection ok", tc.ok === true);

  // schema/preview
  const sc = await api.catalog.getSchema(added.id);
  check("getSchema rowCount 3", sc.rowCount === 3);
  check("getSchema sourceId injected", sc.sourceId === added.id);
  const pv = await api.catalog.previewData(added.id, 10);
  check("previewData comma cell intact", pv.rows[1][1] === "Bob, Jr.");
  check("previewData korean", pv.rows[2][1] === "홍길동");

  // job + sql + db browser
  const job = await api.jobs.create("합계", [added.id]);
  check("jobs.create workspaceDir", !!job.workspaceDir);
  check("jobs.create createdAt string", typeof job.createdAt === "string");
  const run = await api.jobs.runSql(job.id, 'DROP TABLE IF EXISTS result; CREATE TABLE result AS SELECT name FROM "샘플";');
  check("jobs.runSql ok", run.ok === true);
  const tables = await api.db.listTables(job.id);
  check("db.listTables has result", tables.includes("result"));
  const ptab = await api.db.previewTable(job.id, "result");
  check("db.previewTable rows", ptab.rows.length === 3);
  const jobs = await api.jobs.list();
  check("jobs.list 1", jobs.length === 1);

  // files (write/read under home)
  const scratch = path.join(home, "scratch.txt");
  await api.filesHttp.writeText(scratch, "테스트");
  const txt = await api.filesHttp.readText(scratch);
  check("files write/read korean", txt === "테스트");

  // save table as source
  const saved = await api.db.saveAsSource(job.id, "result", "결과복사");
  check("db.saveAsSource ok", saved.ok === true);
  const list2 = await api.catalog.list();
  check("catalog has 2 now", list2.length === 2);
} catch (e) {
  console.error("[integration] ERROR", e);
  fail++;
} finally {
  try { proc.kill(); } catch {}
  rmSync(home, { recursive: true, force: true });
  rmSync(path.join(os.tmpdir(), `aida-tomcat-${port}`), { recursive: true, force: true });
}

if (fail > 0) { console.log(`[integration] FAILED (${fail})`); process.exit(1); }
console.log("[integration] ALL PASS");
