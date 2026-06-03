/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.  
 */
package com.ithows.controller;

import com.ithows.AppConfig;
import com.ithows.BaseDebug;
import com.ithows.HttpUtil;
import com.ithows.JdbcDao;
import com.ithows.JdbcDao2;
import com.ithows.PageBean;
import com.ithows.ResultMap;
import com.ithows.SessionInfo;
import com.ithows.base.ApiInfo;
import com.ithows.base.ApiKeyRequired;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.ithows.service.FileManager;
import com.ithows.service.OndeviceModelFunctions;
import com.ithows.service.UploadConst;
import com.ithows.util.DBUtils;
import com.ithows.util.DateTimeUtils;
import com.ithows.util.NetUtils;
import com.ithows.util.UtilFile;
import com.ithows.util.UtilJSON;
import com.ithows.util.UtilString;
import com.sox.ltex.CommonDoc;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author home
 *
 * return에서 지정되는 경로의 root는 '/ITHowsWeb/WEB-INF/jsp'(URL)임 프로젝트 상에는
 * 'WebPages/WEB-INF/jsp'(project) 임
 */
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class APIController {

    static {
            BaseDebug.info("----------------------" + APIController.class.toString() + " Loading!!");

    }

    //<editor-fold desc="Hello World API (X-API-Key 인증)">
    ////////////////////////////////////////////////////////////////////////
    // Hello World — X-API-Key 헤더 인증이 적용된 예시 엔드포인트

    @ControllerMethodInfo(id = "/api/helloWorld.do")
    @ApiKeyRequired
    @ApiInfo(
        summary = "Hello World",
        description = "X-API-Key 헤더 인증이 적용된 예시 엔드포인트. 유효한 API Key가 있을 때만 \"Hello World\"를 반환한다.",
        tag = "API",
        method = "GET",
        responseDescription = "data 필드에 \"Hello World\" 문자열이 포함된 JSON 응답"
    )
    public String helloWorld(HttpSession session, HttpServletRequest request,
                             HttpServletResponse response, Object command) throws Exception {

        request.setAttribute("result", "OK");
        request.setAttribute("msg", "Success");
        request.setAttribute("data", "Hello World");

        return "RESULT_PAGE_JSON";
    }

    //</editor-fold>
}
