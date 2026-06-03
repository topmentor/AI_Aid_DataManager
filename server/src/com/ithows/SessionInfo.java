/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows;

import com.ithows.base.SOXSessionListener;
import com.ithows.dao.UserDAO;
import com.ithows.util.OneTimePassword;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author dreamct2
 */
public class SessionInfo {

    private ResultMap member = null;
    private ResultMap connectInfo = null;  /*Member의 권한 정보나 기타 정보를 보관하는 곳*/

    public ResultMap getMember() {
        return member;
    }
    
    public ResultMap getConnectInfo() {
        return connectInfo;
    }

    /**
     * 로그인시 세션관련 정보를 불러오고 셋팅.
     * 유저정보는 login에서 UserDAO.getLoginUser(userId, Password)()로 읽어와서 이 함수의 인자로 전달된다.
     * @param memberData 
     */
    public void setLogin(ResultMap memberData) {
        this.member = memberData;
        this.connectInfo = new ResultMap();
    }

    
    // <editor-fold defaultstate="collapsed"  desc="회원정보 관리">
    public boolean getLogin() { // 로그인 여부
        if (member != null) {
            return true;
        } else {
            return false;
        }
    }
  

    public void setLogout() {
        this.member = null;
        this.connectInfo = null;
    }

    // 로그인 확인 후 세션에 유저 객체 등록
    public static ResultMap login(HttpSession session, HttpServletRequest request, HttpServletResponse response, String userId, String Password) throws SQLException {
        
        ResultMap curClient = UserDAO.getLoginUser(userId, Password);

        if(curClient == null){
            return null;
        }
        
        // 접속IP 로그에 저장
        String host = request.getRemoteHost(); 
        String ip = HttpUtil.getClientIp(request);

        curClient.put("userHost", host);
        curClient.put("userIp", ip);
        

        /////////////////////////////////////////////////////////////////
        // 다중 로그인을 막기 위한 세션 초기화
        // SOXSessionListener.checkLogin(curClient.getInt("userNo"));
        // SOXSessionListener.clearLogoutMessage(session.getId());
        ////////////////////////////////////////////////////////////////
        
        SessionInfo clientsInfo = new SessionInfo();
        clientsInfo.setLogin(curClient);

        session.setAttribute("sessionInfo", clientsInfo);

        return curClient;

    }
    

    public int getUserNo() {
        if (this.member != null) {
            return this.member.getInt("userNo");
        } else {
            return 0;
        }
    }

    public String getUserId() {
        if (this.member != null) {
            return this.member.getString("userId");
        } else {
            return "";
        }
    }

    public int getUserSecurityLevel() {
        return this.member.getInt("userSecurityLevel");
    }
    // </editor-fold> 
    
    
    // <editor-fold defaultstate="collapsed"  desc="OneTimePassword를 위한 번호">
    private OneTimePassword oneTimePassword = null;

    public OneTimePassword getOneTimePassword() {
        return oneTimePassword;
    }

    public void setOneTimePassword(OneTimePassword oneTimePassword) {
        this.oneTimePassword = oneTimePassword;
    }
    // </editor-fold> 
}
