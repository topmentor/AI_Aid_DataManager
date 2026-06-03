package com.ithows.aidclaude;

import com.ithows.ResultMap;
import com.ithows.dao.JobDAO;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import org.json.JSONObject;

/**
 * job별 data.db에 대한 SQL 실행. (기존 IPC: jobs:runSql / jobs:runAnalysis)
 */
public final class SqlRunner {

    private SqlRunner() {}

    /** 임의 SQL 실행. result 백업 후 멀티스테이트먼트 실행, 상태/CLAUDE.md 갱신. */
    public static JSONObject runSql(String jobId, String sql) {
        JSONObject o = new JSONObject();
        ResultMap job = JobDAO.get(jobId);
        if (job == null) return o.put("ok", false).put("error", "작업을 찾을 수 없습니다: " + jobId);
        File jobDir = new File(String.valueOf(job.get("workspace_dir")));
        File dbFile = new File(jobDir, "data.db");

        JobDAO.updateStatus(jobId, "running", null);
        try (Connection db = SqliteUtil.open(dbFile.getAbsolutePath())) {
            BackupService.backupResultTable(db);
            SqliteUtil.execScript(db, sql);
            JobDAO.updateStatus(jobId, "done", null);
            JobService.updateClaudeMdFromDb(jobId);
            return o.put("ok", true);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            JobDAO.updateStatus(jobId, "error", msg);
            return o.put("ok", false).put("error", msg);
        }
    }

    /** 워크스페이스의 query.sql을 실행한다(없으면 오류). */
    public static JSONObject runQueryFile(String jobId) {
        JSONObject o = new JSONObject();
        ResultMap job = JobDAO.get(jobId);
        if (job == null) return o.put("ok", false).put("error", "작업을 찾을 수 없습니다: " + jobId);
        File jobDir = new File(String.valueOf(job.get("workspace_dir")));
        File qf = new File(jobDir, "query.sql");
        if (!qf.exists()) {
            return o.put("ok", false).put("error", "query.sql 파일이 없습니다. AI에게 분석을 요청해 주세요.");
        }
        String sql;
        try {
            sql = new String(Files.readAllBytes(qf.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return o.put("ok", false).put("error", "query.sql 읽기 실패: " + e.getMessage());
        }
        BackupService.backupQuerySql(jobDir);
        return runSql(jobId, sql);
    }
}
