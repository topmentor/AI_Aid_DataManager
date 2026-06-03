<%@page import="java.util.HashMap"%>
<%@page contentType="application/json;charset=utf-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

{
    "result" : "${result}",
    "msg" : "${msg != null ? msg : ""}",
    "info" : "${info != null ? info : ""}",
    "resultMap" : ${resultMap != null ? resultMap : "{}"} ,
    "resultList" : ${resultList != null ? resultList : "[]"} 
 }
