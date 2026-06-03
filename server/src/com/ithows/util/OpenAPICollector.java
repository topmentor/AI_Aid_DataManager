/*
 *  Copyright 2020. S.O.X. All rights reserved
 */

package com.ithows.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import com.sox.ltex.CommonUtils;
import com.sox.ltex.model.shape.GPoint;
import com.sox.ltex.model.shape.MBR;

/**
 * Class OpenAPICollector
 * 
 * @author Roi Kim <S.O.X Co. Ltd.>
 */
public class OpenAPICollector {

    public final static int DOCTYPE_JSON = 1;
    public final static int DOCTYPE_XML = 2;
    public final static int DOCTYPE_CSV = 3;

    // SGIS 용 접근 토큰 정보
    private static String accessToken = "";
    private static long accessTimeout = 0;

    private static String getAccessToken(){

        if(accessToken.equals("") || DateTimeUtils.getTimeDifferenceNow(accessTimeout) <= 0 ){
            String param1 = "consumer_key=204ed64b782e4c94967c&consumer_secret=b01fe6c9f7ce489a8350";
            String resultText = null;
            try {
                resultText = NetUtils.getURLString("https://sgisapi.kostat.go.kr/OpenAPI3/auth/authentication.json", param1, "", "");
                JSONObject access = new JSONObject(resultText);
                accessToken = ((JSONObject)access.get("result")).getString("accessToken");
                accessTimeout = Long.parseLong ( ((JSONObject)access.get("result")).getString("accessTimeout"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return accessToken;
    }

    private static JSONObject OpenAPI(String url, String headerName, String headerValue, int type){

        String  resultStr = null;
        JSONObject obj = null;
        try {
            resultStr = NetUtils.getURLString(url, "", headerName, headerValue);

            try {
                Thread.sleep((long) (2 * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.printf(resultStr);

            if (resultStr != null && !resultStr.equals("") ) {

                if(type == DOCTYPE_XML){
                    obj = UtileXmlJson.xmlStringToJson(resultStr);
                }else if (type == DOCTYPE_JSON){
                    obj = new JSONObject(resultStr);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return obj;
    }

    public static JSONObject getAirPolution(String area){

        String url = "http://openapi.airkorea.or.kr/openapi/services/rest/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty?stationName=" +
                area +
                "&dataTerm=month&pageNo=1&numOfRows=10&ServiceKey=eiBxJ55LA8WuqHH683VeICEz5t6WjDhQ3pVeem4BsoF02LKDj9ZEXYSaRw7plXc7zk%2BzRJB9qf94iIqwwL%2FjPA%3D%3D&ver=1.3";
        return OpenAPI(url, "", "", DOCTYPE_XML);
    }

    /**
     *  주소정보를 좌표로 변환해 주는 API
     * @param area
     * @return
     */
    public static JSONObject getAddressToInfo(String area){

        try {
            area = URLEncoder.encode(area, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = "https://dapi.kakao.com/v2/local/search/address.json?&query=" + area ;
        return OpenAPI(url, "Authorization","KakaoAK 202ef1e3eaa638e7f6f162926089ffef", DOCTYPE_JSON);
    }

    /**
     * IP 주소를 지역이나 좌표 정보로 받아주는 API
     * @param ipAddress
     * @return
     */
    public static JSONObject getIPToInfo(String ipAddress){

        String url = "https://api.ipdata.co/" + ipAddress + "?api-key=b969214a291832523965be59d7d87851e7369012c117184a28ba9033" ;
        return OpenAPI(url, "Postman-Token","9d13234d-786c-49aa-bcdb-c9d2f1c06940", DOCTYPE_JSON);
    }


    /**
     * 범위를 주면 범위내 건물 도면 정보를 전달해 줌
     * @param mbr
     * @return
     * @throws Exception
     */
    public static JSONObject getBuildingMap(MBR mbr)throws Exception{

        String param2 = "minx=" + (int)mbr.minX +
                "&miny=" + (int)mbr.minY +
                "&maxx=" + (int)mbr.maxX +
                "&maxy=" + (int)mbr.maxY +
                "&accessToken=" + getAccessToken();
        String resultText = NetUtils.getURLString("https://sgisapi.kostat.go.kr/OpenAPI3/figure/buildingarea.json", param2, "", "");
        JSONObject building = new JSONObject(resultText);
        return building;
    }
    

    public static JSONObject getAddressToInfo2(String area)throws Exception{

        try {
            area = URLEncoder.encode(area, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String param2 = "address=" + area +
                "&pagenum=0&resultcount=3" +
                "&accessToken=" + getAccessToken();
        String resultText = NetUtils.getURLString("https://sgisapi.kostat.go.kr/OpenAPI3/addr/geocode.json", param2, "", "");
        JSONObject building = new JSONObject(resultText);

        // 좌표만 뽑기
//        String coord =   ((JSONObject)((JSONArray)((JSONObject)building.get("result")).get("resultdata")).get(0)).getString("x")
//                + " , " +  ((JSONObject)((JSONArray)((JSONObject)building.get("result")).get("resultdata")).get(0)).getString("y");
//        System.out.println(coord);


        return building;
    }
    
    public static JSONObject getGeoPointToAddress(GPoint pt)throws Exception{

        String param = "lat=" + pt.y +
                "&lon=" + pt.x ;
        String resultText = NetUtils.getURLString("https://spectrummap.kr/address/getEmdKorNm.json", param, "", "");
        JSONObject result = new JSONObject(resultText);
        return result;
    }
    
    
    
    public static void main(String[] args) {
//        JSONObject obj = OpenAPICollector.getAddressToInfo("울산광역시 동구 방어동");
//        System.out.println(obj);
//
//        JSONObject obj2 = OpenAPICollector.getAirPolution("종로구");
//        System.out.println(obj2);

//        JSONObject obj = OpenAPICollector.getIPToInfo("");
//        System.out.println(obj);

        JSONObject obj = null;
        try {
//            obj = OpenAPICollector.getAddressToInfo2("울산광역시 동구 방어동");
//            String coord =   ((JSONObject)((JSONArray)((JSONObject)obj.get("result")).get("resultdata")).get(0)).getString("x")
//                    + " , " +  ((JSONObject)((JSONArray)((JSONObject)obj.get("result")).get("resultdata")).get(0)).getString("y");
//            System.out.println(coord);

//            Utility.sleep(1);
//            obj = OpenAPICollector.getBuildingMap(new MBR( 959032, 1945584,959556, 1946632 ));
//            System.out.println(obj);

//            obj = OpenAPICollector.getAirPolution("종로구");
//            obj = OpenAPICollector.getIPToInfo("");
//            System.out.println(obj);

            Date start_time = new Date(System.currentTimeMillis());
            System.out.println("processing time = " + DateTimeUtils.getTimeDifferenceNow(start_time));
        

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
