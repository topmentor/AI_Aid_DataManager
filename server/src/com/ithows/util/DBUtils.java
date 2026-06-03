/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

import com.ithows.AppConfig;
import com.ithows.ResultMap;
import com.ithows.service.UploadConst;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import org.apache.commons.io.FilenameUtils;

public class DBUtils {

    public static void sortResultMapList(ArrayList<ResultMap> list, String sortField, boolean desc){
        
        if(list == null || list.size()== 0 || list.get(0).get(sortField) == null){
            
            return;
        }
        
        ResultMap.compareField = sortField;
        
        if(desc){
            list.sort(Comparator.reverseOrder());  // 내림 차순 정렬
            
        }else{
            list.sort(Comparator.naturalOrder());  // 오름 차순 정렬
            
        }
        
    }
    
    public static String makeListToInsertSQL(ArrayList<ResultMap> list, String tableName){

        if(list.size() < 1){
            return "No data";
        }

        String sqlStr = "INSERT INTO " + tableName + " " ;
        String fieldName = "(";
        String fieldValue = "";
        String subValue = "";
        ResultMap map = list.get(0);
        int i=1;
        int j=1;

        // 필드 채우기
        for( Object key : map.keySet() ){
            if(i == map.size()){
                fieldName = fieldName + "`" + key + "`) \n";
            }else{
                fieldName = fieldName + "`" + key + "`, ";
            }
            i++;
        }

        for(ResultMap element : list) {
            i=1;
            subValue = "(";
            for (Object key : element.keySet()) {
                if (i == element.size()) {
                    if(element.get(key) instanceof String){
                        subValue = subValue + "'" + element.get(key) + "')";
                    }else{
                        subValue = subValue + element.get(key) + " )";
                    }
                } else {
                    if(element.get(key) instanceof String){
                        subValue = subValue + "'" + element.get(key) + "', ";
                    }else{
                        subValue = subValue + element.get(key) + ", ";
                    }

                }
                i++;
            }

            if(j == list.size()){
                fieldValue = fieldValue + subValue + " ";

            }else{
                fieldValue = fieldValue + subValue + ",\n";
            }
            j++;
        }
        sqlStr = sqlStr + fieldName + "VALUES " + fieldValue + " ;" ;


        return sqlStr;
    }
    
    public static String makeListToInsertSQL(ArrayList<ResultMap> list, String tableName, String exceptColumns){

        if(list.size() < 1){
            return "No data";
        }

        String sqlStr = "INSERT INTO " + tableName + " " ;
        String fieldName = "(";
        String fieldValue = "";
        String subValue = "";
        ResultMap map = list.get(0);
        int i=1;
        int j=1;

        // 필드 채우기
        for( Object key : map.keySet() ){
            if(UtilString.withInString(exceptColumns, (String)key) == -1){
                if(i == map.size()){
                    fieldName = fieldName + "`" + key + "`) \n";
                }else{
                    fieldName = fieldName + "`" + key + "`, ";
                }
            }
            i++;
        }

        for(ResultMap element : list) {
            i=1;
            subValue = "(";
            for (Object key : element.keySet()) {
                if(UtilString.withInString(exceptColumns, (String)key) == -1){
                    if (i == element.size()) {
                        if( element.get(key) instanceof String ){
                            subValue = subValue + "'" + element.get(key) + "')";
                        }else{
                            subValue = subValue + element.get(key) + " )";
                        }
                    } else {
                        if( element.get(key) instanceof String ){
                            subValue = subValue + "'" + element.get(key) + "', ";
                        }else{
                            subValue = subValue + element.get(key) + ", ";
                        }

                    }
                }
                i++;
            }

            if(j == list.size()){
                fieldValue = fieldValue + subValue + " ";

            }else{
                fieldValue = fieldValue + subValue + ",\n";
            }
            j++;
        }
        sqlStr = sqlStr + fieldName + "VALUES " + fieldValue + " ;" ;


        return sqlStr;
    }
    
    
    
    public static String makeListToCSV(ArrayList<ResultMap> list, String separator){

        if(list.size() < 1){
            return "No data";
        }

        String csvStr = "" ;
        String fieldName = "";
        String fieldValue = "";
        String subValue = "";
        ResultMap map = list.get(0);
        int i=1;
        int j=1;

        // 필드 채우기
        for( Object key : map.keySet() ){
            if(i == map.size()){
                fieldName = fieldName + key + " \n";
            }else{
                fieldName = fieldName + key + separator + " ";
            }
            i++;
        }

        for(ResultMap element : list) {
            i=1;
            subValue = "";
            for (Object key : element.keySet()) {
                if (i == element.size()) {
                    subValue = subValue + element.get(key) + " ";
                } else {
                    subValue = subValue + element.get(key) +  separator + " ";
                }
                i++;
            }

            if(j == list.size()){
                fieldValue = fieldValue + subValue + " ";

            }else{
                fieldValue = fieldValue + subValue + "\n";
            }
            j++;
        }
        csvStr = csvStr + fieldName + fieldValue + "\n" ;


        return csvStr;
    }    

    public static String makeResultMapToInsertSQL(ResultMap map, String tableName){
        String sqlStr = "INSERT INTO " + tableName + " " ;
        String fieldName = "";
        String fieldValue = "";
        int i=1;
        for( Object key : map.keySet() ){
            if(i == map.size()){
                fieldName = fieldName + "`" + key + "` ";
                fieldValue = fieldValue + "'" + map.get(key) + "' ";
            }else{
                fieldName = fieldName + "`" + key + "`, ";
                fieldValue = fieldValue + "'" + map.get(key) + "', ";
            }
            i++;
        }
        sqlStr = sqlStr + "(" + fieldName + ") " + "VALUES (" + fieldValue + ");" ;

        return sqlStr;
    }

    
    /**
     * 쿼리 결과를 csv로 저장 후 압축 
     * 파일의 저장경로는 'config_file_exceldown_url'에 설정 됨
     * @param list
     * @param separator
     * @param zipOption
     * @return 
     */
    public static String makeListToFile(ArrayList<ResultMap> list, String separator, boolean zipOption ){
        String fileName = "";
        if(list.size() < 1){
            return "";
        }

        String zipFileName = "";
        boolean zipResult = false;

        try {
            
            String currPath = UploadConst.resourcePath("config_file_exceldown_url"); 
            
            // 디렉토리가 없으면 생성
            File dir = new File(currPath);
            if(!dir.exists()){
                    dir.mkdir();
            }
            
            fileName = currPath + "export_" + DateTimeUtils.getTimeDateNow2() + ".csv";

            OutputStreamWriter csvFile = new OutputStreamWriter(new FileOutputStream(new File(fileName), false));

            String csvStr = "" ;
            String fieldName = "";
            String fieldValue = "";
            String subValue = "";
            ResultMap map = list.get(0);
            int i=1;
            int j=1;

            // 필드 채우기
            for( Object key : map.keySet() ){
                if(i == map.size()){
                    fieldName = fieldName + key + " \n";
                }else{
                    fieldName = fieldName + key + separator + " ";
                }
                i++;
            }
            csvFile.write(fieldName);

            for(ResultMap element : list) {
                i=1;
                subValue = "";
                for (Object key : element.keySet()) {
                    if (i == element.size()) {
                        subValue = subValue + element.get(key) + " ";
                    } else {
                        subValue = subValue + element.get(key) +  separator + " ";
                    }
                    i++;
                }

                if(j == list.size()){
                    csvFile.write(subValue + " ");

                }else{
                    csvFile.write(subValue + "\n");
                }
                j++;
            }

            csvFile.flush();
            csvFile.close();

            
            if(zipOption == true){
                zipFileName = currPath + File.separator + "export_" + DateTimeUtils.getTimeDateNow2() + ".zip";
                if(UtilFile.zip(fileName, zipFileName)){
                    File nfile = new File(fileName);
                    if (nfile.exists()) {
                        System.out.println("delete : " +  nfile.delete());
                    }
                    fileName = FilenameUtils.getName(zipFileName);
                }

            }else{
                fileName = FilenameUtils.getName(fileName);
            }

        }catch (Exception e){
            e.printStackTrace();
            fileName = "";
        }


        
        return fileName;
    }
}
