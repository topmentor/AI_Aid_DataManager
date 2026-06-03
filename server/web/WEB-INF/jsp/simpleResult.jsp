<%@page import="java.util.HashMap"%>
<%@page contentType="text/html;charset=utf-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

 
 
<br>
<br>
<div style="padding-left: 10px">
    <table class="ui very basic collapsing celled table">
        <thead>
            <tr>
                <th style="width:100px">Status</th>
                <th style="width:200px">Mac Address</th>
                <th style="width:100px">RSSI</th>
                <th style="width:100px">Fail Count</th>
                <th style="width:300px">lastScanTime</th>
            </tr></thead>
        <tbody>
            <c:forEach begin="0" end="${sigList.size()-1}" step="1" var="devIdx">
                <tr>
                    <td>
                        <h4 class="ui image header">
                            <img src="${servletPath}/images/${sigList[devIdx].RSSI>-70 && sigList[devIdx].missCount <= 10 ? (sigList[devIdx].RSSI>-50 ? 'redO.png' : 'yellowO.png' ) : 'grayO.png' }" class="ui mini rounded image" style="width:25px;height:20px" >
                        </h4>
                        <span>${sigList[devIdx].status == 'true' && sigList[devIdx].missCount <= 10 ? '접근' : '단절' }</span>
                    </td>
                    <td>
                        ${sigList[devIdx].Mac}
                    </td>
                    <td>
                        ${sigList[devIdx].RSSI}
                    </td>
                    <td>
                        ${sigList[devIdx].missCount}
                    </td>
                    <td>
                        ${sigList[devIdx].lastScanTime}
                    </td>
                </tr>                

            </c:forEach>
        </tbody>    
    </table>


</div>