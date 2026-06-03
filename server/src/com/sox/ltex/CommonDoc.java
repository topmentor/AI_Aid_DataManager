/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex;

import com.ithows.AppConfig;
import com.ithows.ResultMap;
import com.ithows.dao.ConfigDAO;
import com.ithows.util.DurationUtil;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;



/**
 *
 * @author ksyuser
 */
public class CommonDoc {

 
    ///////////////////////////////////////////////////////////////
    //
        
        // 에러시 초기화 할 좌표 
    public static double ERROR_COORD_LONGITUDE = 127.02755;
    public static double ERROR_COORD_LATITUDE = 37.49783;
    
    public static String apikey = "soxapi";
    
    
    public static int select_area_size = 200;
    public static int select_area_size2 = 100; // 인증용
    public static String map_type = "google";
    
    
    public static String devLogPath = "./";    
    
//    public static String locationServer = "http://etriloc.ithows.com:13380/LocationDBService/getPosition.do";    
    public static String locationServer = "http://setri.ithows.com:13380/LocationDBService/getPosition.do";    
    
    
    
    
    /////////////////////////////////////////////////////////////////
    // 복합측위 관련 옵션

    public static double lte_key_weight = 0.1;
    public static double wifi_key_weight = 0.5;
    public static double wifi_mac_weight = 0.2;
    public static double ble_key_weight = 1.0;
    public static double ble_mac_weight = 0.3;

    
    public static String filter_wifi_mac = "";
        
    
    
    //////////////////////////////////////////////////////
    // 측위DB 업데이트 플래그
    public static boolean collectdb_update = false;
    
    
    static{
        
        
        // @@ 대표 지역 설정 
        locationServer = AppConfig.getConf("location_server_api") ;        
        
        
        String OS = System.getProperty("os.name").toLowerCase();
        if(OS.indexOf("win") >= 0){ 
           devLogPath = AppConfig.getConf("context_win_dir") + AppConfig.getConf("config_devlog_path") ;


        }else{
           devLogPath = AppConfig.getConf("context_dir") + AppConfig.getConf("config_devlog_path") ;

        } 
        
        System.out.println("devLogPath = " + devLogPath);
        
        loadGlobelSettings();
    }
    
    public static ArrayList<ResultMap> loadGlobelSettings() {
        
         ArrayList<ResultMap> settingList = (ArrayList<ResultMap>) ConfigDAO.selectAllConfig();
         for(ResultMap map : settingList){

             switch (map.getString("id")) {
                 case "select_area_size":
                   select_area_size =  ConfigDAO.convertInt(map, 200) ;
                    break;
                 case "filter_wifi_mac":
                    filter_wifi_mac =  ConfigDAO.convertCSV(map);                    
                 case "map":
                   map_type = map.getString("value");
                   break;
             }
             
         }
         
         return settingList;
    }
    
    

    /////////////////////////////////////////////////////////////////////
    // 구간 시간 측정 관련
    
    private static DurationUtil durationObj = null;
    
    public static void startDuration(String tag){
        durationObj = new DurationUtil(tag, devLogPath);
    }

    public static void checkInterval(String tag, String subText){
        if(durationObj == null){
            durationObj = new DurationUtil(tag, devLogPath);
        }
        durationObj.check(tag, subText);
    }
    public static void stopDuration(String tag, String subText){
        if(durationObj == null){
            return;
        }
        
        durationObj.stop(tag, subText);
        durationObj.saveLogFile("");
        durationObj = null;
    }
    
    
    public static void main(String[] args) {
        try {
            JSONObject revJson = new JSONObject();
            revJson.put("Hello", "111112");
            revJson.put("Hello2", "한글");
            System.out.println(revJson.toString());
        } catch (JSONException ex) {
            Logger.getLogger(CommonDoc.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    

}
