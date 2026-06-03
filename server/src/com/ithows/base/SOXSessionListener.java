/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.base;

import com.ithows.HttpUtil;
import com.ithows.ResultMap;
import com.ithows.SessionInfo;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;


/**
 * 전체 접속 세션을 관리하는 클래스
 * - 세션이 생성되면 세션 객체가 생기고 이를 sessionMap으로 관리
 * - 세션에 SessionInfo 객체를 넣어 부가적인 세션 정보를 관리 (SessionInfo.login에서 처리) 
 * @author ksyuser
 */

public class SOXSessionListener implements HttpSessionListener {

    private static Map<String, HttpSession> sessionMap = new HashMap<String, HttpSession>();

    
    public static Map<String, HttpSession> getSessionMap(){
        return sessionMap;
    }
    
    // 세션이 생성될 때 자동 호출 (오버라이딩 함수)
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        sessionMap.put(session.getId(), session);
    }

    // 접속을 
    public static void clearLogoutMessage(String sessionId) {
        if (!sessionId.equals("")) {
            HttpSession session = sessionMap.get(sessionId);
            if (session != null) {
                session.setAttribute("distanceAvailable", "NO");
            }

        }
    }

    // 클라이언트 번호로 세션이 있는 지 확인 (다중 접속을 처리하기 위한 확인 코드)
    // 세션 ID와는 다름
    public static void checkLogin(int myClientNo) {
        Iterator<HttpSession> iter = sessionMap.values().iterator();
        while (iter.hasNext()) {
            HttpSession session = iter.next();
            SessionInfo clientsInfo = HttpUtil.getSessionInfo(session);
            if (clientsInfo == null || clientsInfo.getMember() == null) {
                continue;
            }
            
            ResultMap map = clientsInfo.getMember();
            int clientNo = map.getInt("userNo");
            if (myClientNo == clientNo) {
                session.setAttribute("distanceAvailable", "OK");
                session.removeAttribute("sessionInfo");
                break;
            }
        }
    }

    // 세션이 종료될 때 자동 호출 (오버라이딩 함수)
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();

        //로그인 상태에서 세션이 종료된 경우 로그아웃으로 간주
        SessionInfo clientsInfo = HttpUtil.getSessionInfo(session);
        if (clientsInfo != null && clientsInfo.getLogin()) {
            ResultMap map = clientsInfo.getMember();
            int clientNo = map.getInt("userNo");
            
//            // 로그 아웃 타임 갱신
//            try{
//                
//                UserLogDAO.insertLog(clientNo, "", UserLogDAO.USETYPE_LOGOUT, "세션종료");
//                
//            }catch(Exception e){
//                e.printStackTrace();
//            }
        }


        sessionMap.remove(session.getId());
    }
}