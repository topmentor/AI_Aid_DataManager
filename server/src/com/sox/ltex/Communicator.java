/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sox.ltex;

import com.ithows.AppConfig;
import com.ithows.util.DateTimeUtils;
import com.ithows.util.NetUtils;
import org.json.JSONObject;

/**
 * 외부 API 호출하는 클래스
 * 
 * @author mailt
 */
public class Communicator {
    
    
    private static String insertLTEMatchUrl =  "/SSF2026/api/insertLTEMatchAllSlave.do";
    private static String insertCellidindexUrl =  "/SSF2026/api/insertCellidindexAllSlave.do";
    
    
      
    // Key 요청 
    public static JSONObject getPosition(JSONObject jParam){
        JSONObject res = null;
        
        String serverUrl = CommonDoc.locationServer;
        
        try{

           String resString = NetUtils.ajaxPostJson(serverUrl, jParam);

           System.out.println("resString = " + resString);

           res = new JSONObject(resString);

        }catch(Exception ex){
            System.out.println("Server FLP Connect Error  : " + serverUrl);
            System.out.println(ex.getMessage());
        }
        
        return res;
    }
    
    
    
    
        
    // @@ 마스터로 LTE 입력을 호출 
    public static JSONObject insertLTEMatchAPI(String cellID, String macList, String keyList, String rssi, 
                String orgMacList, String orgRssi, String bleList, String blerssi, 
                int mnc, double longitude, double latitude, String sTime, String deviceName, int collectType){
            
        JSONObject res = null;

        String serverUrl = insertLTEMatchUrl;
        
        String timeNow = DateTimeUtils.getTimeDateNow();
        System.out.println("////////////////////////////////////////////////////////////////////////////////");
        System.out.println("insertLTEMatchAPI  --------------------------   " + timeNow + "    " + serverUrl);

        try{  

           JSONObject jParam = new JSONObject();

            try{
                jParam.put("mnc" , mnc);
                jParam.put("longitude" , longitude);
                jParam.put("latitude" , latitude);
                jParam.put("collectType" , collectType);
                jParam.put("sTime" , sTime);
                jParam.put("deviceName" , deviceName);
                jParam.put("cellID" , cellID);
                jParam.put("deviceName" , deviceName);
                jParam.put("macList" , macList);
                jParam.put("keyList" , keyList);
                jParam.put("rssi" , rssi);
                jParam.put("orgMacList" , orgMacList);
                jParam.put("orgRssi" , orgRssi);
                jParam.put("bleList" , bleList);
                jParam.put("blerssi" , blerssi);
                jParam.put("slave" , true);

            }catch(Exception e){

            }


           String resString = NetUtils.ajaxPostJson(serverUrl, jParam);

           System.out.println("resString = " + resString);

          try{  
              res = new JSONObject(resString);
          }catch(Exception e){
              res = new JSONObject("{'result' : '" + res + "'}");
              return res;
          }

        }catch(Exception ex){
            System.out.println("Server Connect Error  : " + serverUrl);
            System.out.println(ex.getMessage());
        }

        return res;
    }
    
        
    // @@ 마스터로 cellid index 갱신 호출 
    public static JSONObject insertCellididexAPI(String cellID, String keyList, double longitude, double latitude, int mnc, String deviceName){
            
        JSONObject res = null;

        String serverUrl = insertCellidindexUrl;
        
        String timeNow = DateTimeUtils.getTimeDateNow();
        System.out.println("////////////////////////////////////////////////////////////////////////////////");
        System.out.println("insertCellididexAPI  --------------------------   " + timeNow + "    " + serverUrl);

        try{  

           JSONObject jParam = new JSONObject();

            try{
                jParam.put("mnc" , mnc);
                jParam.put("longitude" , longitude);
                jParam.put("latitude" , latitude);
                jParam.put("deviceName" , deviceName);
                jParam.put("cellID" , cellID);
                jParam.put("keyList" , keyList);
                jParam.put("slave" , true);

            }catch(Exception e){

            }


           String resString = NetUtils.ajaxPostJson(serverUrl, jParam);

           System.out.println("resString = " + resString);

          try{  
              res = new JSONObject(resString);
          }catch(Exception e){
              res = new JSONObject("{'result' : '" + res + "'}");
              return res;
          }

        }catch(Exception ex){
            System.out.println("Server Connect Error  : " + serverUrl);
            System.out.println(ex.getMessage());
        }

        return res;
    }

      

}
