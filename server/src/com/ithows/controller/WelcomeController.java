/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.  
 */
package com.ithows.controller;

import com.ithows.BaseDebug;
import com.ithows.HttpUtil;
import com.ithows.ResultMap;
import com.ithows.SessionInfo;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.sox.ltex.CommonDoc;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 *
 * @author home
 * 
 * return에서 지정되는 경로의 root는 '/Coplein/WEB-INF/jsp'(URL)임 프로젝트 상에는  'WebPages/WEB-INF/jsp'(project) 임
 */
@ControllerClassInfo(controllerPage="/_main.jsp")
public class WelcomeController {

    static {
        BaseDebug.info("***WelcomeController.class Loading!!");
    }


    @ControllerMethodInfo(id = "/main.do", loginRequired = true)
    @ApiInfo(
        summary = "메인 페이지",
        description = "로그인 후 메인 페이지를 표시합니다.",
        tag = "페이지",
        method = "GET"
    )
    public String main(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);
        
        String userIdStr = HttpUtil.getParameterString(request, "userId", "");

        if (userIdStr == null || userIdStr.equals("")) {
            userIdStr = "";
        }

        ResultMap userInfo = sInfo.getMember();
        request.setAttribute("userInfo",userInfo);
        request.setAttribute("userId", userIdStr);
        request.setAttribute("maptype", CommonDoc.map_type );
        
        return "/main.jsp";
    }
    
    
     
    @ControllerMethodInfo(id = "/logout.do")
    @ApiInfo(
        summary = "로그아웃",
        description = "현재 세션을 종료하고 로그아웃합니다.",
        tag = "인증",
        method = "GET"
    )
    public String logout(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);
        if (sInfo != null && sInfo.getLogin() == true) {
            sInfo.setLogout();
            session.removeAttribute("sessionInfo");
        }
        
        System.out.println("Logout User");
        
        return "/login.jsp";
    }
    
    @ControllerMethodInfo(id = "/login.do")
    @ApiInfo(
        summary = "로그인",
        description = "사용자 인증을 수행합니다.",
        tag = "인증",
        method = "POST",
        parameters = {
            @ApiInfo.Param(name = "passwd", type = "string", description = "비밀번호", required = true)
        }
    )
    public String login(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);
        
//        String userIdStr = HttpUtil.getParameterString(request, "userId", "");
        String userIdStr = "etriadmin";
        String passwdStr = HttpUtil.getParameterString(request, "passwd", "");

        if (passwdStr == null || passwdStr.equals("")) {
           return "/login.jsp";
        }
        
        ResultMap currentMan = SessionInfo.login(session, request, response, userIdStr, passwdStr);
        
        if(currentMan == null){
            request.setAttribute("result", "NO");
            request.setAttribute("msg", "Login fail");
            return "RESULT_PAGE_JSON";
        }
                
//        Map map = SOXSessionListener.getSessionMap();
//        
//        System.out.println("map >>>  " + map );       
        
      
        request.setAttribute("result", "OK");
        request.setAttribute("msg", userIdStr);
        
        return "RESULT_PAGE_JSON";
    }
    
   
}
