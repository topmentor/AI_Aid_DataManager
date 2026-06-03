package com.ithows.controller;

import com.ithows.HttpUtil;
import com.ithows.aida.AidaResp;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.ithows.dao.SettingsDAO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

/**
 * 앱 설정 API. (기존 IPC: settings:get / settings:set)
 */
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class SettingsController {

    @ControllerMethodInfo(id = "/api/getSettings.do")
    @ApiInfo(summary = "설정 조회", description = "claudeBin/pythonBin/workspaceRoot 등 앱 설정을 반환합니다.",
             tag = "Settings", method = "GET")
    public String getSettings(HttpSession session, HttpServletRequest request,
                              HttpServletResponse response, Object command) {
        return AidaResp.map(request, AidaResp.toJson(SettingsDAO.getAll()));
    }

    @ControllerMethodInfo(id = "/api/setSettings.do")
    @ApiInfo(summary = "설정 저장", description = "부분 설정 JSON을 병합 저장한 뒤 전체 설정을 반환합니다.",
             tag = "Settings", method = "POST")
    public String setSettings(HttpSession session, HttpServletRequest request,
                              HttpServletResponse response, Object command) {
        JSONObject body;
        try { body = HttpUtil.getBodyJson(request); } catch (Exception e) { body = null; }
        if (body == null) {
            return AidaResp.error(request, "요청 본문(JSON)이 필요합니다");
        }
        SettingsDAO.putAll(body);
        return AidaResp.map(request, AidaResp.toJson(SettingsDAO.getAll()));
    }
}
