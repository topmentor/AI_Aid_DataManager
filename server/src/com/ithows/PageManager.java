/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows;

import com.ithows.base.TemplateBean;
import com.ithows.util.DateTimeUtils;
import java.io.FileWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import com.sox.ltex.CommonDoc;
import static com.sox.ltex.CommonDoc.devLogPath;
import org.json.JSONException;
import org.json.JSONObject;


/**
 *
 * @author home
 */
public class PageManager {

    static {
        BaseDebug.info("***PageManager Loadiang!!");
    }

    /**
     * 호출할 함수 이름을 추출한다.
     * @param url
     * @return
     */
    protected static String getMethodName(String url) {
        String str = url;
        int start = str.lastIndexOf("/") + 1;
        int end = str.length() - 3;
        return str.substring(start, end);
    }

    /**
     * URL에 접속하면 컨트롤러를 검색한 후 컨트롤러의 함수를 호출해서 view 페이지를 리턴한다.
     * 함수의 이름은 접속 URL의 파일명을 이용한다. 이 때 .do를 제외한 파일명이다.
     * 확장자를 제외한 파일명을 가진 메소드를 컨트롤러에서 호출한 후 결과로 view 페이지를 리턴한다.
     * @param pageContext 
     * @param session
     * @param response
     * @param request 
     * @return  
     */
    public static String callController(PageContext pageContext, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        PageBean pageBean = (PageBean) request.getAttribute("pageBean");
        String pageId = pageBean.getId();
        Object obj = pageBean.getController();

        String view = null;
        if (obj == null) {   //컨트롤러가 등록되어 있지 않다면 디폴트 jsp 페이지로 이동한다.
            view = pageId.replace(".do", ".jsp");
            
        } else {    //컨트롤러가 등록되어 있다면 함수의 이름 규칙을 이용해서 함수를 호출한다.
            String methodName = getMethodName(pageId);
            //String methodName = pageBean.getMethodName();
            
            try {
                Method m = obj.getClass().getMethod(methodName, HttpSession.class, HttpServletRequest.class, HttpServletResponse.class, Object.class);
                Object result = m.invoke(obj, session, request, response, request.getAttribute(pageBean.getCommandName()));
                
                // Controller가 리턴하는 스트링 
                if (result != null) {
                    view = (String) result;
                }
                //@aaa_Logger.getLogger("accessLogger").info(DreamCTLogger.getLog("정상", obj.getClass().getSimpleName() + ", " + m.getName(), null, request));

            } catch (NoSuchMethodException e) {//컨트롤러를 못찾았을 경우
                System.out.println(BaseLogger.getLog("정상", obj.getClass().getSimpleName() + ", Default 함수", null, request));
                view = pageId.replace(".do", ".jsp");//2010-08-14 컨트롤러에 함수가 없어도 디폴트로 가야 한다.

            } catch (Exception e2) {
                System.out.println(BaseLogger.getLog("Error ", "심각", null, request));
                BaseDebug.log(e2, "잘못된 접근입니다.[ " + methodName  + " ] ");
            }
        }
        
        return view;
    }

    /**
     * view Page의 이동을 제어한다.
     * @param view
     * @param pageContext
     * @param request
     * @param response
     */
    public static void moveViewPage(String view, PageContext pageContext, HttpServletRequest request, HttpServletResponse response) {
        try {
            PageBean pageBean = (PageBean) request.getAttribute("pageBean");
            
            if (view == null) {  
                
                //0. Controller 메소드 실행시 예외상황 발생으로 jsp 파일 연결(return jsp)이 안 된 것 
                // static 클래스 메소드 실행시 예외가 발생 할 때에도 jsp 파일 연결이 안 된다.
                // 여기를 거치는 것은 결국 서버상에 발생하는 런타임 예외 때문에 그렇다

                System.out.println("Error Call ------------- " + pageBean.getId());
                
                
                JSONObject jParam = null;
                
                // 서비스 로그 파일에 에러 저장 
                String buf = "";
                buf += "[Error : " + DateTimeUtils.getTimeDateNow() + " ] ------------------------------ \n";
                buf += "Request URI :  " + request.getRequestURI() + "\n" ;
                buf+= "Remote Host :  " + request.getRemoteHost() + "\n" ;
                buf+= "Call API : " + pageBean.getId()  + "\n";
                buf+="Method : " + request.getMethod()  + "\n";
                buf+= "SessionID : " + request.getRequestedSessionId()  + "\n";
                buf+= "Query String : " + request.getQueryString()  + "\n";
                                
                if( pageBean.getId().contains("getPosition.do")){
                    
                    try {
                        jParam = HttpUtil.getBodyJson(request);
                        buf+= "Body : " +  jParam.toString()  + "\n" ;
                        
                    } catch (Exception e) {
                        jParam = null;
                    }
                }else{
                    
                    buf+= "Body : " +   HttpUtil.getBody(request)  + "\n" ;
                }
                
                buf+= "=========================================================\n\n";
                
                writeTextToLogFile(buf.toString());
                System.out.println(buf);
                
                // @@ Error 메세지 
                request.setAttribute("result", 999);
                request.setAttribute("msg", "DB Error or Service Exception");
                request.setAttribute("lat", 36.433954);
                request.setAttribute("lng", 127.390344);
                request.setAttribute("time", DateTimeUtils.getTimestampNow());
                request.setAttribute("response_time_text", DateTimeUtils.getTimeDateNow());
                request.setAttribute("posmethod", "FUSED");
                request.setAttribute("longitude_coarse", 127.390344);
                request.setAttribute("latitude_coarse", 36.433954);
                request.setAttribute("altitude", 0);
                request.setAttribute("accuracy_v", 0);
                request.setAttribute("accuracy_h", 0);
                request.setAttribute("floor", 1);      
                request.setAttribute("res_cellidtype", 1);
                request.setAttribute("session_id", "nothing");

                if(jParam != null){
                    long collect_time = 0;
                    long request_time = 0;
                    String request_time_text = "";
                    String collect_time_text = "";

                    try{
                        if(jParam.has("collect_time")){
                            collect_time = jParam.getLong("collect_time");
                            collect_time_text = DateTimeUtils.convertTimestampToDate(collect_time);
                            request.setAttribute("collect_time", collect_time);
                            request.setAttribute("collect_time_text", collect_time_text);
                        }

                        if(jParam.has("request_time")){
                            request_time = jParam.getLong("request_time");
                            request_time_text = DateTimeUtils.convertTimestampToDate(request_time);
                            request.setAttribute("request_time", request_time);
                            request.setAttribute("request_time_text", request_time_text);
                        }
                    }catch(JSONException je){
                        
                    }

                }else{
                    request.setAttribute("collect_time", -1);
                    request.setAttribute("collect_time_text", "");
                    request.setAttribute("request_time", -1);
                    request.setAttribute("request_time_text", "");                          
                }
                
                pageContext.forward("/WEB-INF/jsp/getPosition_horizontal_II1.jsp");  
//                pageContext.forward("/WEB-INF/jsp/simpleResultJson.jsp");  

            
                
            } else if (view.startsWith("redirect")) {//1. Redirect
                response.sendRedirect(view.replace("redirect:", ""));
                
            } else if (view.equals("RESULT_PAGE")) {//2. 디자인이 없는 경우
                pageContext.forward("/WEB-INF/jsp/simpleResult.jsp");
//                pageContext.forward("/WEB-INF/jsp/template/simpleResult.jsp");

            } else if (view.equals("RESULT_PAGE_JSON")) {//2.1 디자인이 없는 경우
                pageContext.forward("/WEB-INF/jsp/simpleResultJson.jsp");      
                
            } else if (view.equals("RESULT_SIMPLE_JSON")) {// simpleResultJson.jsp
                pageContext.forward("/WEB-INF/jsp/simpleResultJson2.jsp");                     
                
            } else if (view.equals("RESULT_COMMON_JSON")) {//2.2 디자인이 없는 경우  (ResultMap 매칭)
                pageContext.forward("/WEB-INF/jsp/commonResultJson.jsp");   
                
            } else if (view.equals("RESULT_RAW_JSON")) { //  commonResultRawJson.jsp
                pageContext.forward("/WEB-INF/jsp/commonResultRawJson.jsp");                   
                
            } else if (view.equals("RESULT_LIST_JSON")) {//2.3 디자인이 없는 경우 (ResultMap List 매칭)
                pageContext.forward("/WEB-INF/jsp/commonListJson.jsp");      
                
            } else if (view.equals("RESULT_CHART_JSON")) {//2.4 디자인이 없는 경우 (차트 매칭)
                pageContext.forward("/WEB-INF/jsp/chartResultJson.jsp");      
                
            } else if (view.equals("NO_PAGE")) {//3. 페이지가 없는 경우
                
            } else if (pageBean.getTemplate() != null) {//4. pageBean.getTemplate()을 얻는다. main이다.
                TemplateBean tempBean = pageBean.getTemplate(); //main이 디폴트로 걸리는 구나~~
                request.setAttribute("top", "/WEB-INF/jsp" + tempBean.getTop());
                request.setAttribute("bottom", "/WEB-INF/jsp" + tempBean.getBottom());
                request.setAttribute("view", "/WEB-INF/jsp" + view);
                pageContext.forward("/WEB-INF/jsp" + tempBean.getTemplatePage());
            
                
            } else {   
                
                if(view.endsWith(".jsp")){  //5. 정상적인 jsp 파일의 경로
                    pageContext.forward("/WEB-INF/jsp" + view);
                    
                    
                }else{ // 6. 일반 스트링
                    
                    response.setContentType("text/html");
                    response.setCharacterEncoding("utf-8");

                    PrintWriter writer = response.getWriter();
                    writer.println(view);
                }
                
            }
            
            
        } catch (IOException e) {
            BaseDebug.log(e, "redirect view 페이지를 확인하십시오. ", view);
            
        } catch (ServletException e2) {
            BaseDebug.log(e2, "view 페이지를 확인하십시오. ", view);
        }
    }
    
    
    private static boolean writeTextToLogFile(String saveStr) {
        
        String fileName = "./server_error.log";
        
        String OS = System.getProperty("os.name").toLowerCase();
        if(OS.indexOf("win") >= 0){ 
           fileName = AppConfig.getConf("context_win_dir") + AppConfig.getConf("config_errorlog_path") + "server_error.log" ;

        }else{
           fileName = AppConfig.getConf("context_dir") + AppConfig.getConf("config_errorlog_path") + "server_error.log" ;
        } 
        
        try (FileWriter writer = new FileWriter(fileName, true)) {
            writer.append(saveStr);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
