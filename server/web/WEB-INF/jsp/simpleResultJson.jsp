<%@page contentType="text/html;charset=utf-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
{
  "result": "${result==null ? "Error" : result }",
  "msg" : "${msg}",
  "data": "${data==null ? '' : data }"
}