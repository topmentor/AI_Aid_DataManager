/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.base;


import com.ithows.BaseDebug;
import com.ithows.ResultMap;
import com.ithows.HttpUtil;
import com.ithows.SessionInfo;
import com.ithows.dao.UserKeyDAO;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Interceptor — 로그인 확인 및 역할 기반 접근 제어
 * @author dreamct
 */
public class ServiceInterceptor {

    /*일단 여기서 로그인을 체킹한다.*/
    public static String checkLogin(HttpSession session, HttpServletRequest request) {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

        String view = null;
        if (!sInfo.getLogin()) {
            view = "redirect:" + request.getContextPath() + "/login.do";
        }else{
            // 만약 회원의 상태, 유저의 상태에 따라 강제 이동하고 싶다면 아래 부분을 코딩
            // ** 지우지 말것
            /*  //////////////////////////////////////////////////////////////////
            Map map = sInfo.getMemberInfo();
            ResultMap map2 = new ResultMap();
            map2.putAll(map);
            int infoBillingStatus = map2.getInt("infoBillingStatus");
            if (infoBillingStatus == 4 || infoBillingStatus == 5 || infoBillingStatus == 7 || infoBillingStatus == 8) {
                view = "redirect:" + request.getContextPath() + "/billing/billingApproval.do";
            }
            /////////////////////////////////////////////////////////////////////   */
        }

        // null이면 외부의 처리에 맞긴다는 의미이다.
        return view;
    }

    /**
     * 역할 기반 접근 제어 (Role-Based Access Control)
     *
     * ControllerMethodInfo의 loginRequired와 requiredSecurityLevel을 기반으로
     * 현재 사용자의 접근 권한을 확인합니다.
     *
     * @param session  현재 세션
     * @param request  HTTP 요청
     * @param loginRequired  로그인 필요 여부
     * @param requiredSecurityLevel 필요 보안 레벨 (0=모두, 1=General+, 2=Super+, 3=Admin)
     * @return null이면 접근 허용, 문자열이면 리다이렉트 경로
     */
    public static String checkPermission(HttpSession session, HttpServletRequest request,
                                          boolean loginRequired, int requiredSecurityLevel) {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);
        String contextPath = request.getContextPath();

        // 1. 로그인 필요 여부 확인
        if (loginRequired && !sInfo.getLogin()) {
            auditLog(request, "ACCESS_DENIED", "Login required but not logged in");
            return "redirect:" + contextPath + "/login.do";
        }

        // 2. 보안 레벨 확인 (로그인한 사용자만)
        if (requiredSecurityLevel > 0 && sInfo.getLogin()) {
            int userLevel = sInfo.getUserSecurityLevel();
            if (userLevel < requiredSecurityLevel) {
                auditLog(request, "ACCESS_DENIED",
                        "User " + sInfo.getUserId() + " (level=" + userLevel +
                        ") attempted to access resource requiring level=" + requiredSecurityLevel);
                return "redirect:" + contextPath + "/accessDenied.do";
            }
        }

        // null이면 접근 허용
        return null;
    }

    /**
     * API Key 인증 확인.
     *
     * @ApiKeyRequired 가 붙은 컨트롤러 메서드에 대해
     * HTTP 헤더 "X-API-Key" 값을 UserKeyDAO.checkAPIKey() 로 검증한다.
     * 로그인 여부 및 보안 레벨과 독립적으로 동작한다.
     *
     * @param request  HTTP 요청
     * @return null 이면 통과, 문자열이면 실패 사유(에러 메시지)
     */
    public static String checkApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");

        if (apiKey == null || apiKey.isEmpty()) {
            auditLog(request, "ACCESS_DENIED", "Missing X-API-Key header");
            return "Missing API Key";
        }

        if (!UserKeyDAO.checkAPIKey(apiKey)) {
            auditLog(request, "ACCESS_DENIED", "Invalid X-API-Key header");
            return "Invalid API Key";
        }

        return null;
    }

    /**
     * 보안 감사 로그 기록
     *
     * 로그인/로그아웃, 접근 거부, 권한 변경 등 보안 관련 이벤트를 기록합니다.
     *
     * @param request  HTTP 요청
     * @param eventType  이벤트 유형 (LOGIN, LOGOUT, ACCESS_DENIED 등)
     * @param detail  상세 내용
     */
    public static void auditLog(HttpServletRequest request, String eventType, String detail) {
        String clientIp = HttpUtil.getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String sessionId = request.getRequestedSessionId();

        StringBuilder sb = new StringBuilder();
        sb.append("[AUDIT] ");
        sb.append(eventType);
        sb.append(" | IP=").append(clientIp);
        sb.append(" | URI=").append(uri);
        sb.append(" | Method=").append(method);
        sb.append(" | SessionID=").append(sessionId != null ? sessionId.substring(0, Math.min(8, sessionId.length())) + "..." : "none");
        sb.append(" | ").append(detail);

        BaseDebug.info(sb.toString());
    }

}
