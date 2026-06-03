package com.ithows.controller;

import com.ithows.HttpUtil;
import com.ithows.ResultMap;
import com.ithows.aida.AidaResp;
import com.ithows.aida.ConnTester;
import com.ithows.aida.model.SourceRef;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.ithows.dao.CatalogDAO;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 데이터 소스 카탈로그 API.
 * (기존 IPC: catalog:list/add/update/remove/testConnection)
 */
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class CatalogController {

    @ControllerMethodInfo(id = "/api/listSources.do")
    @ApiInfo(summary = "데이터 소스 목록", tag = "Catalog", method = "GET")
    public String listSources(HttpSession session, HttpServletRequest request,
                              HttpServletResponse response, Object command) {
        List<ResultMap> rows = CatalogDAO.list();
        if (rows == null) return AidaResp.error(request, "카탈로그 조회 실패");
        JSONArray arr = new JSONArray();
        for (ResultMap r : rows) arr.put(SourceRef.from(r).toJson());
        return AidaResp.list(request, arr);
    }

    @ControllerMethodInfo(id = "/api/addSource.do")
    @ApiInfo(summary = "데이터 소스 추가", tag = "Catalog", method = "POST")
    public String addSource(HttpSession session, HttpServletRequest request,
                            HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String name = body.optString("name", "").trim();
        String type = body.optString("type", "").trim();
        if (name.isEmpty() || type.isEmpty()) return AidaResp.error(request, "name/type이 필요합니다");
        JSONObject config = body.optJSONObject("config");
        String id = CatalogDAO.add(name, type, (config == null ? new JSONObject() : config).toString());
        if (id == null) return AidaResp.error(request, "추가 실패");
        ResultMap row = CatalogDAO.get(id);
        return AidaResp.map(request, SourceRef.from(row).toJson());
    }

    @ControllerMethodInfo(id = "/api/updateSource.do")
    @ApiInfo(summary = "데이터 소스 수정", tag = "Catalog", method = "POST")
    public String updateSource(HttpSession session, HttpServletRequest request,
                               HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String id = body.optString("id", "").trim();
        if (id.isEmpty()) return AidaResp.error(request, "id가 필요합니다");
        String name = body.optString("name", "").trim();
        String type = body.optString("type", "").trim();
        JSONObject config = body.optJSONObject("config");
        int n = CatalogDAO.update(id, name, type, (config == null ? new JSONObject() : config).toString());
        if (n <= 0) return AidaResp.error(request, "수정 실패(대상 없음)");
        return AidaResp.map(request, SourceRef.from(CatalogDAO.get(id)).toJson());
    }

    @ControllerMethodInfo(id = "/api/removeSource.do")
    @ApiInfo(summary = "데이터 소스 삭제", tag = "Catalog", method = "POST")
    public String removeSource(HttpSession session, HttpServletRequest request,
                               HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String id = body.optString("id", "").trim();
        if (id.isEmpty()) return AidaResp.error(request, "id가 필요합니다");
        int n = CatalogDAO.remove(id);
        return n > 0 ? AidaResp.ok(request) : AidaResp.no(request, "대상이 없습니다");
    }

    @ControllerMethodInfo(id = "/api/testConnection.do")
    @ApiInfo(summary = "연결 테스트", tag = "Catalog", method = "POST")
    public String testConnection(HttpSession session, HttpServletRequest request,
                                 HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        String id = body.optString("id", "").trim();
        if (id.isEmpty()) return AidaResp.error(request, "id가 필요합니다");
        ResultMap row = CatalogDAO.get(id);
        if (row == null) return AidaResp.error(request, "소스를 찾을 수 없습니다");
        return AidaResp.map(request, ConnTester.test(SourceRef.from(row)));
    }

    private static JSONObject bodyOrNull(HttpServletRequest request) {
        try { return HttpUtil.getBodyJson(request); } catch (Exception e) { return null; }
    }
}
