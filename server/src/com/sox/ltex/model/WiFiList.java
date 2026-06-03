/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.model;

import com.ithows.util.DateTimeUtils;
import com.ithows.util.UtilFile;
import com.ithows.util.UtilString;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * WiFi 데이터 핸들 클래스
 * @author mailt
 */
public class WiFiList {
    public ArrayList<WiFiData> list = new ArrayList<WiFiData>();
    
    public void setData(JSONArray wArray){
         
        if(wArray.length() <= 0 ){
            System.out.println("No data in json");
            return;
        }
        
        if(list.size() > 0){
            list.clear();
        }
        
        try {
            for(int i=0; i<wArray.length() ; i++){
                for(int j=0; j<wArray.getJSONArray(i).length() ; j++){
                    WiFiData wObj = new WiFiData();
                    wObj.mac = UtilString.convertMacString(wArray.getJSONArray(i).getJSONObject(j).getString("mac"));
                    wObj.rssi = wArray.getJSONArray(i).getJSONObject(j).getDouble("rssi");                    
                    wObj.ssid = wArray.getJSONArray(i).getJSONObject(j).getString("ssid");
                    wObj.time = DateTimeUtils.convertTimestampToDate(wArray.getJSONArray(i).getJSONObject(j).getLong("time"));
                    
                    list.add(wObj);
                }
            }
            
        } catch (Exception e) {
        }
        
         
    }
    
    public void setDataSOXJson(JSONArray wArray){
        
        if(wArray.length() <= 0 ){
            System.out.println("No data in json");
            return;
        }
        
        if(list.size() > 0){
            list.clear();
        }
        
        try {
            for(int i=0; i<wArray.length() ; i++){
                WiFiData wObj = new WiFiData();
                wObj.mac = UtilString.convertMacString(wArray.getJSONObject(i).getString("mac"));
                wObj.rssi = wArray.getJSONObject(i).getDouble("rssi");                    
                wObj.ssid = wArray.getJSONObject(i).getString("ssid");
                wObj.time = DateTimeUtils.convertTimestampToDate(wArray.getJSONObject(i).getLong("time"));

                list.add(wObj);
            }
            
        } catch (Exception e) {
        }
    }
    
    public void setData(String macStr, String rssiStr){

        if(list.size() > 0){
            list.clear();
        }


        if(macStr == null || macStr.length() == 0 || rssiStr == null || rssiStr.length() == 0){
            return ;
        }        
        
        Map<String, String> map = UtilString.parseSortedMapCSVByKey(macStr, rssiStr, ",");
        
        if(map != null && !map.isEmpty() && map.size() > 0){
            for (Map.Entry<String, String> entry : map.entrySet()) {
                WiFiData wObj = new WiFiData();
                wObj.mac = entry.getKey();
                wObj.rssi = Double.parseDouble(entry.getValue());
                
                list.add(wObj);
            }
        }
    }
    
    
    public void setData(String macStr){

        if(list.size() > 0){
            list.clear();
        }

        
        if(macStr == null || macStr.length() == 0){
            return ;
        }        
        
        
        ArrayList<String> maclist = UtilString.parseListCSV(macStr, ",");
        
        if(maclist != null && !maclist.isEmpty() && maclist.size() > 0){
            for (String mac : maclist) {
                WiFiData wObj = new WiFiData();
                wObj.mac = mac;
                list.add(wObj);
            }
        }
    }
    
    
    
    // 리스트 소팅
    public void sortList(boolean desc){
        
        if(list.size()== 0){
            return;
        }
        
        if(desc){
            list.sort(Comparator.reverseOrder());  // 내림 차순 정렬
            
        }else{
            list.sort(Comparator.naturalOrder());  // 오름 차순 정렬
            
        }
        
    }
    
        
    public void printList(){
        if(list.size()== 0){
            return;
        }
        
        int i = 1;
        for(WiFiData obj : list){
            System.out.println("" + i + " -  " + obj.mac + " ( " + obj.rssi + " ) = " + obj.ssid + " [ " + obj.time + " ] ");
        }
        
    }
    
    
    public void printList(int elementCount){
        
        if(list.size()== 0){
            return;
        }
        
        int i = 1;
        for(WiFiData obj : list){
            System.out.print(obj.mac + ",");
            
            if(i == elementCount){
                break;
            }else{
                i++;
            }
        }
        
        System.out.println("");
        
        i = 1;
        for(WiFiData obj : list){
            System.out.print(obj.rssi + ",");
            if(i == elementCount){
                break;
            }else{
                i++;
            }

        }
        
    }
    
    // 갯수만큼 스트링으로 돌려 줌
    public String[] getListString(int elementCount){
        
        if(list.size()== 0){
            return null;
        }
        
        String[] result = new String[2];
        
        int i = 1;
        for(WiFiData obj : list){
            result[0] += obj.mac + ",";
            result[1] += "" + obj.rssi + ",";
            
            if(i == elementCount){
                break;
            }else{
                i++;
            }
        }
        
        result[0] = UtilString.trimString(result[0], 1, false);
        result[1] = UtilString.trimString(result[1], 1, false);
        
        return result;
    }
    
    
    // 갯수만큼 스트링으로 돌려 줌
    public String getMacSortRssiList(int elementCount){
        
        if(list.size()== 0){
            return "";
        }
        
        if(elementCount <= 0){
            elementCount = list.size();
        }
        
        sortList(true);
        
        String result = "";
        
        int i = 1;
        for(WiFiData obj : list){
            result += obj.mac + ",";
            
            if(i == elementCount){
                break;
            }else{
                i++;
            }
        }
        
        result = UtilString.trimString(result, 1, false);
        
        return result;
    }
    
    
    
    // 갯수만큼 FIND SQL 스트링으로 돌려 줌
    public String getFindSQLMacList(int elementCount, String macField){
        
        if(list.size()== 0){
            return "";
        }
        
        if(elementCount <= 0){
            elementCount = list.size();
        }
        
        sortList(true);
        
        String result = "( ";
        
        int i = 1;
        for(WiFiData obj : list){
            result += "FIND_IN_SET('" + obj.mac + "', "+ macField + ") > 0 or ";
            
            if(i == elementCount){
                break;
            }else{
                i++;
            }
        }
        
        result = UtilString.trimString(result, 3, false);
        result += " ) ";
        
        return result;
    }
    
    
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////
    // static 함수 
    
    public static JSONObject readJsonFile(String fileName){
        JSONObject jParam = UtilFile.readTextToJSonObject(fileName);
        return jParam;
    }
    
    public static String getSortMacListString(String macString, String rssiString, int elementCount){
        String result  = "";
        WiFiList list = new WiFiList();
        list.setData(macString, rssiString);
        
        result = list.getMacSortRssiList(elementCount);
        
        return result;
    }
    
    public static String getSortMacListString(String macString, int elementCount){
        String result  = "";
        WiFiList list = new WiFiList();
        list.setData(macString);
        
        result = list.getMacSortRssiList(elementCount);
        
        return result;
    }
    
    
    public static String getFindSQLMacListString(String macString, String macField, int elementCount){
        String result  = "";
        WiFiList list = new WiFiList();
        list.setData(macString);
        
        result = list.getFindSQLMacList(elementCount, macField);
        
        return result;
    }
    
    public static String getFindSQLMacListString(String macString, String rssiString, String macField, int elementCount){
        String result  = "";
        WiFiList list = new WiFiList();
        list.setData(macString, rssiString);
        
        result = list.getFindSQLMacList(elementCount, macField);
        
        return result;
    }
    
    
       
    public static void main(String[] args) throws Exception {
//        String fileName = "C:\\Users\\mailt\\Desktop\\request_20211119_215829.json";
        String fileName = "C:\\Users\\mailt\\Desktop\\scan_20220607_161946.json";

        JSONObject jParam = readJsonFile(fileName);
        System.out.println(jParam);
        
        WiFiList list = new WiFiList();
//        list.setData(jParam.getJSONArray("wifi_measurements"));
//        list.setDataSOXJson(jParam.getJSONArray("wifi"));

        String macList = "70:5d:cc:dc:87:82,70:5d:cc:dc:87:84,70:5d:cc:dc:87:86,90:9f:33:f7:53:30,90:9f:33:f8:53:30,92:9f:33:57:d7:b0,92:9f:33:47:d7:b0,80:ca:4b:49:c0:1a,ea:f4:08:be:49:de,88:36:6c:c7:df:ac,88:36:6c:c7:df:ae,dc:a6:32:d7:4c:e2,90:9f:33:17:d7:b0,b0:a7:b9:ff:5d:fb,80:ca:4b:49:c0:1b,bc:62:ce:3d:6d:25,0a:08:52:5b:35:40,64:e5:99:db:5a:b4,b0:a7:b9:ff:5d:fa,bc:62:ce:3d:6d:23,64:e5:99:db:5a:b0,0a:08:52:5b:35:70,fa:d0:27:3d:a2:24,b4:a9:4f:55:b2:4f,70:5d:cc:38:7a:48,50:eb:f6:66:03:74,64:e5:99:db:56:0c,0a:08:52:5b:35:80,88:36:6c:b3:39:1c,00:26:66:c7:c7:4a";
        String rssiList = "-82.0,-28.0,-37.0,-38.0,-40.0,-46.0,-46.0,-46.0,-48.0,-48.0,-50.0,-52.0,-55.0,-56.0,-56.0,-57.0,-61.0,-62.0,-65.0,-66.0,-73.0,-75.0,-77.0,-78.0,-78.0,-78.0,-79.0,-81.0,-82.0,-82.0";
//        list.setData(macList, rssiList);
//
//        list.sortList(true);
//        list.printList();
        
        
        list.setData("00:26:66:a9:e5:48,88:3c:1c:cb:e4:c7,08:5d:dd:47:f5:b5,de:03:98:3a:e9:dd,90:9f:33:2b:54:30,f4:fd:2b:20:15:97,b4:a9:4f:a6:eb:7d,00:07:89:6a:c3:16,86:25:19:61:3b:d6,88:36:6c:c7:a3:78,70:5d:cc:a8:7f:80,0c:96:cd:3a:42:5b,00:07:79:0a:2c:48,12:96:cd:3a:42:5b,88:3c:1c:cb:e4:c6,00:27:1c:e4:16:62,08:5d:dd:ef:47:68,86:25:19:a0:47:f7,00:27:1c:32:5e:ad,4c:32:75:c7:58:23,00:27:1c:32:5e:ac,58:86:94:02:2b:dc,00:07:89:3c:40:5b,00:88:ba:32:0a:7e,70:5d:cc:39:1d:18,c8:03:f5:34:06:38,92:9f:33:44:dd:48,b4:a9:4f:5f:83:03,c8:03:f5:b4:06:38,c8:03:f5:f4:06:38,70:5d:cc:0a:31:62,08:5d:dd:47:f5:b4,00:08:9f:56:2d:a0,c8:03:f5:74:06:38,70:5d:cc:06:0d:ae,88:36:6c:7d:8e:7c,88:36:6c:11:8e:32,08:5d:dd:ef:47:67,0c:96:cd:3a:42:5a,88:3c:1c:5a:a4:4a,58:86:94:11:56:ae,26:e8:53:a0:52:dd,c6:a9:4f:a6:eb:7c,b4:a9:4f:a6:eb:7c,60:29:d5:0e:3e:5c,66:29:d5:0e:3e:5c,70:5d:cc:33:78:0c,70:5d:cc:ae:1c:0e,70:5d:cc:a8:7f:82,72:5d:cc:33:78:0c,70:5d:cc:33:a2:34,70:5d:cc:a9:23:12,90:9f:33:74:dd:48,92:9f:33:14:dd:48,00:88:ba:32:0a:7f,42:23:aa:d2:9e:7b,70:5d:cc:80:65:76,b4:a9:4f:a4:52:4a,08:5d:dd:30:c7:df,12:07:89:cc:c0:02,88:36:6c:0c:3e:bc,c6:a9:4f:a4:52:4a");
        System.out.println("" + list.getFindSQLMacList(-1, "mac"));
        
        
        
        // System.out.println("" + WiFiList.getFindSQLMacListString(fileName, fileName, 0) );
        
    }
}
