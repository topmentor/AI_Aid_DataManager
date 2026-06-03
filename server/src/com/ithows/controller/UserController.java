/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.controller;

import com.ithows.BaseDebug;
import com.ithows.HttpUtil;
import com.ithows.SessionInfo;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.ithows.dao.UserDAO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;



@ControllerClassInfo(controllerPage = "/service/_service.jsp")
public class UserController {

    static {
        BaseDebug.info("----------------------" + ServiceController.class.toString() + " Loading!!");

    }
    
    
    @ControllerMethodInfo(id = "/service/updateUserPassword.do", loginRequired = true)
    @ApiInfo(
        summary = "사용자 비밀번호 변경 (관리자)",
        description = "관리자가 특정 사용자의 비밀번호를 변경합니다.",
        tag = "사용자 관리",
        method = "POST",
        parameters = {
            @ApiInfo.Param(name = "userId", type = "string", description = "관리자 사용자 ID", required = true),
            @ApiInfo.Param(name = "selUserId", type = "integer", description = "대상 사용자 번호", required = true),
            @ApiInfo.Param(name = "newPass", type = "string", description = "새 비밀번호", required = true)
        }
    )
    public String updateAdminPassword(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

        String userIdStr = HttpUtil.getParameterString(request, "userId", "");
        int selUserNo = HttpUtil.getParameterInt(request, "selUserId", 0);
        String newPassStr = HttpUtil.getParameterString(request, "newPass", "");

        System.out.println("selUserNo " + selUserNo);
        System.out.println("newPassStr " + newPassStr);

        if (!UserDAO.checkAdmin(userIdStr)) {
            request.setAttribute("result", "No");
            request.setAttribute("msg", "Not Authorized");
        } else {

            if (UserDAO.updatePassword("", selUserNo, newPassStr) == 1) {

                request.setAttribute("result", "OK");
                request.setAttribute("msg", "Success change password");
            } else {
                request.setAttribute("result", "Error");
                request.setAttribute("msg", "Fail change password");

            }

        }

        return "RESULT_PAGE_JSON";
    }  
}
