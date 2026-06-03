/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.model.shape;

import com.ithows.util.UtilString;
import com.sox.ltex.model.QueryKeyObject;
import java.util.ArrayList;

/**
 *
 * @author mailt
 */
public class FindPoint  extends GPoint {
    
    public final static int FINDTYPE_NOTFOUND = 0;
    public final static int FINDTYPE_LTE = 1;
    public final static int FINDTYPE_FUSED = 2;
    public final static int FINDTYPE_WIFI = 3;
    public final static int FINDTYPE_GNSS = 4;
    
    // @@ 2023
    public final static int FINDTYPE_FUSED_LTE = 20;
    public final static int FINDTYPE_FUSED_LTE_WIFI_BLE = 21;
    public final static int FINDTYPE_FUSED_LTE_WIFI = 22;
    public final static int FINDTYPE_FUSED_WIFI_BLE = 23;
    public final static int FINDTYPE_FUSED_BLE = 24;
    public final static int FINDTYPE_FUSED_LTE_BLE = 25;
    public final static int FINDTYPE_FUSED_WIFI = 26;
    public final static int FINDTYPE_TEMP_MATCH = 30;
    public final static int FINDTYPE_INITLTE_POINT = 31;
    public final static int FINDTYPE_INITGPS_POINT = 34;


    // Step1에 대한 초기 탐색 상수  0:NotFind 1:1단계, 2:1.5단계
    public final static int FINDTYPE_PRESTEP_0 = 0;
    public final static int FINDTYPE_PRESTEP_1 = 1;
    public final static int FINDTYPE_PRESTEP_15 = 2;


    ///////////////////////////////////////////////////
    // 쿼리 신호 정보
    public QueryKeyObject queryLte ;
    
    public String queryWifiKey = ""; // Wifi 키 리스트 
    public String queryBleKey = ""; // Ble 키 리스트      
    
    
    public int xId = 0 ;  
    public int yId = 0 ;  
    public int findType = 0 ; //  // 0:못찾음, 1:LTE, 2:복합WiFi 3:WiFi단독  4:GNSS
    
//    public double lteMatchValue = 0.0; // lte 가중치 매칭 값
//    public int lteMaxMatch = 0;
//    public int lteServingMatch = 0;   //  @@ 2025
//    public int lteNeighboringMatch = 0;       //  @@ 2025
//    public int wifiMaxMatch = 0;
//    public int bleMaxMatch = 0;  // @@ BLE
//    public double matchingScore = 0;  // @@ total
    
    
    
    public int lteLength = 0;
    public int wifiLength = 0;
    public int bleLength = 0;  // @@ BLE
    
    public double coarseX = 0 ; // @@ 2023  첫번째 서빙셀 쿼리시 받은 좌표
    public double coarseY = 0 ; // @@ 2023  첫번째 서빙셀 쿼리시 받은 좌표

    // LTE 입력 데이터
    public String singleLteKey = "";  // 최초 요청 LTE 키 (1사)
    public String lteKey = "";  // 최종 LTE 키(3사)
    public String OtherLteKey1 = "";  // 타사 LTE 키1
    public String OtherLteKey2 = "";  // 타사 LTE 키2


    // LTE 신호 구분에 따른 쿼리키 
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


    

    // 3사 매칭 결과
    public String OtherLteMatchKey = "";   // 3사 매칭 LTE 키
    public String OtherWifiMatchKey = "";   // 3사 매칭 wifi 키

    // LTE 매칭결과
    public int findLTEType = 0 ;   // 0:NotFind 1:1단계, 2:1.5단계
    public String step1LteMatchKey = "";   // 1단계 매칭된 LTE 키
    public String step15LteMatchKey = "";   // 1.5단계 매칭된 LTE 키


    
    
    /////////////////////////////////////////////////////////////////////
    // 최종 매칭 결과 (2단계)
//    public String lteMatchKey = "";   // 2단계 매칭된 LTE 키
//    public String lteServingMatchKey = ""; // LTE 매칭 리스트  @@ 2025
//    public String lteNeighboringMatchKey = ""; // LTE 매칭 리스트  @@ 2025    
//    public String wifiMatchKey = "";   // 2단계 매칭된 WiFi 키
//    public String bleMatchKey = "";   // 2단계 매칭된 Ble 키
    
    
    // 신호 별 매칭 수
    public int lteMaxMatch = 0;
    public int lteServingMiddleMeasureMatch = 0;
    public int lteServingLowMeasureMatch = 0;
    public int lteNeighboringMiddleMeasureMatch = 0;
    public int lteNeighboringLowMeasureMatch = 0;
    public int lteServingMiddleEstimateMatch = 0;
    public int lteServingLowEstimateMatch = 0;
    public int lteNeighboringMiddleEstimateMatch = 0;
    public int lteNeighboringLowEstimateMatch = 0;
    
    public int wifiMaxMatch = 0;
    public int bleMaxMatch = 0;   // @@ BLE
    

    
    // 신호 별 매칭 키 
    public String lteMatchKey = ""; // LTE 매칭 리스트  @@ 2024
    public String lteServingMiddleMeasureMatchKey = "";
    public String lteServingLowMeasureMatchKey = "";
    public String lteNeighboringMiddleMeasureMatchKey = "";
    public String lteNeighboringLowMeasureMatchKey = "";
    public String lteServingMiddleEstimateMatchKey = "";
    public String lteServingLowEstimateMatchKey = "";
    public String lteNeighboringMiddleEstimateMatchKey = "";
    public String lteNeighboringLowEstimateMatchKey = "";

    public String wifiMatchKey = ""; // Wifi 매칭 리스트  @@ 2025
    public String bleMatchKey = ""; // Ble 매칭 리스트  @@ 2025

//    public String lteServingMatchKey = ""; // LTE 서빙 매칭 리스트  @@ 2025
//    public String lteNeighboringMatchKey = ""; // LTE 네이버링 매칭 리스트  @@ 2025

    
    // lte 가중치 매칭 값
    public double lteMatchValue = 0.0;  
    public double lteServingMiddleMeasureMatchValue = 0.0;  
    public double lteServingLowMeasureMatchValue = 0.0;  
    public double lteNeighboringMiddleMeasureMatchValue = 0.0;  
    public double lteNeighboringLowMeasureMatchValue = 0.0;  
    public double lteServingMiddleEstimateMatchValue = 0.0;  
    public double lteServingLowEstimateMatchValue = 0.0;  
    public double lteNeighboringMiddleEstimateMatchValue = 0.0;  
    public double lteNeighboringLowEstimateMatchValue = 0.0;  
    
    public double wifiMatchValue = 0.0;  
    public double bleMatchValue = 0.0;  
    
    
    public double matchingScore = 0;   // @@ 복합측위 점수     
    
    
    
    
    public int pointCount = 0;   // 2단계 매칭된 포인트 수
    public int pointIdx = 0;   // 매칭 인덱스
    
    
    


    public FindPoint(GPoint pt) {
        this.x = pt.x;
        this.y = pt.y;
    }

    public FindPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public FindPoint(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.findType = type;
        
        
    }

    public FindPoint() {
    }
    
    
    public void setQueryData(QueryKeyObject queryLteObject, String qWifiKey, String qBleKey) {

        queryLte = queryLteObject;
        queryWifiKey = qWifiKey;
        queryBleKey = qBleKey;
        
    }

    public void setQueryData(String queryLteStr, String qWifiKey, String qBleKey) {

        queryLte = new QueryKeyObject(queryLteStr);
        queryWifiKey = qWifiKey;
        queryBleKey = qBleKey;
        
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
            
            if(part[2].equals("0") || part[2].equals("4")){
                result += str + ",";
            }
        }
        
        if(!result.equals("")){
            result = UtilString.trimString(result, 1, false);
        }
        
        return result;
        
    }
    
    // @@ 서빙셀 키수 
    public static int countServingCellKey(String keyListStr){
        if(keyListStr.equals("")){
            return 0;
        }
        
        int count = 0;
        
        ArrayList<String> parseList = UtilString.parseListCSV(keyListStr, ",");
        for(String str : parseList){
            if(str.equals("")){
                continue;
            }
            
            String[] part = UtilString.parseCSV(str, "_");
            
            if(part.length<3){
                continue;
            }
            
            if(part[2].equals("0") || part[2].equals("4")){
                count ++;
            }
        }
        
       
        
        return count;
        
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
    
    // @@ 네이버링셀 키수 
    public static int countNeighboringCellKey(String keyListStr){
        if(keyListStr.equals("")){
            return 0;
        }
        
        int count = 0;
        
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
                count ++;
            }
        }
        
       
        
        return count;
        
    }
    
    
    
    public double getLteAccuracy(){
        double value = 0;
        
        if(lteMaxMatch > 0  && lteLength > 0){
            value =  Math.round(((double)lteMaxMatch / (double)lteLength) * 100) / 100.0 ;
        }
        
        return value;
    }
    public double getWifiAccuracy(){
        double value = 0;
        
        if(wifiMaxMatch > 0  && wifiLength > 0){
            value =  Math.round(((double)wifiMaxMatch / (double)wifiLength) * 100) / 100.0 ;
        }
        
        return value;
    }
    public double getBleAccuracy(){
        double value = 0;
        
        if(bleMaxMatch > 0  && bleLength > 0){
            value =  Math.round(((double)bleMaxMatch / (double)bleLength) * 100) / 100.0 ;
        }
        
        return value;
    }
    
    // 좌표를 중심으로 주변 extent 좌표를 알려 줌 (WGS84)
    public MBR getAroundExtent(int margin){
        MBR mbr = new MBR();
        
        final double OFFSET_1M_X = 0.0000555 / 5;
        final double OFFSET_1M_Y = 0.0000460 / 5;
        
        mbr.minX = this.x - (OFFSET_1M_X * margin);
        mbr.minY = this.y - (OFFSET_1M_Y * margin);
        mbr.maxX = this.x + (OFFSET_1M_X * margin);
        mbr.maxY = this.y + (OFFSET_1M_Y * margin);
        
        return mbr;
    }
    
    
    // 좌표를 중심으로 주변 extent 좌표를 알려 줌 (WGS84)
    // 외부 호출용 
    public static MBR getAroundExtent(GPoint pt, int margin){
        MBR mbr = new MBR();

        final double OFFSET_1M_X = 0.0000555 / 5;
        final double OFFSET_1M_Y = 0.0000460 / 5;

        mbr.minX = pt.x - (OFFSET_1M_X * margin);
        mbr.minY = pt.y - (OFFSET_1M_Y * margin);
        mbr.maxX = pt.x + (OFFSET_1M_X * margin);
        mbr.maxY = pt.y + (OFFSET_1M_Y * margin);

        return mbr;
    }
    
    // find type 결정 
    public void checkFindType(String req_posmethod){
        
        if(req_posmethod.toLowerCase().equals("agnss") ){
            findType = FINDTYPE_GNSS;
            
        }else if(this.lteMaxMatch > 0 && req_posmethod.toLowerCase().equals("cellid") ){
            findType = FINDTYPE_LTE;
            
        }else if(this.wifiMaxMatch > 0 && req_posmethod.toLowerCase().equals("wifi") ){
            findType = FINDTYPE_WIFI;
            
        }else if (this.lteMaxMatch > 0 && this.wifiMaxMatch > 0 && this.bleMaxMatch > 0){
            findType = FINDTYPE_FUSED_LTE_WIFI_BLE;

        }else if (this.lteMaxMatch > 0 && this.wifiMaxMatch > 0 && this.bleMaxMatch == 0){
            findType = FINDTYPE_FUSED_LTE_WIFI;
            
        }else if(this.lteMaxMatch > 0 && this.wifiMaxMatch == 0 && this.bleMaxMatch == 0){
            findType = FINDTYPE_FUSED_LTE;

        }else if (this.lteMaxMatch == 0 && this.wifiMaxMatch > 0 && this.bleMaxMatch > 0){
            findType = FINDTYPE_FUSED_WIFI_BLE;

        }else if (this.lteMaxMatch == 0 && this.wifiMaxMatch == 0 && this.bleMaxMatch == 0){
            findType = FINDTYPE_FUSED_BLE;

        }else if (this.lteMaxMatch > 0 && this.wifiMaxMatch == 0 && this.bleMaxMatch > 0){
            findType = FINDTYPE_FUSED_LTE_BLE;

        }else if (this.lteMaxMatch == 0 && this.wifiMaxMatch > 0 && this.bleMaxMatch == 0){
            findType = FINDTYPE_FUSED_WIFI;

        }else {
            findType = 0;

        }
    }

    public String getStringFindType(){
        return getStringFindType(this.findType);
    }

    // 찾은 형식에 대한 텍스트 변환  @@ 2023    
    public static String getStringFindType(int fType){
        String result = "";
        

        if(fType == FINDTYPE_LTE){
            result = "LTE";
    
        }else if(fType == FINDTYPE_FUSED){
            result = "Fused";
            
        }else if(fType == FINDTYPE_WIFI){
            result = "WIFI";
            
        }else if(fType == FINDTYPE_GNSS){
            result = "GNSS";
            
        }else if(fType == FINDTYPE_FUSED_LTE_WIFI_BLE){
            result = "Fused(LTE+WIFI+BLE)";
            
        }else if(fType == FINDTYPE_FUSED_LTE_WIFI){
            result = "Fused(LTE+WIFI)";
            
        }else if(fType == FINDTYPE_FUSED_WIFI_BLE){
            result = "Fused(WIFI+BLE)";
            
        }else if(fType == FINDTYPE_FUSED_LTE_BLE){
            result = "Fused(LTE+BLE)";

        }else if(fType == FINDTYPE_FUSED_LTE){
            result = "Fused(LTE)";

        }else if(fType == FINDTYPE_TEMP_MATCH){
            result = "Temp Match";

        }else if(fType == FINDTYPE_INITGPS_POINT){
            result = "Init Point Match - GPS";

        }else if(fType == FINDTYPE_INITLTE_POINT){
            result = "Init Point Match - LTE";

        }else{
            result = "Not Found";
        }
        
        
        return result; 
    }
}
