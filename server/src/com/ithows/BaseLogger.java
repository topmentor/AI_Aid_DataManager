/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows;


import java.util.Enumeration;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 *
 * @author dreamct
 */
public class BaseLogger {

    /**
     * 회원 로그인과 accessLogger에서 사용하는 로그 함수
     * @param target
     * @param msg1
     * @param msg2
     * @param m
     * @param request
     */
    public static String getLog(String msg1, String msg2, ResultMap member, HttpServletRequest request) {
        String requestURI = getCurrentRequestURI(request);
        //매개변수 Member m이 null로 들어오면 SessionInfo에 들어 있는 Member를 이용한다.
        if (member == null) {
            member = getCurrentMember(request);
        }
        String str = String.format("[%s]:[%s], [%s-%s-%s], [%s],[%s],[%s]", msg1, msg2, member.getString("userId"), member.getString("userIName"), member.getString("userCellphone"), request.getRemoteAddr(), requestURI, getParameterString(request));
        return str;
    }
    
    /**
     * request에 들어 있는 parameter들을 하나의 문자열로 묶는 함수
     * @param request
     * @return
     */
    protected static String getParameterString(HttpServletRequest request){
        StringBuilder sb = new StringBuilder();
        Enumeration en = request.getParameterNames();
        while(en.hasMoreElements()){
            String temp = (String)en.nextElement();
            sb.append(temp).append(":").append(request.getParameter(temp)).append(",");
        }
        return sb.toString();
    }
    
    
    /**
     * sms에서 사용하는 log 함수
     * @param target
     * @param msg1
     * @param msg2
     * @param request
     */
    public static String getLog(String msg1, String msg2, HttpServletRequest request) {
        //매개변수 Member m이 null로 들어오면 SessionInfo에 들어 있는 Member를 이용한다.
        ResultMap m = getCurrentMember(request);
        String str = String.format("%s |  %s | %s | %s", msg1, msg2, m.getString("userId"), request.getRemoteAddr());
        return str;
    }
  

    private static String getCurrentRequestURI(HttpServletRequest request) {
        /*실제 접속한 Request URI를 만들기 위해서 PageBean을 이용한다.*/
        Object obj = request.getAttribute("pageBean");
        if (obj != null && obj instanceof PageBean) {
            PageBean pb = (PageBean) obj;
            return pb.getId();
        } else {
            return request.getRequestURI() == null ? "" : request.getRequestURI();
        }
    }

    private static ResultMap getCurrentMember(HttpServletRequest request) {
        Object obj2 = request.getSession().getAttribute("sessionInfo");
        
        // @@ member : 수정해야 함!!!
        
        ResultMap m = null;
        if (obj2 != null && obj2 instanceof SessionInfo) {
            m = ((SessionInfo) obj2).getMember();
        }
        if (m != null) {
            return m;
        } else {
            return new ResultMap();
        }
    }
    //lecLogger.info(id + " | " + request.getRemoteAddr() + " | " + logCode + " | " + logMessage);

    /**
     * 
     * @param msg1
     * @param msg2
     * @param m
     * @param request
     */
//    public static String getLog(String msg1, String msg2, Member m, HttpServletRequest request) {
//        //매개변수 Member m이 null로 들어오면 SessionInfo에 들어 있는 Member를 이용한다.
//        if (m == null) {
//            m = getCurrentMember(request);
//        }
//        String str = String.format("%s | %s | %s | %s", m.getUserId(), request.getRemoteAddr(), msg1, msg2);
//        //Logger.getLogger("lectureLogger").info(str);
//        return str;
//    }
    /**
     * 
     * @param request
     */
    public static String getLog(HttpServletRequest request) {
        ResultMap m = getCurrentMember(request);
        String remoteAddress = request.getRemoteAddr();
        int remotePort = request.getRemotePort();

        Locale locale = request.getLocale();
        String requestURI = getCurrentRequestURI(request);
        String queryString = request.getQueryString() != null ? request.getQueryString() : "NOTHING";
        String browser = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "NOTHING";
        String referer = (request.getHeader("referer") != null) ? request.getHeader("referer") : "NOTHING";
        
        //세션을 구한다.
        HttpSession session = request.getSession();
        String sessionId = session.getId();
        String method = request.getMethod();
        String str = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                sessionId, m.getString("userId"), remoteAddress, remotePort + "",
                locale.toString(), method, requestURI, queryString, referer, browser );
        return str;
    }
}
