<%@page import="java.util.HashMap"%>
<%@page contentType="application/json;charset=utf-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

{
    "result" : "${result}",
    "msg" : "${msg != null ? msg : ""}",
    "restime" : "${restime != null ? restime : ''}",
    "count" : "${count != null ? count : 0}",
    "resultMap" : ${resultMap != null ? resultMap : "{}"} ,
    "resultList" : ${resultList != null ? resultList : "[]"} 
 }
