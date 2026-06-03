package com.ithows.controller;

import com.ithows.HttpUtil;
import com.ithows.ResultMap;
import com.ithows.aida.AidaResp;
import com.ithows.aida.SchemaInspector;
import com.ithows.aida.model.SourceRef;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.ithows.dao.CatalogDAO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

/**
 * 스키마/미리보기 API. (기존 IPC: catalog:getSchema / catalog:previewData)
 */
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class SchemaController {

    @ControllerMethodInfo(id = "/api/getSchema.do")
    @ApiInfo(summary = "스키마 조회", tag = "Schema", method = "POST")
    public String getSchema(HttpSession session, HttpServletRequest request,
                            HttpServletResponse response, Object command) {
        SourceRef s = resolve(request);
        if (s == null) return AidaResp.error(request, "소스를 찾을 수 없습니다(id 확인)");
        try {
            org.json.JSONObject schema = SchemaInspector.inspect(s);
            schema.put("sourceId", s.id);
            schema.put("sourceName", s.name);
            return AidaResp.map(request, schema);
        } catch (Exception e) {
            return AidaResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/previewData.do")
    @ApiInfo(summary = "데이터 미리보기", tag = "Schema", method = "POST")
    public String previewData(HttpSession session, HttpServletRequest request,
                              HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        SourceRef s = resolveFrom(body);
        if (s == null) return AidaResp.error(request, "소스를 찾을 수 없습니다(id 확인)");
        int limit = body.optInt("limit", 50);
        try {
            return AidaResp.map(request, SchemaInspector.preview(s, limit));
        } catch (Exception e) {
            return AidaResp.error(request, e.getMessage());
        }
    }

    private SourceRef resolve(HttpServletRequest request) {
        JSONObject body = bodyOrNull(request);
        return body == null ? null : resolveFrom(body);
    }

    private SourceRef resolveFrom(JSONObject body) {
        String id = body.optString("id", "").trim();
        if (id.isEmpty()) return null;
        ResultMap row = CatalogDAO.get(id);
        return row == null ? null : SourceRef.from(row);
    }

    private static JSONObject bodyOrNull(HttpServletRequest request) {
        try { return HttpUtil.getBodyJson(request); } catch (Exception e) { return null; }
    }
}
