package com.ithows.controller;

import com.ithows.HttpUtil;
import com.ithows.ResultMap;
import com.ithows.aida.AidaResp;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.ithows.dao.SqlHistoryDAO;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * SQL 실행 히스토리 API (앱 전역, app.db). SqlPanel 이전 쿼리 저장/불러오기.
 */
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class SqlHistoryController {

    @ControllerMethodInfo(id = "/api/listSqlHistory.do")
    @ApiInfo(summary = "SQL 히스토리 목록", tag = "SQL", method = "GET")
    public String listSqlHistory(HttpSession session, HttpServletRequest request,
                                 HttpServletResponse response, Object command) {
        int limit = HttpUtil.getParameterInt(request, "limit", 100);
        List<ResultMap> rows = SqlHistoryDAO.list(limit);
        if (rows == null) return AidaResp.error(request, "히스토리 조회 실패");
        JSONArray arr = new JSONArray();
        for (ResultMap r : rows) {
            arr.put(new JSONObject()
                    .put("id", r.get("id"))
                    .put("sql", r.get("sql") == null ? "" : String.valueOf(r.get("sql")))
                    .put("createdAt", r.get("created_at")));
        }
        return AidaResp.list(request, arr);
    }

    @ControllerMethodInfo(id = "/api/addSqlHistory.do")
    @ApiInfo(summary = "SQL 히스토리 추가", tag = "SQL", method = "POST")
    public String addSqlHistory(HttpSession session, HttpServletRequest request,
                                HttpServletResponse response, Object command) {
        JSONObject body;
        try { body = HttpUtil.getBodyJson(request); } catch (Exception e) { body = null; }
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String sql = body.optString("sql", "");
        if (sql.trim().isEmpty()) return AidaResp.error(request, "sql이 필요합니다");
        SqlHistoryDAO.add(sql);
        return AidaResp.ok(request);
    }

    @ControllerMethodInfo(id = "/api/clearSqlHistory.do")
    @ApiInfo(summary = "SQL 히스토리 초기화", tag = "SQL", method = "POST")
    public String clearSqlHistory(HttpSession session, HttpServletRequest request,
                                  HttpServletResponse response, Object command) {
        SqlHistoryDAO.clear();
        return AidaResp.ok(request);
    }
}
