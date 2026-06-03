package com.ithows.aidclaude;

import com.ithows.ResultMap;
import com.ithows.aidclaude.model.SourceRef;
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
        File jobDir = AcContext.jobDir(id);
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
                        System.err.println("[AidClaude] 소스 적재 실패 (" + s.name + "): " + e.getMessage());
                    }
                }
            }
        }

        writeFile(new File(jobDir, "CLAUDE.md"), SystemPromptBuilder.buildClaudeMd(tableInfos));
        writeFile(new File(jobDir, "data_helpers.py"), SystemPromptBuilder.buildDataHelpers(tableNames));
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
