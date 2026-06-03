/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

import com.ithows.HttpUtil;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author dreamct
 */
public class Loginer {

    /**
     * 쿠키 로그인을 성공했는지를 나타낸다. 쿠키로 로그인할 때는 비밀번호를 노출시키지 않고 쿠키형태로만 서버에서 읽어들려서 확인해서
     * 로그인 처리한다.
     *
     * @param request
     * @param user
     * @param userNo
     * @return
     */
    public static boolean isSuccessCookieLogin(HttpServletRequest request, String userKey, String passKey, String paramUser) {
        Cookie us = HttpUtil.getCookie(request, userKey); //아이디
        Cookie ps = HttpUtil.getCookie(request, passKey); //패스워드
        
        if (us != null && ps != null) {
            String cookieUser = us.getValue();
            String cookiePass = ps.getValue();
            if(paramUser!=null){
                String paramPass = com.ithows.DirGenerator.getAutoLoginKey(paramUser);
                if (cookieUser.equals(paramUser) && cookiePass.equals(paramPass)) {
                    return true;
                }
            }else{
                String sysPass = com.ithows.DirGenerator.getAutoLoginKey(cookieUser);
                if (cookiePass.equals(sysPass)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 쿠키가 정상적이지 않으면 자동으로 비밀번호 쿠키를 삭제한다.
     *
     * @param request
     * @param response
     */
    public static boolean isValidCookiePassword(HttpServletRequest request, HttpServletResponse response, String userKey, String passKey) {
        Cookie us = HttpUtil.getCookie(request, userKey); //아이디
        Cookie ps = HttpUtil.getCookie(request, passKey); //패스워드
        if (us != null && ps != null) {
            String cookieUser = us.getValue();
            String cookiePass = ps.getValue();
            String sysPass = com.ithows.DirGenerator.getAutoLoginKey(cookieUser);
            if (!cookiePass.equals(sysPass)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 아이디와 아이디를 암호화한 비밀번호인지 확인
     *
     * @param userId
     * @param password
     * @return
     */
    public static boolean isCookieLoginMode(String userId, String password) {
        String encUserId = com.ithows.DirGenerator.getAutoLoginKey(userId);
        if (password.equals(encUserId)) {
            return true; //쿠키로그인으로
        } else {
            return false; //DB로그인으로
        }
    }

    public static void createCookie(HttpServletResponse response, String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60 * 365);//1달
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public static void clearCookie(HttpServletResponse response, String key) {
        Cookie cookie = new Cookie(key, "");
        cookie.setMaxAge(0);//
        cookie.setPath("/");
        response.addCookie(cookie);
    }
    
    public static boolean hasManualLogin(HttpSession session, String key) {
        Object obj = session.getAttribute(key);
        if (obj != null) {
            return true;
        } else {
            return false;
        }
    }

    public static void setManualLogin(HttpSession session, String key, String manualLogin) {
        session.setAttribute(key, manualLogin);
    }

    public static void removeManualLogin(HttpSession session, String key) {
        session.removeAttribute(key);
    }    
}
