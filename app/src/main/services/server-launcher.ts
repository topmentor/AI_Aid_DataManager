import { spawn, ChildProcess } from "node:child_process";
import net from "node:net";
import fs from "node:fs";
import path from "node:path";

export interface ServerHandle {
  url: string;
  port: number;
  proc: ChildProcess;
}

/** OS가 할당하는 빈 포트를 하나 얻는다. */
export function findFreePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const srv = net.createServer();
    srv.unref();
    srv.on("error", reject);
    srv.listen(0, "127.0.0.1", () => {
      const addr = srv.address();
      const port = typeof addr === "object" && addr ? addr.port : 0;
      srv.close(() => resolve(port));
    });
  });
}

/** 개발/배포 모드에 따라 jar / web / java 경로를 해석한다. */
export function resolvePaths(opts: { isDev: boolean; resourcesPath: string; projectRoot: string }) {
  const { isDev, resourcesPath, projectRoot } = opts;
  if (isDev) {
    return {
      jar: path.join(projectRoot, "server", "target", "aida-server.jar"),
      web: path.join(projectRoot, "server", "web"),
    };
  }
  // 배포: electron-builder extraResources 에 동봉
  return {
    jar: path.join(resourcesPath, "server", "aida-server.jar"),
    web: path.join(resourcesPath, "server", "web"),
  };
}

/** SSF 서버 프로세스를 기동한다(아직 헬스 확인 전). */
export function startServer(opts: {
  jar: string;
  web: string;
  home: string;
  port: number;
  javaBin?: string;
}): ChildProcess {
  const { jar, web, home, port, javaBin = "java" } = opts;
  if (!fs.existsSync(jar)) {
    throw new Error(`SSF jar 없음: ${jar} (server/build.ps1로 빌드하세요)`);
  }
  fs.mkdirSync(home, { recursive: true });
  const args = [
    `-Daida.home=${home}`,
    `-Dserver.port=${port}`,
    `-Dwebapp.base=${web}`,
    "-jar",
    jar,
  ];
  const proc = spawn(javaBin, args, { stdio: ["ignore", "pipe", "pipe"] });
  proc.stdout?.on("data", (b: Buffer) => process.stdout.write(`[ssf] ${b}`));
  proc.stderr?.on("data", (b: Buffer) => process.stderr.write(`[ssf] ${b}`));
  return proc;
}

/** /api/checkHealth.do 가 응답할 때까지 폴링한다. */
export async function waitHealthy(serverUrl: string, timeoutMs = 30000): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  let lastErr: unknown = null;
  while (Date.now() < deadline) {
    try {
      const res = await fetch(`${serverUrl}/api/checkHealth.do`);
      if (res.ok) return;
    } catch (e) {
      lastErr = e;
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error(`SSF 서버 헬스 타임아웃(${timeoutMs}ms): ${String(lastErr)}`);
}

/** 포트 탐색 → 기동 → 헬스 대기 까지 한 번에. */
export async function launchServer(opts: {
  jar: string;
  web: string;
  home: string;
  javaBin?: string;
  timeoutMs?: number;
}): Promise<ServerHandle> {
  const port = await findFreePort();
  const proc = startServer({ ...opts, port });
  const url = `http://localhost:${port}/AIDA`;
  try {
    await waitHealthy(url, opts.timeoutMs ?? 30000);
  } catch (e) {
    try { proc.kill(); } catch { /* noop */ }
    throw e;
  }
  return { url, port, proc };
}
