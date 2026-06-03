/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.proj4j.*;
import com.sox.ltex.model.shape.GPoint;
import com.sox.ltex.model.shape.MBR;


/**
 *
 * @author mailt
 */
public class CoordTransformUtil {
     private static String[][] projectionConst = {
            {"EPSG:5181", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +units=m +no_defs", "Daum, Korea2000 Central Belt" },     // 다음, Korea 2000 Central Belt
            {"EPSG:5179", "+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=GRS80 +units=m +no_defs", "Naver, Korea2000 Unified CS, UTM-K (GRS80), ITRF2000" },  // 네이버, Korea 2000 Unified CS , ITRF2000(도로명지도)
            {"EPSG:3857", "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_defs" , "Google"},  // 구글, OSM
            {"EPSG:5178", "+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43" ,"새주소, UTM-K (Bessel)"},  // 새 주소, KATECH
            {"EPSG:5185", "+proj=tmerc +lat_0=38 +lon_0=125 +k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 +units=m +no_defs", "Korea2000 West Belt 2010" },  // 국립지리원
            {"EPSG:5186", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 +units=m +no_defs", "Korea2000 Central Belt 2010" },  // 국립지리원
            {"EPSG:5187", "+proj=tmerc +lat_0=38 +lon_0=129 +k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 +units=m +no_defs", "Korea2000 East Belt 2010" },  // 국립지리원
            {"NAVI", "+proj=tmerc +lat_0=38 +lon_0=128 +k=0.9999 +x_0=400000 +y_0=600000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43", "Navigation" },  // 네비게이션용
            {"EPSG:2096", "+proj=tmerc +lat_0=38 +lon_0=129 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43", "Korea 1985 East Belt"},  // TM 동부원점
            {"EPSG:32652", "+proj=utm +zone=52 +ellps=WGS84 +datum=WGS84 +units=m +no_defs", "군사지도 표준, UTM52N (WGS84)" }  // UTM 군사지도 표준
    };

    private static String[][] spotConst = {
            {"대전", "{ \"LatLng\" : \"36.36216, 127.38494\", \"EPSG:5181\" : \"234548, 318300\", \"EPSG:5179\" : \"989677.23477, 1818310.04552\", \"EPSG:5186\" : \"234548, 418300\", \"EPSG:5185\" : \"414065.70704, 420873.53273\", \"EPSG:5187\" : \"55042.90461, 419442.66947\", \"EPSG:5178\" : \"989868.72843, 1818003.50987\", \"EPSG:3857\" : \"14180426.28188, 4325224.96027\" }" },
            {"전주", "{ \"LatLng\" : \"35.82013, 127.10893\",\"EPSG:5181\" : \"209844, 258092\", \"EPSG:5179\" : \"964674.05125, 1758253.84461\", \"EPSG:5186\" : \"209844, 358092\", \"EPSG:5185\" : \"390593.65568, 360139.90135\", \"EPSG:5187\" : \"29098.15515, 359737.45536\", \"EPSG:5178\" : \"964866.27038, 1757946.54413\", \"EPSG:3857\" : \"14149701.62504, 4250882.24734\" }" },
            {"전북", "{ \"LatLng\" : \"35.72657, 127.12043\", \"EPSG:5181\" : \"210896, 247712\", \"EPSG:5179\" : \"965672.69857, 1747872.6256\", \"EPSG:5186\" : \"210896, 347712\", \"EPSG:5185\" : \"391857.86834, 349778.88474\", \"EPSG:5187\" : \"29938.39107, 349334.43793\", \"EPSG:5178\" : \"965865.07863, 1747565.27484\", \"EPSG:3857\" : \"14150981.81329, 4238101.74638\"  }" },
            {"전남", "{ \"LatLng\" : \"34.94441, 126.99877\", \"EPSG:5181\" : \"199888, 160928\", \"EPSG:5179\" : \"954231.18113, 1661178.29716\", \"EPSG:5186\" : \"199888, 260928\", \"EPSG:5185\" : \"382600.56751, 262752.7616\", \"EPSG:5187\" : \"17175.38518, 262757.24301\", \"EPSG:5178\" : \"954424.7917, 1660870.26762\", \"EPSG:3857\" : \"14137438.84687, 4131846.29569\"  }" },
            {"광주", "{ \"LatLng\" : \"35.15993, 126.85134\", \"EPSG:5181\" : \"186456, 184848\", \"EPSG:5179\" : \"940924.1189, 1685156.52186\", \"EPSG:5186\" : \"186456, 284848\", \"EPSG:5185\" : \"368686.36422, 286407.59861\", \"EPSG:5187\" : \"4220.03125, 286952.44108\", \"EPSG:5178\" : \"941117.29443, 1684848.42555\", \"EPSG:3857\" : \"14121027.12096, 4161021.13195\"  }" },
            {"경남", "{ \"LatLng\" : \"35.43543, 128.22796\", \"EPSG:5181\" : \"311504, 216096\", \"EPSG:5179\" : \"1066074.35275, 1715760.37725\", \"EPSG:5186\" : \"311504, 316096\", \"EPSG:5185\" : \"493156.29761, 320194.00787\", \"EPSG:5187\" : \"129896.58543, 315677.01503\", \"EPSG:5178\" : \"1066267.80314, 1715454.28393\", \"EPSG:3857\" : \"14274271.2382, 4198429.20955\"  }" },
            {"부산", "{ \"LatLng\" : \"35.17966, 129.07511\", \"EPSG:5181\" : \"389032.00002, 189000\", \"EPSG:5179\" : \"1143422.63434, 1688288.44762\", \"EPSG:5186\" : \"389032.00002, 289000\", \"EPSG:5185\" : \"571300.35811, 294642.34513\", \"EPSG:5187\" : \"206841.62358, 287029.86362\", \"EPSG:5178\" : \"1143616.90822, 1687983.25365\", \"EPSG:3857\" : \"14368575.50636, 4163696.47348\"  }" },
            {"울산", "{ \"LatLng\" : \"35.53938, 129.31153\", \"EPSG:5181\" : \"409640.00005, 229396\", \"EPSG:5179\" : \"1164222.09826, 1728555.699\", \"EPSG:5186\" : \"409640.00005, 329396\", \"EPSG:5185\" : \"591112.13026, 335500.29808\", \"EPSG:5187\" : \"228251.28145, 326981.79575\", \"EPSG:5178\" : \"1164415.95926, 1728251.01871\", \"EPSG:3857\" : \"14394893.81096, 4212578.43709\"  }" },
            {"경북", "{ \"LatLng\" : \"36.47356, 128.6997\", \"EPSG:5181\" : \"352336.00001, 331936\", \"EPSG:5179\" : \"1107477.92954, 1831329.29022\", \"EPSG:5186\" : \"352336.00001, 431936\", \"EPSG:5185\" : \"531640.61337, 436962.69148\", \"EPSG:5187\" : \"173086.25131, 430634.50879\", \"EPSG:5178\" : \"1107670.04219, 1831024.51568\", \"EPSG:3857\" : \"14326784.62101, 4340568.07381\"  }" },
            {"대구", "{ \"LatLng\" : \"35.87137, 128.60181\", \"EPSG:5181\" : \"344665, 264957\", \"EPSG:5179\" : \"1099466.23822, 1764426.8214\", \"EPSG:5186\" : \"344665, 364957\", \"EPSG:5185\" : \"525346.47229, 369768.70989\", \"EPSG:5187\" : \"164039.18605, 363845.10749\", \"EPSG:5178\" : \"1099659.21628, 1764121.49176\", \"EPSG:3857\" : \"14315887.72053, 4257887.95598\"  }" },
            {"충남", "{ \"LatLng\" : \"36.54204, 126.81149\", \"EPSG:5181\" : \"183120, 338208\", \"EPSG:5179\" : \"938372.91059, 1838476.66535\", \"EPSG:5186\" : \"183120, 438208\", \"EPSG:5185\" : \"362213.23785, 439718.57575\", \"EPSG:5187\" : \"4020.72396, 440420.65372\", \"EPSG:5178\" : \"938563.75188, 1838169.50492\", \"EPSG:3857\" : \"14116590.11214, 4350010.80413\"  }" },
            {"천안", "{ \"LatLng\" : \"36.81514, 127.11396\", \"EPSG:5181\" : \"210168, 368504\", \"EPSG:5179\" : \"965568.35576, 1868620.05069\", \"EPSG:5186\" : \"210168, 468504\", \"EPSG:5185\" : \"388633.55702, 470583.74301\", \"EPSG:5187\" : \"31705.96652, 470158.12099\", \"EPSG:5178\" : \"965758.93759, 1868313.51294\", \"EPSG:3857\" : \"14150260.95582, 4387754.20112\"  }" },
            {"충북", "{ \"LatLng\" : \"36.87255, 127.75603\", \"EPSG:5181\" : \"267408, 375136\", \"EPSG:5179\" : \"1022818.33054, 1874949.78019\", \"EPSG:5186\" : \"267408, 475136\", \"EPSG:5185\" : \"445754.23926, 478417.29197\", \"EPSG:5187\" : \"89084.95616, 475591.68304\", \"EPSG:5178\" : \"1023009.23282, 1874644.12644\", \"EPSG:3857\" : \"14221735.9577, 4395705.85384\"  }" },
            {"세종", "{ \"LatLng\" : \"36.47998, 127.28933\", \"EPSG:5181\" : \"225928, 331344\", \"EPSG:5179\" : \"981128.42809, 1831393.18355\", \"EPSG:5186\" : \"225928, 431344\", \"EPSG:5185\" : \"405172.42301, 433742.83668\", \"EPSG:5187\" : \"46692.91738, 432666.00007\", \"EPSG:5178\" : \"981319.67085, 1831086.61213\", \"EPSG:3857\" : \"14169783.27905, 4341453.10299\"  }" },
            {"경기", "{ \"LatLng\" : \"36.47998, 127.28933\", \"EPSG:5181\" : \"225928, 331344\", \"EPSG:5179\" : \"981128.42809, 1831393.18355\", \"EPSG:5186\" : \"225928, 431344\", \"EPSG:5185\" : \"405172.42301, 433742.83668\", \"EPSG:5187\" : \"46692.91738, 432666.00007\", \"EPSG:5178\" : \"981319.67085, 1831086.61213\", \"EPSG:3857\" : \"14169783.27905, 4341453.10299\"  }" },
            {"서울", "{ \"LatLng\" : \"37.56678, 126.97872\", \"EPSG:5181\" : \"198120, 451916\", \"EPSG:5179\" : \"953964.9386, 1952062.69993\", \"EPSG:5186\" : \"198120, 551916\", \"EPSG:5185\" : \"374822.27605, 553756.69794\", \"EPSG:5187\" : \"21417.13063, 553836.76385\", \"EPSG:5178\" : \"954154.189, 1951756.62313\", \"EPSG:3857\" : \"14135206.47268, 4492341.15888\"  }" },
            {"인천", "{ \"LatLng\" : \"37.45566, 126.70515\", \"EPSG:5181\" : \"173912, 439624\", \"EPSG:5179\" : \"929700.7169, 1939903.90197\", \"EPSG:5186\" : \"173912, 539624\", \"EPSG:5185\" : \"350873.28961, 540948.71326\", \"EPSG:5187\" : \"-3057.64228, 542056.96697\", \"EPSG:5178\" : \"929889.96013, 1939597.36414\", \"EPSG:3857\" : \"14104752.37729, 4476813.29082\"  }" },
            {"춘천", "{ \"LatLng\" : \"37.88526, 127.72984\", \"EPSG:5181\" : \"264204, 487516\", \"EPSG:5179\" : \"1020210.93032, 1987294.86965\", \"EPSG:5186\" : \"264204, 587516\", \"EPSG:5185\" : \"440164.37336, 590779.72593\", \"EPSG:5187\" : \"88263.03871, 588025.50361\", \"EPSG:5178\" : \"1020400.19445, 1986990.04933\", \"EPSG:3857\" : \"14218820.97564, 4536977.19458\"  }" },
            {"강원", "{ \"LatLng\" : \"37.73929, 128.28462\", \"EPSG:5181\" : \"313232, 471840\", \"EPSG:5179\" : \"1069131.23486, 1971364.28544\", \"EPSG:5186\" : \"313232, 571840\", \"EPSG:5185\" : \"489554.70502, 576146.13906\", \"EPSG:5187\" : \"136944.19758, 571303.92592\", \"EPSG:5178\" : \"1069321.12066, 1971060.03082\", \"EPSG:3857\" : \"14280578.56067, 4516494.98293\"  }" },
            {"속초", "{ \"LatLng\" : \"38.2069, 128.59187\", \"EPSG:5181\" : \"339428, 524164\", \"EPSG:5179\" : \"1095594.3053, 2023520.07503\", \"EPSG:5186\" : \"339428, 624164\", \"EPSG:5185\" : \"514642.81763, 629070.32374\", \"EPSG:5187\" : \"164253.43853, 623044.60357\", \"EPSG:5178\" : \"1095783.68288, 2023216.62053\", \"EPSG:3857\" : \"14314781.01018, 4582253.10659\"  }" },
            {"한국", "{ \"LatLng\" : \"36.63197, 127.93184\", \"EPSG:5181\" : \"283344, 348576\", \"EPSG:5179\" : \"1038608.17665, 1848319.1353\", \"EPSG:5186\" : \"283344, 448576\", \"EPSG:5185\" : \"462253.82291, 452177.16425\", \"EPSG:5187\" : \"104463.67485, 448702.98279\", \"EPSG:5178\" : \"1038799.57322, 1848013.5166\", \"EPSG:3857\" : \"14241307.60616, 4362425.23353\" }" }
    } ;

//    {"EPSG:4326", "+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs" }, // WGS84 경위도
//    {"EPSG:4019", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +units=m +no_defs" },  // GRS80 경위도, Korean 2000


    // 25m : 0.0002739 ,  0.0002291
    // 50M : 0.0005477 ,  0.0004583
    public static double x_offset = -999;
    public static double y_offset = -999;
    public static double offset_dist = -999;
    
    

    // 좌표를 보고 투영을 찾아내기. 한국에 한정됨
    public static String findProjection(GPoint pt, String areaName){
        String findName = "Not found";
        double min = 999999999;
        double temp;
        GPoint nPt;
        String[] item1;
        String[] item2;

        String findReferenceName = "한국";
        JSONObject findRefCoordinate = null;
        int findIndex= 20;

        String proj = "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +units=m +no_defs";
        String bestProjection = "";


        if(pt.x<90 || pt.y < 180){
            return "WGS84";
        }

        // 1. 지역 레퍼런스 찾기
        for(int i=0; i<spotConst.length ; i++){
            if(areaName.equals(spotConst[i][0])){
                findReferenceName = areaName;
                findIndex = i;
                break;
            }
        }

        try {
            findRefCoordinate = new JSONObject(spotConst[findIndex][1]);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        // 2. pt 좌표와 레퍼런스 좌표 거리 계산하여 최소 값 찾기
        GPoint refPt;

        for (int j=0 ; j< projectionConst.length-3 ; j++ ){
            item2 = projectionConst[j];

            refPt = getSpotPoint(findRefCoordinate, item2[0]);
            temp = Math.sqrt(  Math.pow((pt.x - refPt.x), 2) + Math.pow((pt.y - refPt.y), 2) );


            System.out.printf("%s dist = %.4f  :  " , item2[0], temp);
            System.out.printf("coord = %.4f , %.2f \n" , refPt.x, refPt.y );

            if(min > temp){
                bestProjection = " : " + item2[2];
                min = temp;
            }
        }


        return bestProjection;
    }

    private static GPoint getSpotPoint(JSONObject refCoordinate, String name){
        GPoint pt = new GPoint();
        String[] coord;
        String coordStr;

        try {
            coordStr = refCoordinate.getString(name);
            coord = coordStr.split(", ");
            pt.x = Double.parseDouble(coord[0]);
            pt.y = Double.parseDouble(coord[1]);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return pt;
    }



    // 988128, 1820305.5  네이버
    // 232984.5, 320288  다음
    //  232971.25 , 420296.48  지도    12동 건물



    public static String findProjectionParam(String projectionName){
        String[] item;

        for (int j=0 ; j< projectionConst.length ; j++ ){
            item = projectionConst[j];
            if(projectionName.equals(item[0])){
                return item[1];
            }
        }
        return "";
    }


    public static GPoint transformCoords(GPoint pt, String csName1, String csName2){

        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CRSFactory csFactory = new CRSFactory();

        CoordinateReferenceSystem crs1 = csFactory.createFromName(csName1);
        CoordinateReferenceSystem crs2 = csFactory.createFromName(csName2);

        CoordinateTransform trans = ctFactory.createTransform(crs1, crs2);

        ProjCoordinate p1 = new ProjCoordinate(pt.x, pt.y);
        ProjCoordinate p2 = new ProjCoordinate();

        trans.transform(p1, p2);

        return new GPoint(p2.x, p2.y);
    }


    public static GPoint transformCoords2(GPoint pt, String param1, String param2){

        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CRSFactory csFactory = new CRSFactory();

        final String PARAM1 = param1;
        CoordinateReferenceSystem crs1 = csFactory.createFromParameters("Param1", PARAM1);
        final String PARAM2 = param2;
        CoordinateReferenceSystem crs2 = csFactory.createFromParameters("Param2", PARAM2);

        CoordinateTransform trans = ctFactory.createTransform(crs1, crs2);

        ProjCoordinate p1 = new ProjCoordinate(pt.x, pt.y);
        ProjCoordinate p2 = new ProjCoordinate();
        trans.transform(p1, p2);

        return new GPoint(p2.x, p2.y);
    }



    /////////////////////////////////////////////////////////////////
    // 경위도 GPS 좌표를 특정 투영법으로 변환

    public static GPoint toWGS84(GPoint pt, String csName){

        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CRSFactory csFactory = new CRSFactory();

        CoordinateReferenceSystem crs = csFactory.createFromName(csName);

        final String WGS84_PARAM = "+title=long/lat:WGS84 +proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs";
        CoordinateReferenceSystem WGS84 = csFactory.createFromParameters("WGS84", WGS84_PARAM);

        CoordinateTransform trans = ctFactory.createTransform(crs, WGS84);

        ProjCoordinate p1 = new ProjCoordinate(pt.x, pt.y);
        ProjCoordinate p2 = new ProjCoordinate();
        trans.transform(p1, p2);

        return new GPoint(p2.x, p2.y);
    }

    
        
    // EPSG:5181 = Korea 2000 Central Belt
    public static GPoint WGS84toDaum(GPoint pt){
        String sourceParam = "+title=long/lat:GRS80 +proj=longlat +ellps=GRS80 +no_defs";
        String targetParam = "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +units=m +no_defs";

        return transformCoords2(pt, sourceParam, targetParam);
    }
    
    // EPSG:3857 
    public static GPoint WGS84toGoogle(GPoint pt){
        String sourceParam = "+title=long/lat:GRS80 +proj=longlat +ellps=GRS80 +no_defs";
        String targetParam =  "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_defs";

        return transformCoords2(pt, sourceParam, targetParam);
    }

    // EPSG:5179 
    public static GPoint WGS84toNaver(GPoint pt){
        String sourceParam = "+title=long/lat:GRS80 +proj=longlat +ellps=GRS80 +no_defs";
        String targetParam = "+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=GRS80 +units=m +no_defs";

        return transformCoords2(pt, sourceParam, targetParam);
    }

    // EPSG:32652 
    public static GPoint WGS84toUTM(GPoint pt){
        String sourceParam = "+title=long/lat:GRS80 +proj=longlat +ellps=GRS80 +no_defs";
        String targetParam = "+proj=utm +zone=52 +ellps=WGS84 +datum=WGS84 +units=m +no_defs";

        return transformCoords2(pt, sourceParam, targetParam);
    }

    
    //////////////////////////////////////////////////////////////////
    // 특정 투영을 경위도로 변환 
    public static GPoint toDegree(GPoint pt, String csName){

        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CRSFactory csFactory = new CRSFactory();

        CoordinateReferenceSystem crs = csFactory.createFromName(csName);

        final String GRS80_PARAM = "+title=long/lat:GRS80 +proj=longlat +ellps=GRS80 +no_defs";
        CoordinateReferenceSystem WGS84 = csFactory.createFromParameters("GRS80", GRS80_PARAM);

        CoordinateTransform trans = ctFactory.createTransform(crs, WGS84);

        ProjCoordinate p1 = new ProjCoordinate(pt.x, pt.y);
        ProjCoordinate p2 = new ProjCoordinate();
        trans.transform(p1, p2);

        return new GPoint(p2.x, p2.y);
    }
    
        // EPSG:32652 
    public static GPoint UTMtoWGS84(GPoint pt){
        String sourceParam = "+proj=utm +zone=52 +ellps=WGS84 +datum=WGS84 +units=m +no_defs";
        String targetParam = "+title=long/lat:GRS80 +proj=longlat +ellps=GRS80 +no_defs";

        return transformCoords2(pt, sourceParam, targetParam);
    }

    /////////////////////////////////////////////////////////////////
    // 특정 투영법을 다른 투영법으로 변환
    public static GPoint toKorEastBessel(GPoint pt, String param){
        String sourceParam = param;
        String targetParam = "+proj=tmerc +lat_0=38 +lon_0=129 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43";

        return transformCoords2(pt, sourceParam, targetParam);
    }

    public static GPoint toKorCenterBessel(GPoint pt, String param){
        String sourceParam = param;
        String targetParam = "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43";

        return transformCoords2(pt, sourceParam, targetParam);
    }

    public static GPoint toKorWestBessel(GPoint pt, String param){
        String sourceParam = param;
        String targetParam = "+proj=tmerc +lat_0=38 +lon_0=125 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43";

        return transformCoords2(pt, sourceParam, targetParam);
    }

    // EPSG:5179 = Korea 2000 Unified CS, UTM-K(GRS80),
    public static GPoint toNaver(GPoint pt, String param){
        String sourceParam = param;
        String targetParam = "+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=GRS80 +units=m +no_defs";

        return transformCoords2(pt, sourceParam, targetParam);
    }

    // EPSG:5181 = Korea 2000 Central Belt
    public static GPoint toDaum(GPoint pt, String param){
        String sourceParam = param;
        String targetParam = "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +units=m +no_defs";

        return transformCoords2(pt, sourceParam, targetParam);
    }
    
    public static GPoint toGoogle(GPoint pt, String param){
        String sourceParam = param;
        String targetParam = "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +no_defs";

        return transformCoords2(pt, sourceParam, targetParam);
    }
    
    

    ///////////////////////////////////////////////////////////////
    // 거리 구하기 
    
    /**
     * 거리 구하기
     * @param sPt
     * @param ePt
     * @param isWGS84  : 경위도 일때와 아닐때 구분
     * @return 
     */
    public static double getDistanceBetween(GPoint sPt, GPoint ePt, boolean isWGS84){
        
        GPoint pt;
        GPoint refPt;
        
        if(sPt == null || ePt == null){
            return -1;
        }
        
        if(isWGS84){
            pt = WGS84toUTM(sPt);
            refPt = WGS84toUTM(ePt);
        }else{
            pt = sPt;
            refPt = ePt;
        }
        
        double dist = Math.sqrt(  Math.pow((pt.x - refPt.x), 2) + Math.pow((pt.y - refPt.y), 2) );
        
        return dist;
        
    }
    
    
    
    
    ///////////////////////////////////////////////////////////////
    // 특정 거리에 있는 점을 알려 줌
    
    public final static int DIST_DIRECTION_ONLY_X = 1 ;
    public final static int DIST_DIRECTION_ONLY_Y = 2 ;
    public final static int DIST_DIRECTION_XY = 3 ;
    /**
     * 거리 좌표를 계산해 줌 (한국 중심)
     * @param pt   : 경위도 좌표
     * @param dist : 거리 미터단위
     * @param opt : 방향
     * @return 
     */
    public static GPoint getDistanceCoord(GPoint pt, double dist, int opt){
        GPoint projPt = WGS84toDaum(pt);
        
        if(opt == DIST_DIRECTION_ONLY_X){
            projPt.x += dist;
        }else if (opt == DIST_DIRECTION_ONLY_Y){
            projPt.y += dist;
        }else{
            projPt.x += dist;
            projPt.y += dist;
            
        }
        
        return toWGS84(projPt, "EPSG:5181");
        
    }
    
    /**
     * 거리 좌표를 계산해 줌 (UTM)
     * @param pt   : 경위도 좌표
     * @param dist : 거리 미터단위
     * @param opt : 방향
     * @return 
     */
    public static GPoint getDistanceCoordUTM(GPoint pt, double dist, int opt){
        GPoint projPt = WGS84toUTM(pt);
        
        if(opt == DIST_DIRECTION_ONLY_X){
            projPt.x += dist ;
        }else if (opt == DIST_DIRECTION_ONLY_Y){
            projPt.y += dist;
        }else{
            projPt.x += dist;
            projPt.y += dist;
            
        }
        
        return toWGS84(projPt, "EPSG:32652");
    }
    

    // 특정 거리에 대한 좌표 옵셋을 구함
    public static GPoint setBoxOffset(double dist){
        GPoint ptOffset = new GPoint();
        GPoint orgPt = new GPoint(127.07719311406767, 37.6192894421894);  // @@ 국가에 따라 이 좌표를 달리해야 할 수 있음
        
        offset_dist = dist;
        GPoint pt = getDistanceCoordUTM(orgPt, offset_dist, DIST_DIRECTION_XY);
        ptOffset.x = x_offset = pt.x - orgPt.x;
        ptOffset.y = y_offset = pt.y - orgPt.y;
        return ptOffset;
    }
    
    // 특정 좌표를 중심으로 옵셋을 적용한 박스리턴
    public static MBR getBoxFromCoord(GPoint orgPt, double dist){
        MBR mbr = new MBR();
        
        if(offset_dist !=  dist){
            setBoxOffset(dist);
        }

        mbr.minX = orgPt.x - x_offset;
        mbr.minY = orgPt.y - y_offset;
        mbr.maxX = orgPt.x + x_offset;
        mbr.maxY = orgPt.y + y_offset;
        
        return mbr;
    }
    
    // 특정 좌표를 베이스로 옵셋을 적용한 박스리턴
    public static MBR getBoxFromCoord2(GPoint orgPt, double dist){
        MBR mbr = new MBR();
        
        if(offset_dist !=  dist){
            setBoxOffset(dist);
        }

        mbr.minX = orgPt.x ;
        mbr.minY = orgPt.y ;
        mbr.maxX = orgPt.x + x_offset;
        mbr.maxY = orgPt.y + y_offset;
        
        return mbr;
    }
    


    private static void sample1(){
        String csName1 = "EPSG:5181";  // 다음
        String csName2 = "EPSG:5179";  // 네이버
        String csName3 = "EPSG:3857";  // OSM
        
        // 투영 --> 경위도
        System.out.println("231866.0, 320188.0 >> " + transformCoords(new GPoint(231866.0, 320188.0), csName1, csName2));
        System.out.println("231866.0, 320188.0 >> " + transformCoords(new GPoint(231866.0, 320188.0), csName1, csName3));
        System.out.println("231866.0, 320188.0 >> " + toWGS84(new GPoint(231866.0, 320188.0), csName1));
        System.out.println("231866.0, 320188.0 >> " + toDegree(new GPoint(231866.0, 320188.0), csName1));

        // 투영 변환 : 경위도 --> 투영
        String param1 = "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +units=m +no_defs";
        String param2 = "+proj=longlat +ellps=GRS80 +no_defs";
        System.out.println("231866.0, 320188.0 >> " + transformCoords2(new GPoint(231866.0, 320188.0), param1, param2));
        System.out.println("231866.0, 320188.0 KorEastBessel>> " + toKorEastBessel(new GPoint(231866.0, 320188.0), param1));
        System.out.println("231866.0, 320188.0 Google>> " + toGoogle(new GPoint(231866.0, 320188.0), param1));
        System.out.println("231866.0, 320188.0 Daum>> " + toDaum(new GPoint(231866.0, 320188.0), param1));
        System.out.println("231866.0, 320188.0 Naver>> " + toNaver(new GPoint(231866.0, 320188.0), param1));
        System.out.println("231866.0, 320188.0 KCB>> " + toKorCenterBessel(new GPoint(231866.0, 320188.0), param1));

                // 투영 찾기
        System.out.println("962211, 1957001 projection>> " + findProjection(new GPoint(962211, 1957001), "서울"));
        System.out.println("14186012.15642, 4451213.42557 projection>> " + findProjection(new GPoint(14186012.15642, 4451213.42557), "한양"));

    }


    public static void main(String[] args){
        String csName1 = "EPSG:5181";  // 다음
        String csName2 = "EPSG:5179";  // 네이버
        String csName3 = "EPSG:3857";  // OSM
        String csName4 = "EPSG:32652";  // UTM

        System.out.println("-30000, -60000 >> " + toWGS84(new GPoint( -30000, -60000), csName1));
        System.out.println("494288, 988576 >> " + toWGS84(new GPoint( 494288, 988576), csName1));

        System.out.println("90112, 1192896 >> " + toWGS84(new GPoint( 90112, 1192896), csName2));
        System.out.println("1990673, 2761664 >> " + toWGS84(new GPoint( 1990673, 2761664), csName2));

        
//        System.out.println("231866.0, 320188.0 >> " + transformCoords(new GPoint(231866.0, 320188.0), csName1, csName2));
//        System.out.println("231866.0, 320188.0 >> " + transformCoords(new GPoint(231866.0, 320188.0), csName1, csName3));
//        System.out.println("231866.0, 320188.0 >> " + toWGS84(new GPoint(231866.0, 320188.0), csName1));

        GPoint wgsPt = toWGS84(new GPoint(231866.0, 320188.0), csName1);
        
        String param1 = "+proj=utm +zone=52 +ellps=WGS84 +datum=WGS84 +units=m +no_defs";
        String param2 = "+proj=longlat +ellps=GRS80 +no_defs";
        String param3 = "+title=long/lat:GRS80 +proj=longlat +ellps=GRS80 +no_defs";
//        System.out.println("127.35513144016795,36.37927016152328 >> " + toDaum(wgsPt, param3));
        
        GPoint res = getDistanceCoord(wgsPt, 5, DIST_DIRECTION_XY );
//        System.out.println(wgsPt + " >> " + res );
//        System.out.println(String.format("%.7f ,  %.7f", (res.x - wgsPt.x), (res.y - wgsPt.y)));
        
        
//         System.out.println("231866.0, 320188.0 >> " + transformCoords2(new GPoint(231866.0, 320188.0), "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +units=m +no_defs", param1));

        // UTM으로 5미터씩 
//         GPoint pt = wgsPt;
//         System.out.println(pt + " >> " + WGS84toUTM(pt) );
//         res = getDistanceCoordUTM(pt, 25, DIST_DIRECTION_XY );
//         System.out.println(pt + " >> " + res );
//         System.out.println(String.format("25m : %.7f ,  %.7f", (res.x - pt.x), (res.y - pt.y)));
//         res = getDistanceCoordUTM(pt, 50, DIST_DIRECTION_XY );
//         System.out.println(String.format("50M : %.7f ,  %.7f", (res.x - pt.x), (res.y - pt.y)));

        GPoint offset = setBoxOffset(5);
        System.out.println(String.format("5M : %.7f ,  %.7f", offset.x, offset.y));
        System.out.println("5M : " +  getBoxFromCoord(wgsPt, 5).toString(true));

        GPoint offset2 = setBoxOffset(25);
        System.out.println(String.format("25M : %.7f ,  %.7f", offset2.x, offset2.y));
        System.out.println("25M : " +  getBoxFromCoord(wgsPt, 25).toString(true));

        GPoint offset3 = setBoxOffset(50);
        System.out.println(String.format("50M : %.7f ,  %.7f", offset3.x, offset3.y));
        System.out.println("50M : " +  getBoxFromCoord(wgsPt, 50).toString(true));

        GPoint offset4 = setBoxOffset(1000);
        System.out.println(String.format("1000M : %.7f ,  %.7f", offset4.x, offset4.y));
        System.out.println("1000M : " +  getBoxFromCoord(wgsPt, 1000).toString(true));


//          GPoint pt = new GPoint(127.35513144016795,36.37927016152328);
//          GPoint pt2 = WGS84toUTM(pt);
//          System.out.println(pt + " >> " + pt2 );
//          System.out.println(pt2 + " >> " + UTMtoWGS84(pt2));
    }
}
