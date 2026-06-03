package com.ithows.aidclaude;

import com.ithows.ResultMap;
import com.ithows.dao.JobDAO;
import com.ithows.dao.SettingsDAO;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * analyze.py AST 검증 + 실행. (기존 ast-validator.ts + python-runner.ts 포팅)
 *
 * <p>검증 스크립트는 classpath 리소스(ast_validate.py)에서 데이터 홈으로 1회 추출한다.</p>
 */
public final class PythonRunner {

    private PythonRunner() {}

    /** analyze.py가 있으면 검증 후 실행. 결과 JSON 반환. */
    public static JSONObject runAnalysis(String jobId) {
        JSONObject out = new JSONObject();
        ResultMap job = JobDAO.get(jobId);
        if (job == null) return out.put("ok", false).put("error", "작업을 찾을 수 없습니다: " + jobId);
        File jobDir = new File(String.valueOf(job.get("workspace_dir")));
        File analyze = new File(jobDir, "analyze.py");
        if (!analyze.exists()) return out.put("ok", false).put("error", "analyze.py 파일이 없습니다");

        String python = pythonBin();

        // 1) AST 검증
        try {
            File validator = extractValidator();
            ExecResult vr = exec(new String[]{python, validator.getAbsolutePath(), analyze.getAbsolutePath()}, null, null);
            JSONObject v;
            try { v = new JSONObject(vr.stdout.trim()); }
            catch (Exception e) {
                return out.put("ok", false).put("error",
                        "검증기 오류: " + (vr.stderr.isEmpty() ? vr.stdout : vr.stderr));
            }
            if (!v.optBoolean("ok", false)) {
                JSONArray errs = v.optJSONArray("errors");
                return out.put("ok", false).put("error", "AST 검증 실패")
                        .put("validationErrors", errs == null ? new JSONArray() : errs);
            }
        } catch (Exception e) {
            return out.put("ok", false).put("error", "검증 실행 실패: " + e.getMessage());
        }

        // 2) 실행
        try {
            File outputDir = new File(jobDir, "output");
            outputDir.mkdirs();
            ExecResult rr = exec(new String[]{python, analyze.getAbsolutePath()}, jobDir,
                    java.util.Collections.singletonMap("PYTHONPATH", jobDir.getAbsolutePath()));
            out.put("ok", rr.exitCode == 0);
            out.put("exitCode", rr.exitCode);
            out.put("stdout", rr.stdout);
            out.put("stderr", rr.stderr);
            out.put("outputFiles", scanOutput(outputDir));
            if (rr.exitCode != 0) out.put("error", "analyze.py 실행 실패(코드 " + rr.exitCode + ")");
            return out;
        } catch (Exception e) {
            return out.put("ok", false).put("error", "실행 실패: " + e.getMessage());
        }
    }

    private static String pythonBin() {
        Map<String, String> s = SettingsDAO.getAll();
        String p = s.get("pythonBin");
        if (p == null || p.isEmpty()) p = com.ithows.AppConfig.getConf("python_command");
        return (p == null || p.isEmpty()) ? "python" : p;
    }

    private static File extractValidator() throws Exception {
        File dest = new File(AcContext.home(), "ast_validate.py");
        try (InputStream in = PythonRunner.class.getResourceAsStream("/com/ithows/aidclaude/python/ast_validate.py")) {
            if (in == null) throw new Exception("ast_validate.py 리소스를 찾을 수 없습니다");
            Files.copy(in, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return dest;
    }

    private static JSONArray scanOutput(File outputDir) {
        JSONArray arr = new JSONArray();
        File[] files = outputDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && !f.getName().startsWith(".")) {
                    String name = f.getName().toLowerCase();
                    String type = name.endsWith(".csv") ? "csv" : name.endsWith(".png") ? "png"
                            : name.endsWith(".html") ? "html" : name.endsWith(".json") ? "json" : "other";
                    arr.put(new JSONObject().put("name", f.getName())
                            .put("path", f.getAbsolutePath()).put("type", type).put("sizeBytes", f.length()));
                }
            }
        }
        return arr;
    }

    private static final class ExecResult { int exitCode; String stdout = ""; String stderr = ""; }

    private static ExecResult exec(String[] cmd, File cwd, Map<String, String> env) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (cwd != null) pb.directory(cwd);
        pb.environment().put("PYTHONUTF8", "1");
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        if (env != null) pb.environment().putAll(env);
        Process p = pb.start();
        ExecResult r = new ExecResult();
        final StringBuilder errBuf = new StringBuilder();
        Thread errThread = new Thread(() -> {
            try { errBuf.append(readAll(p.getErrorStream())); } catch (Exception ignore) {}
        });
        errThread.start();
        r.stdout = readAll(p.getInputStream());
        errThread.join();
        r.stderr = errBuf.toString();
        r.exitCode = p.waitFor();
        return r;
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
