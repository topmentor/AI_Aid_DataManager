/*
 *  Copyright 2020. S.O.X. All rights reserved
 */

package com.ithows.dao;

import com.ithows.CommonUtils;
import com.ithows.JdbcDao;
import com.ithows.ResultMap;
import com.ithows.util.DateTimeUtils;
import com.ithows.util.UtilFile;
import com.ithows.util.UtilString;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import com.sox.ltex.model.shape.GPoint;
import com.sox.ltex.model.shape.MBR;
import org.json.JSONObject;

/**
 * Class PoiDAO
 * 
 * @author Roi Kim <S.O.X Co. Ltd.>
 */
public class PoiDAO {

    public static void main(String[] args) throws Exception{
       
//        Date start_time = new Date(System.currentTimeMillis());
//
//        String buildingPath = "C:\\Users\\mailt\\Desktop\\내비게이션용DB\\건물\\";
//        insertBuilding(buildingPath);    
//        
//        String addressPath = "C:\\Users\\mailt\\Desktop\\내비게이션용DB\\지번\\";
//        insertAddress(addressPath);
//       
//        System.out.println("total time = " + DateTimeUtils.getTimeDifferenceNow(start_time));    
        
        
//        path = "C:\\Users\\mailt\\Desktop\\내비게이션용DB\\지번\\";
//        String fileName = path + "utf_match_jibun_jeju.csv";
//        insertAddressPoi(fileName);
        
          JSONObject obj = new JSONObject("{\"minY\":36.3490753,\"minX\":127.386795,\"maxY\":36.3512497,\"maxX\":127.3901672}");
          String result = "";
          MBR extent = new MBR( obj.getDouble("minX") , obj.getDouble("minY") , obj.getDouble("maxX") , obj.getDouble("maxY"));
          result = getAddressFromExtent(extent, 13);
          System.out.println("dong = " + result);    
    }
    
    
    // point에 대한 지역명 주기   @@ 2023
    public static String getAddressFromPoint(double cx, double cy) {
        String result = "대한민국";
        
        String query = "select *,\n" +
                        "SQRT(POW((centerX - " + cx + "), 2) + POW((centerY - " + cy + "), 2)) as dist \n" +
                        "from regcode\n" +
                        "where maxX >= " + cx + "  and maxY >= " + cy + " and minX <= " + cx + " and minY <= " + cy + " \n" +
                        "      and dor <> \"\" \n" +
                        "order by dist asc  \n" +
                        "limit 1;" ;
        
//        System.out.println("query = " + query);
        
        try {
               ArrayList<ResultMap> List = (ArrayList<ResultMap>) JdbcDao.queryForMapList(query, new Object[]{}); 

                if(List != null && List.size() > 0){
                    result = List.get(0).getString("sido") + " " + List.get(0).getString("gun") + " " + List.get(0).getString("dong") ;
                    return result;
                }

        } catch (SQLException e) {
            e.printStackTrace();
        }
      
        return result;
    }
    
    
    
    // extent에 대한 지역명 주기
    public static String getAddressFromExtent(MBR extent, int zoomLevel) {
        double cx = extent.getCenterX();
        double cy = extent.getCenterY();
        String result = "";
        
        
        String query = "select *,\n" +
                        "SQRT(POW((centerX - " + cx + "), 2) + POW((centerY - " + cy + "), 2)) as dist \n" +
                        "from regcode\n" +
                        "where maxX >= " + cx + "  and maxY >= " + cy + " and minX <= " + cx + " and minY <= " + cy + " \n" +
                        "      and dor <> \"\" \n" +
                        "order by dist asc  \n" +
                        "limit 2;" ;
        
//        System.out.println("" + extent + "   " + zoomLevel);
//        System.out.println("query = " + query);
        
        
        try {
               ArrayList<ResultMap> List = null;
               
               if(zoomLevel > 9) {
                    List = (ArrayList<ResultMap>) JdbcDao.queryForMapList(query, new Object[]{}); 
                    
                    if(List != null && List.size() > 0){
                        result = List.get(0).getString("sido") + " " + List.get(0).getString("gun") + " " ;
                        for(ResultMap map : List ){
                            result += map.getString("dong") + ",";
                        }

                        if(!result.equals("")){
                            result = UtilString.trimTail(result, 1);
                        }
                        return result;
                    }
               }
               
               query = "select *,\n" +
                        "SQRT(POW((centerX - " + cx + "), 2) + POW((centerY - " + cy + "), 2)) as dist \n" +
                        "from regcode\n" +
                        "where maxX >= " + cx + "  and maxY >= " + cy + " and minX <= " + cx + " and minY <= " + cy + " \n" +
                        "      and dor = \"\" and gun <> \"\" \n" +
                        "order by dist asc  \n" +
                        "limit 1;" ;
        
//                       System.out.println("query = " + query);
               if(zoomLevel > 6) {
                    List = (ArrayList<ResultMap>) JdbcDao.queryForMapList(query, new Object[]{}); 
                    
                    if(List != null && List.size() > 0 ){
                        result = List.get(0).getString("sido") + " " ;

                        for(ResultMap map : List){
                            result += map.getString("gun") + ",";
                        }

                        if(!result.equals("")){
                            result = UtilString.trimTail(result, 1);
                        }
                        return result;
                    }
               }
               
               query = "select *,\n" +
                        "SQRT(POW((centerX - " + cx + "), 2) + POW((centerY - " + cy + "), 2)) as dist \n" +
                        "from regcode\n" +
                        "where maxX >= " + cx + "  and maxY >= " + cy + " and minX <= " + cx + " and minY <= " + cy + " \n" +
                        "      and dor = \"\" and gun = \"\" \n" +
                        "order by dist asc  \n" +
                        "limit 1;" ;
               
               ResultMap map = JdbcDao.queryForMapObject(query, new Object[]{}); 
               if(map != null){
                   result = map.getString("sido") ;
               }else{
                   result = "대한민국";
               }
               

        } catch (SQLException e) {
            e.printStackTrace();
            return "대한민국";
        }
      
        return result;
    }
        
    
    
    public static String getAddressFromExtent_old(MBR extent, int zoomLevel) {
        double cx = extent.getCenterX();
        double cy = extent.getCenterY();
        String result = "";
        

        
        String query = "" +
                "SELECT \n" +
                "sido,\n" +
                "gun,\n" +
                "GROUP_CONCAT(if(result.dong<>\"\", result.dong, null) SEPARATOR ',') as 'dong',\n" +
                "score \n" +
                "from(\n" +
                "	select *,\n" +
                "       (if(centerX >= " + extent.minX + " and centerY >= " + extent.minY + "  and centerX <= " + extent.maxX + " and centerY <= " + extent.maxY + ", 2,0) + \n" +
                "	if(maxX >= " + cx + "  and maxY >= " + cy + " and minX <= " + cx + " and minY <= " + cy + ", 2, 0)) as score \n" +
                "	from regcode\n" +
                "	where dor <> \"\" and \n" +
                "	(if(centerX >= " + extent.minX + " and centerY >= " + extent.minY + "  and centerX <= " + extent.maxX + " and centerY <= " + extent.maxY + ", 2,0) + \n" +
                "	if(maxX >= " + cx + "  and maxY >= " + cy + " and minX <= " + cx + " and minY <= " + cy + ", 2, 0)) > 1 \n" +
                ") as result\n" +
                "group by score \n" +
                "order by score desc \n" +
                "limit 1;";
        
//        System.out.println("" + extent + "   " + zoomLevel);
//        System.out.println("query = " + query);
        
        
        try {
               ResultMap map = null;
               
               if(zoomLevel > 10) {
                    map = JdbcDao.queryForMapObject(query, new Object[]{}); 
                    if(map != null){
                        result = map.getString("sido") + " " + map.getString("gun") + " " + map.getString("dong");
                        return result;
                    }
               }
               
               query = "" +
                "SELECT \n" +
                "sido,\n" +
                "gun,\n" +
                "score \n" +
                "from(\n" +
                "	select *,\n" +
                "    (if(not(maxX < " + extent.minX + " or maxY < " + extent.minY + "  or minX > " + extent.maxX + " or minY > " + extent.maxY + "), 1,0) + \n" +
                "	if(maxX >= " + cx + "  and maxY >= " + cy + " and minX <= " + cx + " and minY <= " + cy + ", 2, 0)) as score \n" +
                "	from regcode\n" +
                "	where gun <> \"\" and dong = \"\" and \n" +
                "	(if(not(maxX < " + extent.minX + " or maxY < " + extent.minY + "  or minX > " + extent.maxX + " or minY > " + extent.maxY + "), 1,0) + \n" +
                "	if(maxX >= " + cx + "  and maxY >= " + cy + " and minX <= " + cx + " and minY <= " + cy + ", 2, 0)) > 0\n" +
                ") as result\n" +
                "group by score \n" +
                "order by score desc \n" +
                "limit 1;";
               
               if(zoomLevel > 7) {
                    map = JdbcDao.queryForMapObject(query, new Object[]{}); 
                    if(map != null){
                        result = map.getString("sido") + " " + map.getString("gun");
                        return result;
                    }
               }
               
               query = "" +
                "SELECT \n" +
                "sido,\n" +
                "score \n" +
                "from(\n" +
                "	select *,\n" +
                "    (if(not(maxX < " + extent.minX + " or maxY < " + extent.minY + "  or minX > " + extent.maxX + " or minY > " + extent.maxY + "), 1,0) + \n" +
                "	if(maxX >= " + cx + "  and maxY >= " + cy + " and minX <= " + cx + " and minY <= " + cy + ", 2, 0)) as score \n" +
                "	from regcode\n" +
                "	where sido <> \"\" and  gun = \"\" and dong = \"\" and  \n" +
                "	(if(not(maxX < " + extent.minX + " or maxY < " + extent.minY + "  or minX > " + extent.maxX + " or minY > " + extent.maxY + "), 1,0) + \n" +
                "	if(maxX >= " + cx + "  and maxY >= " + cy + " and minX <= " + cx + " and minY <= " + cy + ", 2, 0)) > 0\n" +
                ") as result\n" +
                "group by score \n" +
                "order by score desc \n" +
                "limit 1;";
               
               map = JdbcDao.queryForMapObject(query, new Object[]{}); 
               if(map != null){
                   result = map.getString("sido") ;
                   return result;
               }
               

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
      
        return result;
    } 
    
    public static void insertBuilding(String csvDir){

        String[] filenames = UtilFile.getFileList(csvDir, "") ;
        for (int i = 0; i < filenames.length; i++) {
            System.out.println("file : " + filenames[i] + "=====================================");
            
            String transFile = csvDir + "utf_" + FilenameUtils.getBaseName(filenames[i]) + ".csv" ;
            
            try {
                // 1. UTF-8 변환
                UtilFile.transformEncoding(csvDir+ filenames[i], "MS949", transFile, "UTF-8");
                
                // 2. DB 입력
                insertBuildingPoi(transFile);
                
            } catch (IOException ex) {
                System.out.println("" + filenames[i] + " trans encoding error");
            }
            
        }
    }
    
    public static void insertAddress(String csvDir){

        String[] filenames = UtilFile.getFileList(csvDir, "") ;
        for (int i = 0; i < filenames.length; i++) {
            System.out.println("file : " + filenames[i] + "=====================================");

            String transFile = csvDir + "utf_" + FilenameUtils.getBaseName(filenames[i]) + ".csv" ;
            
            try {
                // 1. UTF-8 변환
                UtilFile.transformEncoding(csvDir+ filenames[i], "MS949", transFile, "UTF-8");
                
                // 2. DB 입력
                insertAddressPoi(transFile);
                
            } catch (IOException ex) {
                System.out.println("" + filenames[i] + " trans encoding error");
            }
                        
        }
    }
    
    
    // 건물 텍스트 데이터를 읽어서 DB에 저장
    private static boolean insertBuildingPoi(String fileName){

        
        Date start_time = new Date(System.currentTimeMillis());
        
        String InsertSQL = "insert into building (jusoemd_code,sid,sig,emd,road_code,road_name,basement,building_no1,building_no2,post_code,id,"
                + "building_name,building_use,district_code,district_name,level_high,level_low,building_type,building_count,building_name2,building_history,building_history2,"
                + "building_live,centerX,centetY,enterX,entetY,sid_eng,sig_eng,emd_eng,road_name_eng,emd_type ) values ";

        int i=1;
        int count = 0;
        int bundle = 5000; // 쿼리가 많은 경우 5000개씩 쪼개서 인서트 함
        
        GPoint centerPointWGS84 = null;
        GPoint enterPointWGS84 = null;
                
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {

            String line = "";
            
            while ((line = reader.readLine()) != null) {
                
                line = line.replace("\'", " ");
                String[] lineData = line.split("\\|") ;


                
                if (i%bundle == 0) {
                    
                    InsertSQL = InsertSQL + "('" + lineData[0] +  "', '" + lineData[1] + "', '" + lineData[2] + "', '" + lineData[3] + "', '" + lineData[4]
                            + "', '" + lineData[5] + "', '" + lineData[6] + "', " + (lineData[7].equals("") ? "0" : lineData[7]) + ", " + (lineData[8].equals("") ? "0" : lineData[8]) + ", '" + lineData[9] + "', '" + lineData[10]  
                            + "', '" + lineData[11] + "', '" + lineData[12] + "', '" + lineData[13] + "', '" + lineData[14] + "', " + (lineData[15].equals("") ? "0" : lineData[15]) + ", " + (lineData[16].equals("") ? "0" : lineData[16]) 
                            + ", '" + lineData[17] + "', " + (lineData[18].equals("") ? "1" : lineData[18]) + ", '" + lineData[19] + "', '" + lineData[20] + "', '" + lineData[21] + "', '" + lineData[22]  
                            + "', " + (lineData[23].equals("") ? "0" : lineData[23])  + ", " + (lineData[24].equals("") ? "0" : lineData[24]) + ", " + (lineData[25].equals("") ? "0" : lineData[25]) 
                            + ", " + (lineData[26].equals("") ? "0" : lineData[26]) + ", '" + lineData[27] + "', '" + lineData[28]  
                            + "', '" + lineData[29] + "', '" + lineData[30] + "', '" + lineData[31] + "' ); ";

                } else {
                    InsertSQL = InsertSQL + "('" + lineData[0] +  "', '" + lineData[1] + "', '" + lineData[2] + "', '" + lineData[3] + "', '" + lineData[4]
                            + "', '" + lineData[5] + "', '" + lineData[6] + "', " + (lineData[7].equals("") ? "0" : lineData[7]) + ", " + (lineData[8].equals("") ? "0" : lineData[8]) + ", '" + lineData[9] + "', '" + lineData[10]  
                            + "', '" + lineData[11] + "', '" + lineData[12] + "', '" + lineData[13] + "', '" + lineData[14] + "', " + (lineData[15].equals("") ? "0" : lineData[15]) + ", " + (lineData[16].equals("") ? "0" : lineData[16]) 
                            + ", '" + lineData[17] + "', " + (lineData[18].equals("") ? "1" : lineData[18]) + ", '" + lineData[19] + "', '" + lineData[20] + "', '" + lineData[21] + "', '" + lineData[22]  
                            + "', " + (lineData[23].equals("") ? "0" : lineData[23])  + ", " + (lineData[24].equals("") ? "0" : lineData[24]) + ", " + (lineData[25].equals("") ? "0" : lineData[25]) 
                            + ", " + (lineData[26].equals("") ? "0" : lineData[26]) + ", '" + lineData[27] + "', '" + lineData[28]
                            + "', '" + lineData[29] + "', '" + lineData[30] + "', '" + lineData[31] + "' ), ";
                }

                
                if(i > 0 && i%bundle == 0){
                    System.out.println("insert record ... " + (i));
                    JdbcDao.update(InsertSQL);
                    CommonUtils.Sleep(1);


                    InsertSQL = "insert into building (jusoemd_code,sid,sig,emd,road_code,road_name,basement,building_no1,building_no2,post_code,id,"
                    + "building_name,building_use,district_code,district_name,level_high,level_low,building_type,building_count,building_name2,building_history,building_history2,"
                    + "building_live,centerX,centetY,enterX,entetY,sid_eng,sig_eng,emd_eng,road_name_eng,emd_type ) values ";
                }

                i++;


            }

            System.out.println("insert record ... " + (i));

            InsertSQL = UtilString.trimString(InsertSQL, 2, false) + " ;";
            JdbcDao.update(InsertSQL);

        } catch (Exception e) {
            System.out.println("error query = " + InsertSQL);
            e.printStackTrace();
        }
                
        System.out.println("time = " + DateTimeUtils.getTimeDifferenceNow(start_time));    


        return true;
    }
    
    // 지번 주소 텍스트 데이터를 읽어서 DB에 저장
    private static boolean insertAddressPoi(String fileName){

        
        Date start_time = new Date(System.currentTimeMillis());
        
        String InsertSQL = "insert into address (district_code,sid,sig,emd,ri,san,jibun_no1,jibun_no2,road_code,basement,building_no1,building_no2,id,"
                + "sid_eng,sig_eng,emd_eng,ri_eng,move_type,building_manage_no,jusoemd_code ) values ";

        int i=1;
        int count = 0;
        int bundle = 5000; // 쿼리가 많은 경우 5000개씩 쪼개서 인서트 함

                
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {

            String line = "";
            while ((line = reader.readLine()) != null) {
                line = line.replace("\'", " ");    
                String[] lineData = line.split("\\|") ;

                 if (i%bundle == 0) {
                    InsertSQL = InsertSQL + "('" + lineData[0] +  "', '" + lineData[1] + "', '" + lineData[2] + "', '" + lineData[3] + "', '" + lineData[4]
                            + "', '" + lineData[5] + "', " + (lineData[6].equals("") ? "0" : lineData[6]) + ", " + (lineData[7].equals("") ? "0" : lineData[7])  
                            + ", '"   + lineData[8] + "', '" + lineData[9] + "', " + (lineData[10].equals("") ? "0" : lineData[10]) + ", " + (lineData[11].equals("") ? "0" : lineData[11]) 
                            + ", " + (lineData[12].equals("") ? "0" : lineData[12]) + ", '" + lineData[13] + "', '" + lineData[14] + "', '" + lineData[15] + "', '" + lineData[16]  
                            + "', '" + lineData[17] + "', '" + lineData[18] + "', '" + lineData[19] + "' ); ";

                } else {
                    InsertSQL = InsertSQL + "('" + lineData[0] +  "', '" + lineData[1] + "', '" + lineData[2] + "', '" + lineData[3] + "', '" + lineData[4]
                            + "', '" + lineData[5] + "', " + (lineData[6].equals("") ? "0" : lineData[6]) + ", " + (lineData[7].equals("") ? "0" : lineData[7])  
                            + ", '"   + lineData[8] + "', '" + lineData[9] + "', " + (lineData[10].equals("") ? "0" : lineData[10]) + ", " + (lineData[11].equals("") ? "0" : lineData[11]) 
                            + ", " + (lineData[12].equals("") ? "0" : lineData[12]) + ", '" + lineData[13] + "', '" + lineData[14] + "', '" + lineData[15] + "', '" + lineData[16]  
                            + "', '" + lineData[17] + "', '" + lineData[18] + "', '" + lineData[19] + "' ), ";

                }

                if(i > 0 && i%bundle == 0){
                    System.out.println("insert record ... " + (i));
                    JdbcDao.update(InsertSQL);
                    CommonUtils.Sleep(1);


                    InsertSQL = "insert into address (district_code,sid,sig,emd,ri,san,jibun_no1,jibun_no2,road_code,basement,building_no1,building_no2,id,"
                        + "sid_eng,sig_eng,emd_eng,ri_eng,move_type,building_manage_no,jusoemd_code ) values ";
                }

                i++;


            }

            System.out.println("insert record ... " + (i));

            InsertSQL = UtilString.trimString(InsertSQL, 2, false) + " ;";
            JdbcDao.update(InsertSQL);

        } catch (Exception e) {
            e.printStackTrace();
        }
                
        System.out.println("time = " + DateTimeUtils.getTimeDifferenceNow(start_time));    


        return true;
    }

}
