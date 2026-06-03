/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sox.ltex.model;

import com.ithows.util.UtilString;
import java.util.ArrayList;
import java.util.Map;

/**
 * 5G, LTE, WIFI, BLE 모든 신호데이터에 대한 키관리
 * @author mailt
 */
public class QueryKeyObject {
    
    public final static String LTE_ALL = "lteall";
    public final static String LTE_MEASURE_ALL = "measureall";
    public final static String LTE_SERVING_MIDDLE_MEASURE = "servingmiddlemeasure";
    public final static String LTE_SERVING_LOW_MEASURE = "servinglowmeasure";
    public final static String LTE_NEIGHBORING_MIDDLE_MEASURE = "neighboringmiddlemeasure";
    public final static String LTE_NEIGHBORING_LOW_MEASURE = "neighboringlowmeasure";
    
    public final static String LTE_ESTIMATE_ALL = "estimateall";
    public final static String LTE_SERVING_MIDDLE_ESTIMATE = "servingmiddleestimate";
    public final static String LTE_SERVING_LOW_ESTIMATE = "servinglowestimate";
    public final static String LTE_NEIGHBORING_MIDDLE_ESTIMATE = "neighboringmiddleestimate";
    public final static String LTE_NEIGHBORING_LOW_ESTIMATE = "neighboringlowestimate";
    
    
    public String measureKey = "";
    public String estimateKey = "";
    
    public String servingMiddleMeasureKey = "";
    public String servingLowMeasureKey = "";
    public String neighboringMiddleMeasureKey = "";
    public String neighboringLowMeasureKey = "";
    
    public String servingMiddleEstimateKey = "";
    public String servingLowEstimateKey = "";
    public String neighboringMiddleEstimateKey = "";
    public String neighboringLowEstimateKey = "";
    
    private static String middleChannels = "275, 1350, 2850, 3200, 1550, 1694, 100, 3050";
    private static String lowChannels = "2500, 3743, 2600";
    
    
    public String wifiMac = "";
    public String wifiKey = "";
    
    public String bleMac = "";
    public String bleKey = "";
    
    
    
    public static void main(String[] args) {
        
        
        QueryKeyObject obj = new QueryKeyObject("1_275_2_0,1_275_0_2233,2_275_2_0,2_2500_2_0,2_2500_0_3344", 
                "3_1350_2_0,3_1350_0_5566,3_1350_2_0,4_3743_2_0,4_3743_0_7788");
        System.out.println("lte all = " + obj.getQueryLteKey(LTE_ALL));
        System.out.println("lte measuer = " + obj.getQueryLteKey(LTE_MEASURE_ALL));
        System.out.println("lte estimate = " + obj.getQueryLteKey(LTE_ESTIMATE_ALL));
        System.out.println("LTE_SERVING_MIDDLE_MEASURE = " + obj.getQueryLteKey(LTE_SERVING_MIDDLE_MEASURE));
        System.out.println("LTE_SERVING_LOW_MEASURE = " + obj.getQueryLteKey(LTE_SERVING_LOW_MEASURE));
        System.out.println("LTE_NEIGHBORING_MIDDLE_MEASURE = " + obj.getQueryLteKey(LTE_NEIGHBORING_MIDDLE_MEASURE));
        System.out.println("LTE_NEIGHBORING_LOW_MEASURE = " + obj.getQueryLteKey(LTE_NEIGHBORING_LOW_MEASURE));
        System.out.println("LTE_SERVING_MIDDLE_ESTIMATE = " + obj.getQueryLteKey(LTE_SERVING_MIDDLE_ESTIMATE));
        System.out.println("LTE_SERVING_LOW_ESTIMATE = " + obj.getQueryLteKey(LTE_SERVING_LOW_ESTIMATE));
        System.out.println("LTE_NEIGHBORING_MIDDLE_ESTIMATE = " + obj.getQueryLteKey(LTE_NEIGHBORING_MIDDLE_ESTIMATE));
        System.out.println("LTE_NEIGHBORING_LOW_ESTIMATE = " + obj.getQueryLteKey(LTE_NEIGHBORING_LOW_ESTIMATE));
        
        obj.setWifi("08:10:77:19:7d:5b,08:10:77:19:7d:60,0c:96:cd:92:f1:d1,14:eb:b6:38:6f:97,1c:61:b4:21:44:9c,1c:61:b4:21:44:9d,1c:ec:72:67:6b:0d,1c:ec:72:67:6b:0e,1e:39:29:91:9b:b8,1e:39:29:91:9b:bd,1e:39:29:91:c6:9f,1e:39:29:91:f5:e1,1e:39:29:91:f5:e5,1e:39:29:91:f5:e9,1e:39:29:91:f5:ef,1e:39:29:93:11:21,1e:39:29:93:12:1d,22:ec:72:67:6b:0e,38:f4:5e:28:4d:ec,38:f4:5e:28:4d:ed,38:f4:5e:56:19:d5,3a:d5:7a:63:03:ec,3a:f4:5e:48:4d:ec,4a:6c:d0:46:61:f2,50:46:ae:38:bf:aa,50:46:ae:38:bf:ab,50:46:ae:4d:0f:ad,50:46:ae:57:64:40,54:af:97:26:ac:be,54:af:97:26:ac:bf,56:46:ae:38:bf:ab,58:86:94:61:8f:18,58:86:94:61:8f:1a,58:86:94:da:fb:d8,5a:46:ae:38:bf:aa,60:29:d5:9a:9c:88,60:29:d5:9a:9c:89,66:29:d5:9a:9c:88,66:7b:ce:e7:64:f6,6a:61:a4:d7:a0:97,6a:87:ba:96:63:98,6a:87:ba:96:63:99,70:5d:cc:dc:87:82,70:5d:cc:dc:87:84,70:5d:cc:dc:87:86,70:5d:cc:eb:28:3c,7a:46:d4:57:f3:a5,86:25:19:a6:98:af,86:25:19:c6:f0:2d,86:25:19:c8:32:50,86:25:19:cc:88:8b,88:36:6c:14:84:60,88:36:6c:14:84:62,88:36:6c:6b:a6:04,88:3c:1c:91:38:cd,88:3c:1c:a2:e5:45,88:3c:1c:a3:cb:75,88:3c:1c:bf:6a:ab,88:3c:1c:bf:7a:cf,88:3c:1c:c9:63:9f,88:3c:1c:c9:cc:53,88:3c:1c:ca:3e:67,88:3c:1c:ca:6f:a3,88:3c:1c:e3:80:6f,88:3c:1c:e3:bf:d7,8a:3c:1c:91:38:cd,8a:3c:1c:a3:cb:75,8a:3c:1c:b0:4f:d6,8a:3c:1c:bf:6a:ab,8a:3c:1c:bf:7a:cf,8a:3c:1c:c9:63:9f,8a:3c:1c:c9:cc:53,8a:3c:1c:ca:3e:67,8a:3c:1c:ca:6f:a3,8a:3c:1c:e3:bf:d7,98:26:ad:4a:6e:f8,b4:a9:4f:2b:7c:dd,b4:a9:4f:3b:39:1b,b4:a9:4f:3c:a9:7f,b4:a9:4f:3d:37:73,b4:a9:4f:3d:60:13,b4:a9:4f:3d:9f:47,b4:a9:4f:3d:bb:cf,b4:a9:4f:3d:ca:ab,b4:a9:4f:3e:36:c7,b4:a9:4f:3e:be:6f,b4:a9:4f:5f:4a:e7,b4:a9:4f:74:ed:5e,b4:a9:4f:9e:41:8d,b4:a9:4f:a1:c9:3d,b4:a9:4f:be:5c:09,b4:a9:4f:c0:65:fd,b4:a9:4f:c2:c8:d5,ba:3c:1c:91:34:5d,ba:3c:1c:a3:cb:75,ba:3c:1c:b0:4f:d5,ba:3c:1c:b0:4f:d6,ba:3c:1c:b0:5a:0e,ba:3c:1c:bf:6a:ab,ba:3c:1c:bf:7a:cf,ba:3c:1c:c9:63:9f,ba:3c:1c:c9:cc:53,ba:3c:1c:ca:3e:67,ba:3c:1c:e3:80:6f,ba:3c:1c:e3:bf:d7,ba:a9:4f:3c:a9:7f,ba:a9:4f:3d:37:73,ba:a9:4f:3d:9f:47,ba:a9:4f:3d:bb:cf,ba:a9:4f:3d:e1:cb,ba:a9:4f:3d:fe:ff,ba:a9:4f:3e:36:c7,ba:a9:4f:3e:6f:17,ba:a9:4f:3e:be:6f,ba:a9:4f:58:29:18,ba:a9:4f:74:ed:5e,ba:a9:4f:75:da:6a,ba:a9:4f:a1:c9:3d,ba:a9:4f:b4:7c:01,ba:a9:4f:be:5c:09,ba:a9:4f:c0:65:fd,ba:a9:4f:c7:68:8f,c2:87:ba:96:63:94,c2:87:ba:96:63:95,c6:a9:4f:3d:37:73,c6:a9:4f:3d:60:13,c6:a9:4f:3d:9f:47,c6:a9:4f:3e:36:c7,c6:a9:4f:3e:6f:17,c6:a9:4f:3e:be:6f,c6:a9:4f:74:ed:5e,c6:a9:4f:75:da:6a,c6:a9:4f:9e:41:8d,c6:a9:4f:a1:c9:3d,c6:a9:4f:be:5c:09,c6:a9:4f:c0:65:fd,c6:a9:4f:c2:c8:d5,ca:87:ba:96:63:95,ca:87:ba:96:63:96,d0:be:2c:2a:bf:47,d0:be:2c:2c:eb:16,dc:8e:8d:97:b5:6b,dc:8e:8d:97:b5:6e",
                "-77.0,-72.0,-73.0,-81.0,-48.0,-32.0,-72.0,-80.0,-64.0,-77.0,-65.0,-37.0,-75.0,-60.0,-33.0,-23.0,-68.0,-80.0,-65.0,-73.0,-78.0,-66.0,-73.0,-73.0,-85.0,-71.0,-76.0,-75.0,-77.0,-72.0,-78.0,-78.0,-69.0,-81.0,-84.0,-70.0,-53.0,-70.0,-58.0,-47.0,-32.0,-29.0,-32.0,-32.0,-41.0,-50.0,-62.0,-80.0,-73.0,-68.0,-67.0,-83.0,-74.0,-45.0,-79.0,-81.0,-76.0,-76.0,-66.0,-77.0,-75.0,-70.0,-76.0,-76.0,-64.0,-78.0,-75.0,-77.0,-73.0,-75.0,-76.0,-74.0,-74.0,-75.0,-77.0,-76.0,-78.0,-66.0,-78.0,-72.0,-77.0,-52.0,-76.0,-81.0,-77.0,-74.0,-74.0,-72.0,-76.0,-78.0,-74.0,-76.0,-75.0,-77.0,-63.0,-89.0,-75.0,-81.0,-73.0,-74.0,-71.0,-70.0,-73.0,-75.0,-74.0,-77.0,-75.0,-75.0,-76.0,-70.0,-67.0,-75.0,-72.0,-77.0,-81.0,-77.0,-65.0,-68.0,-76.0,-73.0,-74.0,-78.0,-37.0,-41.0,-60.0,-73.0,-73.0,-74.0,-75.0,-69.0,-75.0,-75.0,-80.0,-78.0,-75.0,-73.0,-75.0,-56.0,-56.0,-75.0,-69.0,-62.0,-51.0");
        
        obj.setBle("F3:22:33,f3:22:11,4a:55:66", "-55,-33,-72");
        
        System.out.println(obj.wifiKey);
        System.out.println(obj.bleKey);
        
    }
    
    
    
    public QueryKeyObject(String mainKey, String otherKey){
        setMeasure(mainKey);
        setEstimate(otherKey);
        
    }
    
    
    public QueryKeyObject(String mainKey){
        setMeasure(mainKey);

    }
    
    // 추정키 (타사) 구분
    public void setEstimate(String otherKey){
        estimateKey = otherKey;
        
        String servingKeys =  collectServingCellKey(otherKey);
        String neightboringKeys =  collectNeighboringCellKey(otherKey);
        
        servingMiddleEstimateKey = collectMiddleKey(servingKeys, true);
        servingLowEstimateKey = collectMiddleKey(servingKeys, false);
        neighboringMiddleEstimateKey = collectMiddleKey(neightboringKeys, true);
        neighboringLowEstimateKey = collectMiddleKey(neightboringKeys, false);
        
    }
    
    // 측정키 구분
    public void setMeasure(String mainKey){
        measureKey = mainKey;
        
        String servingKeys =  collectServingCellKey(mainKey);
        String neightboringKeys =  collectNeighboringCellKey(mainKey);
        
        servingMiddleMeasureKey = collectMiddleKey(servingKeys, true);
        servingLowMeasureKey = collectMiddleKey(servingKeys, false);
        neighboringMiddleMeasureKey = collectMiddleKey(neightboringKeys, true);
        neighboringLowMeasureKey = collectMiddleKey(neightboringKeys, false);
        
    }
    
    
    public void setWifi(String wmac, String rssi){
        
        String[] result = wifiAddressArrange(wmac, rssi);
        
        if(result == null || result.length == 0){
            return;
        }
        
        this.wifiMac =  result[0];
        this.wifiKey =  result[2];
        
    }    

    public void setBle(String bmac, String rssi){
        
        String[] result = bleAddressArrange(bmac, rssi);
        
        if(result == null || result.length == 0){
            return;
        }
        
        this.bleMac =  result[0];
        this.bleKey =  result[2];
        
    }    

    public int getQueryLteLength(String type){
        int count = 0;
        
        String queryLteKey = "" ; 
 
        if(type.toLowerCase().equals(LTE_SERVING_MIDDLE_MEASURE)){
            queryLteKey = servingMiddleMeasureKey;
            
        }else if(type.toLowerCase().equals(LTE_SERVING_LOW_MEASURE)){
            queryLteKey = servingLowMeasureKey;
            
            
        }else if(type.toLowerCase().equals(LTE_NEIGHBORING_MIDDLE_MEASURE)){
            queryLteKey = neighboringMiddleMeasureKey;
            
            
        }else if(type.toLowerCase().equals(LTE_NEIGHBORING_LOW_MEASURE)){
            queryLteKey = neighboringLowMeasureKey;
            
            
        }else if(type.toLowerCase().equals(LTE_SERVING_MIDDLE_ESTIMATE)){
            queryLteKey = servingMiddleEstimateKey;
            
            
        }else if(type.toLowerCase().equals(LTE_SERVING_LOW_ESTIMATE)){
            queryLteKey = servingLowEstimateKey;
            
            
        }else if(type.toLowerCase().equals(LTE_NEIGHBORING_MIDDLE_ESTIMATE)){
            queryLteKey = neighboringMiddleEstimateKey;
            
            
            
        }else if(type.toLowerCase().equals(LTE_NEIGHBORING_LOW_ESTIMATE)){
            queryLteKey = neighboringLowEstimateKey;
            
            
        }else if(type.toLowerCase().equals(LTE_MEASURE_ALL)){
            queryLteKey = measureKey;
            
            
        }else if(type.toLowerCase().equals(LTE_ESTIMATE_ALL)){
            queryLteKey = estimateKey ;
            
            
        }else if(type.toLowerCase().equals(LTE_ALL)){
            queryLteKey = UtilString.removeNullCsv(measureKey + "," + estimateKey , ",") ;
            
        }
        
        count = UtilString.countElementCSV(queryLteKey);
        
        return count ;
        
    }
    
    
    public String getQueryLteKey(String type){

        String queryLteKey = "" ; 
 
        if(type.toLowerCase().equals(LTE_SERVING_MIDDLE_MEASURE)){
            queryLteKey = servingMiddleMeasureKey;
            
        }else if(type.toLowerCase().equals(LTE_SERVING_LOW_MEASURE)){
            queryLteKey = servingLowMeasureKey;
            
            
        }else if(type.toLowerCase().equals(LTE_NEIGHBORING_MIDDLE_MEASURE)){
            queryLteKey = neighboringMiddleMeasureKey;
            
            
        }else if(type.toLowerCase().equals(LTE_NEIGHBORING_LOW_MEASURE)){
            queryLteKey = neighboringLowMeasureKey;
            
            
        }else if(type.toLowerCase().equals(LTE_SERVING_MIDDLE_ESTIMATE)){
            queryLteKey = servingMiddleEstimateKey;
            
            
        }else if(type.toLowerCase().equals(LTE_SERVING_LOW_ESTIMATE)){
            queryLteKey = servingLowEstimateKey;
            
            
        }else if(type.toLowerCase().equals(LTE_NEIGHBORING_MIDDLE_ESTIMATE)){
            queryLteKey = neighboringMiddleEstimateKey;
            
            
            
        }else if(type.toLowerCase().equals(LTE_NEIGHBORING_LOW_ESTIMATE)){
            queryLteKey = neighboringLowEstimateKey;
           
            
        }else if(type.toLowerCase().equals(LTE_MEASURE_ALL)){
            queryLteKey = measureKey;
            
            
        }else if(type.toLowerCase().equals(LTE_ESTIMATE_ALL)){
            queryLteKey = estimateKey ;
            
            
        }else if(type.toLowerCase().equals(LTE_ALL)){
            queryLteKey = UtilString.removeNullCsv(measureKey + "," + estimateKey , ",") ;
            
        }
        
        return queryLteKey ;
        
    }
    
    
    // @@ Middle 키만 추려내는 로직
    //true : 미들키, false : 로우키
    public static String collectMiddleKey(String keyListStr, boolean isMiddle){
        if(keyListStr.equals("")){
            return "";
        }
        
        String result = "";
        String findList = "";
        

        
        ArrayList<String> parseList = UtilString.parseListCSV(keyListStr, ",");
        for(String str : parseList){
            if(str.equals("")){
                continue;
            }
            
            String[] part = UtilString.parseCSV(str, "_");
            
            if(part.length<3){
                continue;
            }
            
            
            if(isMiddle){
                if(UtilString.containCSVElementsCount(lowChannels, part[1]) <= 0){
                    result += str + ",";
                }
            }else{
                if(UtilString.containCSVElementsCount(lowChannels, part[1]) > 0){
                    result += str + ",";
                }
            }
        }
        
        if(!result.equals("")){
            result = UtilString.trimString(result, 1, false);
        }
        
        return result;
        
    }
    
    // @@ 서빙셀 키만 추려내는 로직
    public static String collectServingCellKey(String keyListStr){
        if(keyListStr.equals("")){
            return "";
        }
        
        String result = "";
        
        ArrayList<String> parseList = UtilString.parseListCSV(keyListStr, ",");
        for(String str : parseList){
            if(str.equals("")){
                continue;
            }
            
            String[] part = UtilString.parseCSV(str, "_");
            
            if(part.length<3){
                continue;
            }
            
            if(part[2].equals("0") || part[2].equals("4")){    // 서빙
                result += str + ",";
            }
        }
        
        if(!result.equals("")){
            result = UtilString.trimString(result, 1, false);
        }
        
        return result;
        
    }
    
    public static int countKey(String keyListStr){
        if(keyListStr.equals("")){
            return 0;
        }
        
        ArrayList<String> parseList = UtilString.parseListCSV(keyListStr, ",");
        return parseList.size();
    }
    
    // @@ 네이버링셀 키 
    public static String collectNeighboringCellKey(String keyListStr){
        if(keyListStr.equals("")){
            return "";
        }
        
        String result = "";
        
        ArrayList<String> parseList = UtilString.parseListCSV(keyListStr, ",");
        for(String str : parseList){
            if(str.equals("")){
                continue;
            }
            
            String[] part = UtilString.parseCSV(str, "_");
            
            if(part.length<3){
                continue;
            }
            
            if(part[2].equals("2") || part[2].equals("3")){
                result += str + ",";
            }
        }
        
        if(!result.equals("")){
            result = UtilString.trimString(result, 1, false);
        }
        
        return result;
        
    }
    
    
    public static String[] wifiAddressArrange(String srcStr1, String srcStr2){
        
        String result[] = new String[3];
        result[0] = "";
        result[1] = "";
        result[2] = "";
        String charValue = ",";

        srcStr1 = srcStr1 != null ? UtilString.removeNullCsv(srcStr1, charValue) : null ;
        srcStr2 = srcStr2 != null ? UtilString.removeNullCsv(srcStr2, charValue) : null ;
        

        if(srcStr1 == null || srcStr1.length() == 0 || srcStr2 == null || srcStr2.length() == 0){
            return result;
        }
        
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();
        StringBuffer sb3 = new StringBuffer();

        Map<String, String> map = UtilString.parseSortedMapCSVByKey(srcStr1, srcStr2, charValue);

        if(map != null && !map.isEmpty() && map.size() > 0){
            for (Map.Entry<String, String> entry : map.entrySet()) {
                sb1.append(entry.getKey()).append(",");
                sb2.append(entry.getValue()).append(",");
                int rssi = -100;
                try{
                    rssi = (int)Float.parseFloat(entry.getValue());
                }catch(Exception e){
                    
                }
                
                rssi = (int) Math.round(rssi / 10.0);                
                rssi *= -1;                
                
                sb3.append(entry.getKey()).append("_").append(rssi).append(",");
            }

            if(sb1.length() > 0){
                result[0] = sb1.toString().toLowerCase();
                result[0] = UtilString.trimString(result[0], 1, false);
            }

            if(sb2.length() > 0){
                result[1] = sb2.toString();
                result[1] = UtilString.trimString(result[1], 1, false);
            }
            
            if(sb3.length() > 0){
                result[2] = sb3.toString().toLowerCase();
                result[2] = UtilString.trimString(result[2], 1, false);
            }

        }

        return result;
    }
    
    public static String[] bleAddressArrange(String srcStr1, String srcStr2){
        
        String result[] = new String[3];
        result[0] = "";
        result[1] = "";
        result[2] = "";
        String charValue = ",";

        srcStr1 = srcStr1 != null ? UtilString.removeNullCsv(srcStr1, charValue) : null ;
        srcStr2 = srcStr2 != null ? UtilString.removeNullCsv(srcStr2, charValue) : null ;
        

        if(srcStr1 == null || srcStr1.length() == 0 || srcStr2 == null || srcStr2.length() == 0){
            return result;
        }
        
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();
        StringBuffer sb3 = new StringBuffer();

        Map<String, String> map = UtilString.parseSortedMapCSVByKey(srcStr1, srcStr2, charValue);

        if(map != null && !map.isEmpty() && map.size() > 0){
            for (Map.Entry<String, String> entry : map.entrySet()) {
                sb1.append(entry.getKey()).append(",");
                sb2.append(entry.getValue()).append(",");
                int rssi = -100;
                try{
                    rssi = (int)Float.parseFloat(entry.getValue());
                }catch(Exception e){
                    
                }
                
                rssi = (int) Math.round(rssi / 10.0);                
                rssi *= -1;                
                
                sb3.append(entry.getKey()).append("_").append(rssi).append(",");
            }

            if(sb1.length() > 0){
                result[0] = sb1.toString().toUpperCase();
                result[0] = UtilString.trimString(result[0], 1, false);
            }

            if(sb2.length() > 0){
                result[1] = sb2.toString();
                result[1] = UtilString.trimString(result[1], 1, false);
            }
            
            if(sb3.length() > 0){
                result[2] = sb3.toString().toUpperCase();
                result[2] = UtilString.trimString(result[2], 1, false);
            }

        }

        return result;
    }
    
    
}
