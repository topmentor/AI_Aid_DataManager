package com.ithows.base;

import com.google.common.base.Predicates;
import com.ithows.AppConfig;
import com.ithows.AppConfig;
import com.ithows.HttpUtil;
import com.ithows.BaseDebug;
import com.ithows.BaseDebug;
import com.ithows.BaseLogger;
import com.ithows.BaseLogger;
import com.ithows.HttpUtil;
import com.ithows.PageBean;
import com.ithows.PageBean;
import com.ithows.SessionInfo;
import com.ithows.SessionInfo;
import com.ithows.base.ApiKeyRequired;
import com.ithows.base.CommandManager;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.ithows.base.PageBeanContainer;
import com.ithows.base.TemplateBean;
import com.ithows.base.TemplateContainer;
import com.ithows.util.DateTimeUtils;
import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;
import javax.servlet.*;
import javax.servlet.http.*;
//import org.w3c.dom.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;
import javax.xml.parsers.*;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

/**
 * 클라이언트의 요청을 전달받는다. 컨트롤러에게 클라이언트의 요청을 전달하고 컨트롤러가 반환한 결과 값을 분석하여 알맞은 응답을 생성하도록
 * 한다.
 *
 * @author dreamct2
 */
public class DispatcherServlet extends HttpServlet {

    static {
        try {
            Class.forName("com.ithows.AppConfig");

        } catch (ClassNotFoundException ex) {
            BaseDebug.log(ex, "App1과 App2를 로딩할 수 없습니다.");
        }
    }
    private PageBeanContainer container = null;

    public void init(ServletConfig config) throws ServletException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);

        try {
//            String fileName = config.getServletContext().getRealPath("/") + "WEB-INF\\dispatcher-servlet.xml";
            String fileName = config.getServletContext().getRealPath("/") + "WEB-INF/dispatcher-servlet.xml";
            DocumentBuilder dBuilder = factory.newDocumentBuilder();
            Document document = dBuilder.parse(fileName);
            BaseDebug.info("--------------dispacher-servlet.xml 로딩 완료---------------------");

            if (this.container == null) {
                this.container = new PageBeanContainer();
            }
            this.container.clear();//모든 요소를 제거한다.

            // 레이아웃 템플릿을 담는다.
            NodeList tmpList = document.getElementsByTagName("template");
            TemplateContainer tmpContainer = new TemplateContainer();
            if (tmpList != null) {
                for (int i = 0; i < tmpList.getLength(); i++) {
                    Node templateNode = tmpList.item(i);
                    TemplateBean tmpBean = new TemplateBean();
                    tmpBean.setId(templateNode.getAttributes().getNamedItem("id").getNodeValue());
                    tmpBean.setTop(templateNode.getAttributes().getNamedItem("top").getNodeValue());
                    tmpBean.setBottom(templateNode.getAttributes().getNamedItem("bottom").getNodeValue());
                    tmpBean.setTemplatePage(templateNode.getAttributes().getNamedItem("templatePage").getNodeValue());
                    tmpContainer.add(tmpBean.getId(), tmpBean);
                    System.out.println("[" + i + "] Design Templete : " + tmpBean.getId());
                }
            }//controller

            HashMap<Class, Object> ctrlContainer = new HashMap<Class, Object>();

            // 컨트롤러에 있는 어노테이션을 읽어 처리
            Reflections reflections = new Reflections("com.ithows.controller");
            Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(ControllerClassInfo.class);
            String controllerPage = "";
            ControllerClassInfo cci = null;

            for (Class<?> c : annotated) {

                try {
                    cci = c.getAnnotation(ControllerClassInfo.class);
                    controllerPage = cci.controllerPage();
                } catch (Exception e) {
                }
                if (!ctrlContainer.containsKey(c)) {
                    try {
                        Object ctrlObject = c.newInstance();//Controller 객체 설정
                        ctrlContainer.put(c, ctrlObject);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Set<Method> mset = reflections.getAllMethods(c, Predicates.and(ReflectionUtils.withAnnotation(ControllerMethodInfo.class)));

                for (Method m : mset) {
                    ControllerMethodInfo cm = m.getAnnotation(ControllerMethodInfo.class);

                    PageBean pageBean = new PageBean();
                    pageBean.setId(cm.id());
                    pageBean.setCommandName(cm.commandName());
                    pageBean.setCommandClass(cm.commandClass());
                    pageBean.setVersion(cm.version());
                    pageBean.setLoginRequired(cm.loginRequired());
                    pageBean.setRequiredSecurityLevel(cm.requiredSecurityLevel());
                    pageBean.setApiKeyRequired(m.isAnnotationPresent(ApiKeyRequired.class));

                    //pageBean.setMethodName(m.getName());/*Controller Method Name is saved*/
                    String cPage = cm.controllerPage();
                    if (!cPage.isEmpty()) {
                        controllerPage = cPage;
                    }
                    pageBean.setControllerPage(controllerPage);//Controller Page 설정

                    try {
                        String ctrlClass = cm.controllerClass();
                        if (!ctrlClass.isEmpty() && !ctrlContainer.containsKey(c)) {//Controller Object do not exist
                            Class tmpClass = Class.forName(ctrlClass);
                            Object ctrlObject = tmpClass.newInstance();
                            ctrlContainer.put(tmpClass, ctrlObject);
                            pageBean.setController(ctrlContainer.get(tmpClass));//Set Controller Object
                        } else { //Controller Object do exist
                            pageBean.setController(ctrlContainer.get(c));//Set Controller Object
                        }
                    } catch (Exception e) {
                        BaseDebug.log(e, "Can not create and load Controller Instance!");
                    }
//                    String template = cm.template();
//                    if (!template.isEmpty()) {
//                        pageBean.setTemplate(tmpContainer.get(template));
//                    }
                    //이미 키를 포함하고 있다면
                    if (container.containsKey(pageBean.getId())) {
                        PageBean old = container.get(pageBean.getId());
                        if (old.getVersion() < pageBean.getVersion()) { //새로운 버전이다면 집어 넣는다.
                            container.add(pageBean.getId(), pageBean);
                        }
                    } else { //새로운 키라면
                        container.add(pageBean.getId(), pageBean);
                    }
                    System.out.println(pageBean);
                }
            }

            BaseDebug.info("***DispacherServlet Initializing OK");
        } catch (Exception ex) {
//        } catch (ParserConfigurationException ex) {
//            throw new ServletException(ex + "DispatcherServlet.java  Parsing 에러 dispatcher-servlet.xml을 확인하십시오");
//        } catch (SAXException ex) {
//            throw new ServletException(ex + "DispatcherServlet.java  Parsing 에러 dispatcher-servlet.xml을 확인하십시오");
//        } catch (IOException ex) {
//            throw new ServletException(ex + "DispatcherServlet.java  입출력 에러 dispatcher-servlet.xml을 확인하십시오");
            throw new ServletException(ex + "확인하십시오");
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException , IOException {
        SessionInfo sInfo = HttpUtil.getSessionInfo(request.getSession());
        request.setAttribute("servletPath", request.getContextPath());

        // 보안 헤더는 SecurityFilter에서 일괄 처리됨

        String cmd = request.getRequestURI();
        cmd = cmd.substring(request.getContextPath().length());
        PageBean pb = container.get(cmd); //list.do에 해당하는 데이터를 얻어낸다.
        String view = "";
        if (pb != null) {

            // ── API Key 검증 (@ApiKeyRequired) — 로그인과 독립적으로 먼저 처리 ──
            if (pb.isApiKeyRequired()) {
                String apiKeyError = ServiceInterceptor.checkApiKey(request);
                if (apiKeyError != null) {
                    request.setAttribute("result", "ERROR");
                    request.setAttribute("msg", apiKeyError);
                    request.setAttribute("restime", DateTimeUtils.getTimeDateNow());
                    response.setContentType("application/json;charset=UTF-8");
                    RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/jsp/api/resultJson.jsp");
                    rd.forward(request, response);
                    return;
                }
            }

            // ── 권한 체크 (loginRequired, requiredSecurityLevel) ──
            if (pb.isLoginRequired() || pb.getRequiredSecurityLevel() > 0) {
                String permissionView = ServiceInterceptor.checkPermission(
                        request.getSession(), request,
                        pb.isLoginRequired(), pb.getRequiredSecurityLevel());
                if (permissionView != null) {
                    // 접근 거부 — 리다이렉트
                    if (permissionView.startsWith("redirect:")) {
                        response.sendRedirect(permissionView.replace("redirect:", ""));
                    } else {
                        RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/jsp" + permissionView);
                        rd.forward(request, response);
                    }
                    return;
                }
            }

            view = pb.getControllerPage().substring(0, pb.getControllerPage().length() - 4); //예를 들어 bbs.do에서 bbs를 추출한다.
            request.setAttribute("pageBean", pb);
            /*
             * Command Data를 분석해서 Command 객체로 만든다. 그리고 내부에서 Command 객체를 request에
             * 등록한다.
             */
            if (pb.getCommandClass() != null && !pb.getCommandClass().equals("")) {
                CommandManager cm = new CommandManager();
                cm.setCommandClass(pb.getCommandClass());
                cm.setCommandName(pb.getCommandName());
                cm.setRequestResponse(request, response);
            }
        } else {
            view = cmd.substring(0, cmd.length() - 3);
        }
        view = view + ".jsp";
        String loginStatus = AppConfig.getConf("log_siteLogger");
        if (loginStatus != null && loginStatus.equals("on")) {
            System.out.println(BaseLogger.getLog(request));
        }

        // CSRF 토큰을 request attribute로 전달 (JSP에서 사용)
        request.setAttribute("_csrf", SecurityFilter.getCsrfToken(request.getSession(false)));

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/jsp" + view);
        dispatcher.forward(request, response);

    }

    public static String getBody(HttpServletRequest request) throws IOException {

        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        body = stringBuilder.toString();
        return body;
    }

}
