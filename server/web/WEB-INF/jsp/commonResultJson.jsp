<%@page import="java.util.HashMap"%>
<%@page contentType="text/html;charset=utf-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

{
    "result" : "${result}",
    "msg" : "${msg != null ? msg : ''}",
    
    <c:if test="${resultMap !=null && resultMap.size() >0}">
    "resultMap" : {
        <c:set var="cnt" value="0"></c:set>
        <c:forEach var="i" items="${resultMap}"> 
            <c:if test="${cnt < resultMap.size()-1 }">
                "${i.key}" : "${i.value}",
            </c:if>
            <c:if test="${cnt == resultMap.size()-1 }">
                "${i.key}" : "${i.value}"
            </c:if>
            <c:set var="cnt" value="${cnt+1}"></c:set>
        </c:forEach>
     }, 
    </c:if>
     
    "count" : ${resultList != null ? resultList.size() : 0},
    "resultList" : [
    
    <c:if test="${resultList.size() >0}">
        <c:forEach begin="0" end="${resultList.size()-1}" step="1" var="Idx">
            {
            <c:set var="cnt" value="0"></c:set>
            <c:forEach var="i" items="${resultList[Idx]}"> 
                <c:if test="${cnt < resultList[Idx].size()-1 }">
                    "${i.key}" : "${i.value}",
                </c:if>
                <c:if test="${cnt == resultList[Idx].size()-1 }">
                    "${i.key}" : "${i.value}"
                </c:if>
                <c:set var="cnt" value="${cnt+1}"></c:set>
            </c:forEach>

            <c:if test="${Idx < resultList.size()-1 }">
                },
            </c:if>
            <c:if test="${Idx == resultList.size()-1 }">
                }
            </c:if>

        </c:forEach>
    </c:if>                
   ]  
 }