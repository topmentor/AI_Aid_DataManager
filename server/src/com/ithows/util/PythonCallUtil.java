package com.ithows.util;

import com.ithows.AppConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

import org.json.JSONObject;

/**
 * 파이썬 스크립트 호출 유틸리티.
 *
 * 호출 흐름:
 *   1. 임시 요청 파일(request JSON)을 디스크에 기록한다.
 *   2. ProcessBuilder 로 파이썬을 실행한다.
 *        python &lt;script&gt; --request &lt;req.json&gt; --response &lt;res.json&gt;
 *   3. 파이썬이 기록한 응답 파일을 읽어 JSON 으로 파싱해 반환한다.
 *   4. 임시 파일을 정리한다.
 *
 * 설정 (configplatform.xml 에서 선택적 지정):
 *   <pre>
 *   &lt;!-- python_command 는 OS 무관 단일 키 --&gt;
 *   &lt;entry key="python_command"&gt;python3&lt;/entry&gt;
 *
 *   &lt;!-- 디렉토리는 OS 별 컨텍스트 루트(context_win_dir / context_dir) 하위의 "상대 경로" 로 지정.
 *        실제 절대 경로 = AppConfig.getContextPath() + 설정값
 *        (Windows 면 context_win_dir, 그 외엔 context_dir 가 자동 선택됨)        --&gt;
 *   &lt;entry key="python_script_dir"&gt;python_process/&lt;/entry&gt;
 *   &lt;entry key="python_temp_dir"  &gt;python_process/temp/&lt;/entry&gt;
 *   </pre>
 *
 * 디렉토리 경로 해석:
 *   1. 설정값이 절대 경로면 그대로 사용 (override 가능)
 *   2. 상대 경로면 AppConfig.getContextPath() 와 결합 → OS 별 컨텍스트 루트 하위로 해석
 *   3. 설정값이 없거나 컨텍스트 루트도 비어 있으면 코드 기본값
 *
 * 코드 기본값:
 *   - python_command    : "python"
 *   - python_script_dir : $user.dir/python_process
 *   - python_temp_dir   : java.io.tmpdir
 *
 * 반환 규약:
 *   - 파이썬이 정상 종료하고 응답 파일을 남기면 그 JSON 을 그대로 반환.
 *   - 그 외의 실패 상황(스크립트 없음, 타임아웃, 응답 파일 없음, 잘못된 JSON 등)은
 *     {"result":"ERROR", "msg":"<원인>"} 형태의 JSONObject 를 반환한다.
 *   - 호출부는 최상위 "result" 키가 "OK" 인지 확인해서 성공/실패를 구별할 수 있다.
 */
public class PythonCallUtil {

    /** 기본 타임아웃 (초). */
    public static final int DEFAULT_TIMEOUT_SEC = 60;

    /** 기본 타임아웃으로 호출. */
    public static JSONObject callPython(String scriptName, JSONObject requestJson) {
        return callPython(scriptName, requestJson, DEFAULT_TIMEOUT_SEC);
    }

    /**
     * 파이썬 스크립트를 동기 호출하고 응답 JSON 을 반환한다.
     *
     * @param scriptName  스크립트 파일명 (확장자 생략 가능). 경로는 python_script_dir 아래.
     * @param requestJson 파이썬에 전달할 JSON (null 허용)
     * @param timeoutSec  타임아웃(초). 초과 시 프로세스 강제 종료.
     * @return 응답 JSONObject (항상 non-null)
     */
    public static JSONObject callPython(String scriptName, JSONObject requestJson, int timeoutSec) {

        if (scriptName == null || scriptName.isEmpty()) {
            return error("scriptName is required");
        }

        // 1. 설정 해석 — 디렉토리는 OS 별 컨텍스트 루트(AppConfig.getContextPath()) 하위로 결합
        String pythonCmd = resolveConfig("python_command", "python");
        String scriptDir = resolveContextDir("python_script_dir",
                System.getProperty("user.dir") + File.separator + "python_process");
        String tempDir   = resolveContextDir("python_temp_dir",
                System.getProperty("java.io.tmpdir"));

        if (!scriptName.toLowerCase().endsWith(".py")) {
            scriptName += ".py";
        }

        // 2. 스크립트 파일 경로 보안 검증
        //    - 상위 경로 탈출 방지를 위해 basename 만 사용
        String safeName = new File(scriptName).getName();
        File scriptFile = new File(scriptDir, safeName);
        if (!scriptFile.isFile()) {
            return error("Python script not found: " + scriptFile.getAbsolutePath());
        }

        // 3. 임시 요청/응답 파일 준비
        File tempDirFile = new File(tempDir);
        if (!tempDirFile.exists()) tempDirFile.mkdirs();

        String guid    = UUID.randomUUID().toString().replace("-", "");
        File   reqFile = new File(tempDirFile, "py_req_" + guid + ".json");
        File   resFile = new File(tempDirFile, "py_res_" + guid + ".json");

        StringBuilder stdout = new StringBuilder();

        try {
            // 4. 요청 JSON 파일 쓰기
            String reqText = (requestJson == null ? new JSONObject() : requestJson).toString();
            Files.write(reqFile.toPath(), reqText.getBytes(StandardCharsets.UTF_8));

            
            // 5. 파이썬 프로세스 실행
            ProcessBuilder pb = new ProcessBuilder(
                    pythonCmd,
                    scriptFile.getAbsolutePath(),
                    "--request",  reqFile.getAbsolutePath(),
                    "--response", resFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);   // stderr → stdout 병합
            Process process = pb.start();

            // 6. 자식 프로세스 stdout 비동기 수집 (블로킹 방지 + 디버깅 용)
            Thread drainer = new Thread(new StreamDrainer(process, stdout), "py-stdout-drain");
            drainer.setDaemon(true);
            drainer.start();

            // 7. 타임아웃 대기
            boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return error("Python call timed out after " + timeoutSec + "s");
            }
            try { drainer.join(500); } catch (InterruptedException ignore) {}

            int exitCode = process.exitValue();

            // 8. 응답 파일 읽기
            if (!resFile.isFile()) {
                return error("Python call finished (exit=" + exitCode
                        + ") but no response file was written. Stdout:\n" + stdout);
            }

            String body = new String(Files.readAllBytes(resFile.toPath()), StandardCharsets.UTF_8);
            
            try {
                return new JSONObject(body);
            } catch (Exception jsonEx) {
                return error("Response file is not valid JSON: " + jsonEx.getMessage()
                        + "\nRaw content:\n" + body);
            }

        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.toString() : e.getMessage();
            if (stdout.length() > 0) detail += "\nStdout:\n" + stdout;
            return error("Python call failed: " + detail);

        } finally {
            // 9. 임시 파일 정리
            try { if (reqFile.exists()) reqFile.delete(); } catch (Exception ignore) {}
            try { if (resFile.exists()) resFile.delete(); } catch (Exception ignore) {}
        }
    }

    // ------------------------------------------------------------------

    /**
     * 진단용 — 현재 해석된 Python 실행 설정과 스크립트 디렉토리 상태를 반환한다.
     * callPython 이 실패할 때 어디서 문제가 생겼는지 빠르게 확인하기 위한 헬퍼.
     *
     * 반환 JSON 구조:
     * <pre>
     * {
     *   "python_command":    "python",
     *   "python_script_dir": "C:\\03_work\\SSF2026\\python_process",
     *   "python_temp_dir":   "C:\\Users\\...\\AppData\\Local\\Temp\\",
     *   "user.dir":          "...",     // JVM 작업 디렉토리
     *   "os.name":           "Windows 11",
     *   "script_dir_exists": true,
     *   "scripts":           ["tutorial_echo.py", "tutorial_text_stats.py"],
     *   "python_version":    "Python 3.11.4"   // python --version 실행 결과
     * }
     * </pre>
     */
    public static JSONObject getDiagnosticInfo() {
        JSONObject info = new JSONObject();
        try {
            String pythonCmd = resolveConfig("python_command", "python");
            String scriptDir = resolveContextDir("python_script_dir",
                    System.getProperty("user.dir") + File.separator + "python_process");
            String tempDir   = resolveContextDir("python_temp_dir",
                    System.getProperty("java.io.tmpdir"));

            info.put("python_command",    pythonCmd);
            info.put("python_script_dir", scriptDir);
            info.put("python_temp_dir",   tempDir);
            info.put("context_path",      safeContextPath());
            info.put("user.dir",          System.getProperty("user.dir"));
            info.put("os.name",           System.getProperty("os.name"));

            File dir = new File(scriptDir);
            info.put("script_dir_exists", dir.isDirectory());

            org.json.JSONArray scripts = new org.json.JSONArray();
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().toLowerCase().endsWith(".py")) {
                            scripts.put(f.getName());
                        }
                    }
                }
            }
            info.put("scripts", scripts);

            // python --version 실행 시도 (빠른 connectivity 체크)
            info.put("python_version", probePythonVersion(pythonCmd));

        } catch (JSONException ex) {
            Logger.getLogger(PythonCallUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return info;
    }

    /** python --version probe 의 기본 타임아웃 (초). configplatform.xml 에서 override 가능. */
    public static final int DEFAULT_VERSION_PROBE_TIMEOUT_SEC = 5;

    /**
     * python --version 을 실행해 stdout 한 줄을 회수. 실패 시 괄호 메시지.
     *
     * 타임아웃은 configplatform.xml 의 "python_version_timeout_sec" 값을 따른다.
     * 설정이 없거나 파싱 실패면 {@link #DEFAULT_VERSION_PROBE_TIMEOUT_SEC}.
     */
    private static String probePythonVersion(String pythonCmd) {
        int timeoutSec = resolveConfigInt("python_version_timeout_sec",
                DEFAULT_VERSION_PROBE_TIMEOUT_SEC);
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder out = new StringBuilder();
            Thread drainer = new Thread(new StreamDrainer(p, out), "py-version-drain");
            drainer.setDaemon(true);
            drainer.start();

            if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "(timed out after " + timeoutSec + "s)";
            }
            try { drainer.join(500); } catch (InterruptedException ignore) {}

            String s = out.toString().trim();
            return s.isEmpty() ? "(exit=" + p.exitValue() + ", no output)" : s;

        } catch (Exception e) {
            return "(failed to run '" + pythonCmd + " --version': "
                    + (e.getMessage() == null ? e.toString() : e.getMessage()) + ")";
        }
    }

    /** AppConfig 에서 값 조회, 없거나 비었으면 defaultValue 반환. */
    private static String resolveConfig(String key, String defaultValue) {
        String v = tryGetConfig(key);
        return v != null ? v : defaultValue;
    }

    /** AppConfig 에서 정수 값 조회. 미존재/빈값/파싱 실패면 defaultValue. */
    private static int resolveConfigInt(String key, int defaultValue) {
        String v = tryGetConfig(key);
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException ignore) {
            return defaultValue;
        }
    }

    /**
     * OS 별 컨텍스트 루트(AppConfig.getContextPath()) 하위의 디렉토리를 해석한다.
     *
     *   1. configKey 가 절대 경로면 그대로 사용 (override 가능)
     *   2. 상대 경로면 컨텍스트 루트와 결합 — Windows 면 context_win_dir,
     *      Linux 면 context_dir 가 자동 선택됨
     *   3. configKey 가 없거나 컨텍스트 루트도 비어 있으면 defaultValue
     */
    private static String resolveContextDir(String configKey, String defaultValue) {
        String subDir = tryGetConfig(configKey);
        if (subDir == null) return defaultValue;

        File subFile = new File(subDir);
        if (subFile.isAbsolute()) {
            return subFile.getAbsolutePath();
        }

        String ctxRoot = safeContextPath();
        if (ctxRoot == null || ctxRoot.isEmpty()) {
            // 컨텍스트 루트 미설정 + 상대 경로 → 안전하게 defaultValue
            return defaultValue;
        }
        return new File(ctxRoot, subDir).getAbsolutePath();
    }

    /** AppConfig.getContextPath() 를 예외 안전하게 호출. 실패/미초기화면 null. */
    private static String safeContextPath() {
        try {
            return AppConfig.getContextPath();
        } catch (Exception ignore) {
            return null;
        }
    }

    /** AppConfig 에서 단일 키 조회. 미초기화/누락/빈 값이면 null. */
    private static String tryGetConfig(String key) {
        try {
            if (AppConfig.has(key)) {
                String s = AppConfig.getConf(key);
                if (s != null && !s.isEmpty()) return s;
            }
        } catch (Exception ignore) {
            // AppConfig 미초기화 환경(단위 테스트 등)도 허용
        }
        return null;
    }

    private static JSONObject error(String msg) {
        JSONObject o = new JSONObject();
        try {
            o.put("result", "ERROR");
            o.put("msg",    msg);
        } catch (JSONException ex) {
            Logger.getLogger(PythonCallUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return o;
    }

    /** 자식 프로세스 stdout 을 읽어 StringBuilder 로 흡수하는 Runnable. */
    private static final class StreamDrainer implements Runnable {
        private final Process       process;
        private final StringBuilder sink;

        StreamDrainer(Process process, StringBuilder sink) {
            this.process = process;
            this.sink    = sink;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    synchronized (sink) { sink.append(line).append('\n'); }
                }
            } catch (Exception ignore) {
            }
        }
    }
}
