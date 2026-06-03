package com.ithows.aida;

import com.ithows.ResultMap;
import com.ithows.aida.model.SourceRef;
import com.ithows.dao.CatalogDAO;
import com.ithows.dao.JobDAO;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 작업 워크스페이스 생성/갱신. 기존 job-service.ts 포팅(Claude 호출 제외 — Electron 담당).
 *
 * <p>워크스페이스: {@code <home>/jobs/job_<uuid>/} 에 data.db / CLAUDE.md / data_helpers.py /
 * sources.json / output/ 생성.</p>
 */
public final class JobService {

    private JobService() {}

    /** job 생성 후 job 메타(JSON: id/userRequest/status/workspaceDir/createdAt) 반환. */
    public static JSONObject createJob(String userRequest, List<String> sourceIds) throws Exception {
        String id = UUID.randomUUID().toString();
        File jobDir = AidaContext.jobDir(id);
        jobDir.mkdirs();
        new File(jobDir, "output").mkdirs();

        loadSourcesAndWrite(jobDir, sourceIds);

        // sourceIds 보존(refresh용)
        writeFile(new File(jobDir, "sources.json"), new JSONArray(sourceIds).toString());

        JobDAO.insert(id, userRequest, "idle", jobDir.getAbsolutePath());
        return jobJson(JobDAO.get(id));
    }

    /** 저장된 sourceIds로 data.db 재적재 + CLAUDE.md/data_helpers.py 갱신. */
    public static boolean refreshJobSources(String jobId) throws Exception {
        ResultMap job = JobDAO.get(jobId);
        if (job == null) return false;
        File jobDir = new File(String.valueOf(job.get("workspace_dir")));
        List<String> sourceIds = readSourceIds(jobDir);
        loadSourcesAndWrite(jobDir, sourceIds);
        return true;
    }

    /** data.db 적재 + CLAUDE.md/data_helpers.py 기록 공통 로직. */
    private static void loadSourcesAndWrite(File jobDir, List<String> sourceIds) throws Exception {
        File dbFile = new File(jobDir, "data.db");
        List<SystemPromptBuilder.TableInfo> tableInfos = new ArrayList<>();
        List<String> tableNames = new ArrayList<>();

        try (Connection db = SqliteUtil.open(dbFile.getAbsolutePath())) {
            if (sourceIds != null) {
                for (String sid : sourceIds) {
                    ResultMap row = CatalogDAO.get(sid);
                    if (row == null) continue;
                    SourceRef s = SourceRef.from(row);
                    try {
                        for (SourceLoader.LoadedTable lt : SourceLoader.load(db, s)) {
                            tableInfos.add(new SystemPromptBuilder.TableInfo(lt.tableName, lt.rowCount, s.name));
                            tableNames.add(lt.tableName);
                        }
                    } catch (Exception e) {
                        System.err.println("[AIDA] 소스 적재 실패 (" + s.name + "): " + e.getMessage());
                    }
                }
            }
        }

        writeFile(new File(jobDir, "CLAUDE.md"), SystemPromptBuilder.buildClaudeMd(tableInfos));
        writeFile(new File(jobDir, "data_helpers.py"), SystemPromptBuilder.buildDataHelpers(tableNames));
    }

    /** SQL 실행 후: 현재 data.db 테이블 목록/행수로 CLAUDE.md·data_helpers.py 재작성(소스 재적재 없음). */
    public static void updateClaudeMdFromDb(String jobId) {
        ResultMap job = JobDAO.get(jobId);
        if (job == null) return;
        File jobDir = new File(String.valueOf(job.get("workspace_dir")));
        File dbFile = new File(jobDir, "data.db");
        if (!dbFile.exists()) return;
        List<SystemPromptBuilder.TableInfo> infos = new ArrayList<>();
        List<String> names = new ArrayList<>();
        try (Connection db = SqliteUtil.open(dbFile.getAbsolutePath())) {
            for (String t : SqliteUtil.listTables(db)) {
                if (t.matches("result_bak_\\d+")) continue;
                long cnt = 0;
                ResultMap c = SqliteUtil.queryForMap(db, "SELECT COUNT(*) AS n FROM " + SqliteUtil.quoteIdent(t), null);
                if (c != null) cnt = c.getLong("n", 0);
                infos.add(new SystemPromptBuilder.TableInfo(t, cnt, ""));
                names.add(t);
            }
            writeFile(new File(jobDir, "CLAUDE.md"), SystemPromptBuilder.buildClaudeMd(infos));
            writeFile(new File(jobDir, "data_helpers.py"), SystemPromptBuilder.buildDataHelpers(names));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** query.sql의 {@code -- [옵션 N] 제목} 마커를 파싱해 옵션 목록 반환(2개 미만이면 빈 배열). */
    public static JSONArray parseSqlOptions(String sql) {
        JSONArray out = new JSONArray();
        if (sql == null) return out;
        java.util.regex.Pattern marker = java.util.regex.Pattern.compile("^--\\s*\\[옵션\\s*\\d+\\]\\s*(.*)");
        String curTitle = null;
        StringBuilder cur = new StringBuilder();
        List<JSONObject> opts = new ArrayList<>();
        for (String line : sql.split("\n", -1)) {
            java.util.regex.Matcher m = marker.matcher(line);
            if (m.find()) {
                if (curTitle != null) {
                    String s = cur.toString().trim();
                    if (!s.isEmpty()) opts.add(new JSONObject().put("title", curTitle).put("sql", s));
                }
                String t = m.group(1).trim();
                curTitle = t.isEmpty() ? ("옵션 " + (opts.size() + 1)) : t;
                cur.setLength(0);
            } else if (curTitle != null) {
                cur.append(line).append("\n");
            }
        }
        if (curTitle != null) {
            String s = cur.toString().trim();
            if (!s.isEmpty()) opts.add(new JSONObject().put("title", curTitle).put("sql", s));
        }
        if (opts.size() >= 2) for (JSONObject o : opts) out.put(o);
        return out;
    }

    /** query.sql 옵션 파싱(파일에서 읽어). */
    public static JSONArray sqlOptionsForJob(String jobId) {
        ResultMap job = JobDAO.get(jobId);
        if (job == null) return new JSONArray();
        File qf = new File(String.valueOf(job.get("workspace_dir")), "query.sql");
        if (!qf.exists()) return new JSONArray();
        try {
            String sql = new String(Files.readAllBytes(qf.toPath()), StandardCharsets.UTF_8);
            return parseSqlOptions(sql);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    /** history/query_NNN.sql 목록(최신순). */
    public static JSONArray queryHistory(String jobId) {
        JSONArray out = new JSONArray();
        ResultMap job = JobDAO.get(jobId);
        if (job == null) return out;
        File dir = new File(String.valueOf(job.get("workspace_dir")));
        for (String f : BackupService.listQueryHistory(dir)) out.put(f);
        return out;
    }

    public static JSONObject jobJson(ResultMap r) {
        JSONObject o = new JSONObject();
        if (r == null) return o;
        o.put("id", strOr(r.get("id"), ""));
        o.put("userRequest", strOr(r.get("user_request"), ""));
        o.put("status", strOr(r.get("status"), ""));
        Object err = r.get("error_msg");
        o.put("errorMsg", err == null ? JSONObject.NULL : String.valueOf(err));
        o.put("workspaceDir", strOr(r.get("workspace_dir"), ""));
        o.put("createdAt", longOr(r.get("created_at")));
        o.put("updatedAt", longOr(r.get("updated_at")));
        return o;
    }

    private static List<String> readSourceIds(File jobDir) {
        List<String> ids = new ArrayList<>();
        try {
            File f = new File(jobDir, "sources.json");
            if (f.exists()) {
                JSONArray arr = new JSONArray(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                for (int i = 0; i < arr.length(); i++) ids.add(arr.getString(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ids;
    }

    private static void writeFile(File f, String content) throws Exception {
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String strOr(Object o, String d) { return o == null ? d : String.valueOf(o); }
    private static long longOr(Object o) {
        try { return o == null ? 0L : Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }
}
