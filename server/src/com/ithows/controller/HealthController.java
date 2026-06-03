/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ithows.controller;

import com.ithows.BaseDebug;
import com.ithows.HttpUtil;
import com.ithows.JdbcDao;
import com.ithows.JdbcDao2;
import com.ithows.SessionInfo;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

/**
 *
 * @author mailt
 */
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class HealthController {
    static {    
            BaseDebug.info("----------------------" + HealthController.class.toString() + " Loading!!");

    }
    
    
    //<editor-fold desc="Health API">
    ////////////////////////////////////////////////////////////////////////
    // Health API (외부 API)

    @ControllerMethodInfo(id = "/api/checkHealth.do")
    @ApiInfo(summary = "서비스 상태 확인", description = "서비스 정상 동작 여부를 확인합니다.", tag = "Health Check", method = "GET")
    public String checkHealth(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

                
        JSONObject jObj = new JSONObject();
        jObj.put("type", "service");
        jObj.put("status", "OK" );

        request.setAttribute("result", jObj.toString());

        return "RESULT_SIMPLE_JSON";

    }

    
    @ControllerMethodInfo(id = "/api/checkDB.do")
    @ApiInfo(summary = "DB 연결 상태 확인", description = "DB1, DB2 연결 상태를 확인합니다.", tag = "Health Check", method = "GET")
    public String checkDB(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

        int dbReact1 = 0;
        
        try {
            dbReact1 = JdbcDao.queryForInt("SELECT 1 ", new Object[]{}); 
        } catch (SQLException e) {
            
        }
        
        int dbReact2 = 0;
        
        try {
            dbReact2 = JdbcDao2.queryForInt("SELECT 1 ", new Object[]{}); 
        } catch (SQLException e) {
        }
        
                
        JSONObject jObj = new JSONObject();
        jObj.put("type", "db");
        jObj.put("status", (dbReact1 == 1 && dbReact2 == 1 )? "OK" : "Error"  );
        jObj.put("db1", dbReact1 == 1 ? "OK" : "Error"  );
        jObj.put("db2", dbReact2 == 1 ? "OK" : "Error"  );

        request.setAttribute("result", jObj.toString());

        return "RESULT_SIMPLE_JSON";

    }

    @ControllerMethodInfo(id = "/api/checkAllInfo.do")
    @ApiInfo(summary = "전체 시스템 정보 확인", description = "OS, 서버 주소, DB 연결 등 전체 시스템 상태를 확인합니다.", tag = "Health Check", method = "GET")
    public String checkAllInfo(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

        String OS = System.getProperty("os.name").toLowerCase();
        String localAddr = request.getLocalAddr();

        int dbReact1 = 0;
        
        try {
            dbReact1 = JdbcDao.queryForInt("SELECT 1 ", new Object[]{}); 
        } catch (SQLException e) {
        }
        
        int dbReact2 = 0;
        
        try {
            dbReact2 = JdbcDao2.queryForInt("SELECT 1 ", new Object[]{}); 
        } catch (SQLException e) {
        }
        
                
        JSONObject jObj = new JSONObject();
        jObj.put("os", OS);
        jObj.put("address", localAddr);
        jObj.put("service", "OK");
        jObj.put("db", (dbReact1 == 1 && dbReact2 == 1 )? "OK" : "Error"  );
        jObj.put("db1", dbReact1 == 1 ? "OK" : "Error"  );
        jObj.put("db2", dbReact2 == 1 ? "OK" : "Error"  );
 
        

        request.setAttribute("result", jObj.toString());

        return "RESULT_SIMPLE_JSON";
    }

    
    //</editor-fold>
}
