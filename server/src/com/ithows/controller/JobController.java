package com.ithows.controller;

import com.ithows.HttpUtil;
import com.ithows.ResultMap;
import com.ithows.aida.AidaResp;
import com.ithows.aida.JobService;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.ithows.dao.JobDAO;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 작업(job) API. (기존 IPC: jobs:list/create/refreshSources)
 * SQL 실행/DB 브라우저는 별도 컨트롤러(M6)에서 추가.
 */
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class JobController {

    @ControllerMethodInfo(id = "/api/listJobs.do")
    @ApiInfo(summary = "작업 목록", tag = "Jobs", method = "GET")
    public String listJobs(HttpSession session, HttpServletRequest request,
                           HttpServletResponse response, Object command) {
        List<ResultMap> rows = JobDAO.list();
        if (rows == null) return AidaResp.error(request, "작업 목록 조회 실패");
        JSONArray arr = new JSONArray();
        for (ResultMap r : rows) arr.put(JobService.jobJson(r));
        return AidaResp.list(request, arr);
    }

    @ControllerMethodInfo(id = "/api/createJob.do")
    @ApiInfo(summary = "작업 생성", description = "워크스페이스/data.db/CLAUDE.md 생성 후 선택 소스를 적재합니다.",
             tag = "Jobs", method = "POST")
    public String createJob(HttpSession session, HttpServletRequest request,
                            HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String userRequest = body.optString("userRequest", "");
        List<String> sourceIds = strList(body.optJSONArray("sourceIds"));
        try {
            return AidaResp.map(request, JobService.createJob(userRequest, sourceIds));
        } catch (Exception e) {
            return AidaResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/refreshJobSources.do")
    @ApiInfo(summary = "작업 소스 갱신", tag = "Jobs", method = "POST")
    public String refreshJobSources(HttpSession session, HttpServletRequest request,
                                    HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String jobId = body.optString("jobId", "").trim();
        if (jobId.isEmpty()) return AidaResp.error(request, "jobId가 필요합니다");
        try {
            boolean ok = JobService.refreshJobSources(jobId);
            return ok ? AidaResp.ok(request) : AidaResp.no(request, "작업을 찾을 수 없습니다");
        } catch (Exception e) {
            return AidaResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/runJobSql.do")
    @ApiInfo(summary = "작업 SQL 실행", description = "임의 SQL을 job의 data.db에 실행합니다(result 백업 후).",
             tag = "Jobs", method = "POST")
    public String runJobSql(HttpSession session, HttpServletRequest request,
                            HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String jobId = body.optString("jobId", "").trim();
        String sql = body.optString("sql", "");
        if (jobId.isEmpty() || sql.trim().isEmpty()) return AidaResp.error(request, "jobId/sql이 필요합니다");
        return AidaResp.map(request, com.ithows.aida.SqlRunner.runSql(jobId, sql));
    }

    @ControllerMethodInfo(id = "/api/runJobAnalysis.do")
    @ApiInfo(summary = "작업 분석 실행", description = "워크스페이스의 query.sql을 실행합니다.",
             tag = "Jobs", method = "POST")
    public String runJobAnalysis(HttpSession session, HttpServletRequest request,
                                 HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String jobId = body.optString("jobId", "").trim();
        if (jobId.isEmpty()) return AidaResp.error(request, "jobId가 필요합니다");
        ResultMap job = JobDAO.get(jobId);
        if (job == null) return AidaResp.error(request, "작업을 찾을 수 없습니다");
        java.io.File analyze = new java.io.File(String.valueOf(job.get("workspace_dir")), "analyze.py");
        if (analyze.exists()) {
            return AidaResp.map(request, com.ithows.aida.PythonRunner.runAnalysis(jobId));
        }
        return AidaResp.map(request, com.ithows.aida.SqlRunner.runQueryFile(jobId));
    }

    @ControllerMethodInfo(id = "/api/getSqlOptions.do")
    @ApiInfo(summary = "SQL 옵션 조회", description = "query.sql의 -- [옵션 N] 마커를 파싱합니다.", tag = "Jobs", method = "POST")
    public String getSqlOptions(HttpSession session, HttpServletRequest request,
                                HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String jobId = body.optString("jobId", "").trim();
        if (jobId.isEmpty()) return AidaResp.error(request, "jobId가 필요합니다");
        return AidaResp.list(request, com.ithows.aida.JobService.sqlOptionsForJob(jobId));
    }

    @ControllerMethodInfo(id = "/api/listQueryHistory.do")
    @ApiInfo(summary = "쿼리 히스토리 목록", tag = "Jobs", method = "POST")
    public String listQueryHistory(HttpSession session, HttpServletRequest request,
                                   HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String jobId = body.optString("jobId", "").trim();
        if (jobId.isEmpty()) return AidaResp.error(request, "jobId가 필요합니다");
        return AidaResp.list(request, com.ithows.aida.JobService.queryHistory(jobId));
    }

    @ControllerMethodInfo(id = "/api/listOrphanTables.do")
    @ApiInfo(summary = "고아 테이블 목록", description = "모든 job DB의 미사용 테이블을 조회합니다.", tag = "Jobs", method = "GET")
    public String listOrphanTables(HttpSession session, HttpServletRequest request,
                                   HttpServletResponse response, Object command) {
        return AidaResp.list(request, com.ithows.aida.OrphanService.list());
    }

    @ControllerMethodInfo(id = "/api/dropOrphanTables.do")
    @ApiInfo(summary = "고아 테이블 삭제", tag = "Jobs", method = "POST")
    public String dropOrphanTables(HttpSession session, HttpServletRequest request,
                                   HttpServletResponse response, Object command) {
        int dropped = com.ithows.aida.OrphanService.dropAll();
        return AidaResp.map(request, new JSONObject().put("dropped", dropped));
    }

    static List<String> strList(JSONArray a) {
        List<String> out = new ArrayList<>();
        if (a != null) for (int i = 0; i < a.length(); i++) out.add(a.optString(i, ""));
        return out;
    }

    private static JSONObject bodyOrNull(HttpServletRequest request) {
        try { return HttpUtil.getBodyJson(request); } catch (Exception e) { return null; }
    }
}
