<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.ithows.*,com.ithows.base.*"%>
<%
    PageBean pageBean = (PageBean) request.getAttribute("pageBean");
    String pageId = pageBean.getId();
    SessionInfo sInfo = HttpUtil.getSessionInfo(session);

    // 로그인/권한 체크는 DispatcherServlet에서 어노테이션 기반으로 처리됨
    String view = PageManager.callController(pageContext, session, request, response);

    com.ithows.PageManager.moveViewPage(view, pageContext, request, response);

%>