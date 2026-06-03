/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.controller;

import com.ithows.AppConfig;
import com.ithows.BaseDebug;
import com.ithows.HttpUtil;
import com.ithows.ResultMap;
import com.ithows.SessionInfo;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@ControllerClassInfo(controllerPage = "/service/_service.jsp")
public class ServiceController {

    static {
        BaseDebug.info("----------------------" + ServiceController.class.toString() + " Loading!!");

    }
   
    
    
}
