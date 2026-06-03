/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ithows.util;

import com.ithows.ResultMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author mailt
 */
public class ExportImportUtils {
    

    // sql 결과를 csv나 sql로 export
    public static String exportNormalSQL(Connection conn, String sqlWhere, String dbName, String tableName, String[] exceptField, String path, String backupFileName,
                                       int type, boolean isBundle, boolean zipOption) {

        String result = null;
        
        if(conn == null || sqlWhere == null || sqlWhere.isEmpty() || dbName == null || dbName.isEmpty() || tableName == null || tableName.isEmpty()){
            System.out.println("exportNormalSQL : 잘못된 파라미터");
            return result;
        }

        if(type == 1) {
            //csv
            backupFileName = path + backupFileName + "_csv.csv"; //  경로

            try {
                result = dumpQueryToCSV(conn, tableName, sqlWhere, backupFileName, exceptField, "|", zipOption) ;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }else if(type == 2){
            // sql
            backupFileName = path + backupFileName + ".sql"; //  경로
            result = dumpQueryToSQL(conn, dbName, tableName, sqlWhere, backupFileName, exceptField, isBundle, zipOption);

        }
        
        return result; 

    }


    // SQL 쿼리 결과를 Sql 덤프파일로 생성 하기
    // @@ FetchSize를 설정하여 OutOfMemoryError 방지
    // @@ exceptField : 제외할 필드
    public static String dumpQueryToSQL(Connection conn, String dbName, String tableName, String whereString, String backupName, String[] exceptField, boolean isBundle, boolean zipOption){
        String fileName = "";

        String zipFileName = "";
        String fileNameNoExt = "";
        boolean zipResult = false;

        System.out.println("\n\n//////////////////////////////////////////////////////////////////");
        System.out.println("//  make Table To Dump ");

        long rowCount = 0;

        try {

            // 메타데이터를 가져오기 위한 쿼리
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, null);

            List<String> columnNames = new ArrayList<>();

            // 컬럼명 목록 가져오기
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                if (!contains(exceptField, columnName)) {
                    columnNames.add(columnName);
                }
            }

            String selectQuery = "";

            // SELECT 문 생성
            selectQuery = "SELECT " + String.join(", ", columnNames) +
                    " FROM " + tableName + " " +
                    whereString + " ; ";

            System.out.println(selectQuery);

            String insertSQL = "insert into " + tableName + " ( " + String.join(", ", columnNames) + " ) values " ;

            try {
                String curPath = "";

                curPath = FilenameUtils.getFullPath(backupName);
                fileNameNoExt = FilenameUtils.getBaseName(backupName);
                fileName = curPath + fileNameNoExt + ".sql";

                System.out.println(fileName);

                try (OutputStreamWriter csvFile = new OutputStreamWriter(new FileOutputStream(new File(fileName), false))){

//                    csvFile.write("USE `" + dbName + "`; \n");
//                    csvFile.write("LOCK TABLES `" + tableName + "` WRITE; \n\n");


//                System.out.println(sqlStr);
                    if(isBundle){
                        rowCount = queryBundleCSVFile(conn, selectQuery, insertSQL, exceptField, csvFile);
                        
                    }else{
                        rowCount = queryCSVFile(conn, selectQuery, insertSQL, exceptField, csvFile);
                    }

//                    csvFile.write("\nUNLOCK TABLES;\n");

                    csvFile.close();
                }


                if(rowCount == 0){
                    
                    return null;
                }
                
                if(zipOption == true && rowCount > 0){
                    zipFileName = curPath + fileNameNoExt + ".zip";
                    System.out.println("압축을 시작합니다 : " + zipFileName);

                    if(ZipUtils.zipFile(fileName, zipFileName, "")){
                        System.out.println("Zip File size = " + UtilString.convertNumberToCommaString(UtilFile.getFileSize(zipFileName)) + " bytes");

                        UtilFile.deleteFile(fileName);
                    }else{
                        System.out.println("압축 실패");
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
                return null;
            }


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if(zipOption){
            return zipFileName;
        }else{
            return fileName;
            
        }
    }

    // 묶음 insert 문 만들기 
    private static int queryBundleCSVFile(Connection conn,  String sp_query, String insertSql , String[] exceptField, OutputStreamWriter csvFile) throws Exception {
        if (sp_query == null || conn == null) {
            throw new Exception("쿼리 조건이나 환경이 구성되지 않음");
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int rowcount = 0 ;
        int BATCH_SIZE = 1000 ;
        int cnt = 0;

        try {
            pstmt = conn.prepareStatement(sp_query);
            pstmt.setFetchSize(BATCH_SIZE);   // 1000개씩 가져오기

            boolean hasResults = pstmt.execute();
            rs = pstmt.getResultSet();


            /*----------------------------------------------------------------*/
            ResultSetMetaData rsMetaData = rs.getMetaData();

            String rowString = "";
            StringBuffer rowBuffer = new StringBuffer();


            while (rs.next()) {
                rowBuffer.setLength(0);

                for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {

                    if (contains(exceptField, rsMetaData.getColumnLabel(i))) {
                        continue;
                    }

                    if(rs.getObject(i) != null ) {
                        rowBuffer.append("'" + rs.getObject(i) + "',");
                    }else{
                        rowBuffer.append("'',");
                    }
                }


                rowString = rowBuffer.toString();

                cnt++;
                if (cnt % BATCH_SIZE == 1) {
                    // 첫 번째 배치에서 insert SQL 작성
                    csvFile.write(insertSql);
                }

                if (!rowString.isEmpty() && rowString.endsWith(",")) {
                    rowString = rowString.substring(0, rowString.length() - 1);
                }

                if (cnt % BATCH_SIZE == 0 ) {
                    // 매 BATCH_SIZE마다 파일에 작성하고 flush
                    csvFile.write("(" + rowString + ") ; \n");
                    csvFile.flush();  // 매 BATCH_SIZE마다 파일에 flush
                } else if(rs.isLast()) {
                    csvFile.write("(" + rowString + ") ; \n");
                    csvFile.flush();  // 매 BATCH_SIZE마다 파일에 flush
                } else {
                    csvFile.write( "(" + rowString + "),");
                }

            }

            /*----------------------------------------------------------------*/
            csvFile.flush();




        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage() + "\ncatch:" + sp_query);
            throw e;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getLocalizedMessage() + "\ncatch :" + sp_query);
                throw ex;
            }
        }
        return cnt;
    }

    // 레코드별로 insert문 만들기 
    private static int queryCSVFile(Connection conn,  String sp_query, String insertSql , String[] exceptField, OutputStreamWriter csvFile) throws Exception {
        if (sp_query == null || conn == null) {
            throw new Exception("쿼리 조건이나 환경이 구성되지 않음");
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int rowcount = 0 ;
        int BATCH_SIZE = 1000 ;
        int cnt = 0;

        try {
            pstmt = conn.prepareStatement(sp_query);
            pstmt.setFetchSize(BATCH_SIZE);   // 1000개씩 가져오기

            boolean hasResults = pstmt.execute();
            rs = pstmt.getResultSet();


            /*----------------------------------------------------------------*/
            ResultSetMetaData rsMetaData = rs.getMetaData();

            String rowString = "";
            StringBuffer rowBuffer = new StringBuffer();


            while (rs.next()) {
                rowBuffer.setLength(0);

                for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {

                    if (contains(exceptField, rsMetaData.getColumnLabel(i))) {
                        continue;
                    }

                    if(rs.getObject(i) != null ) {
                        rowBuffer.append("'" + rs.getObject(i) + "',");
                    }else{
                        rowBuffer.append("'',");
                    }
                }


                rowString = rowBuffer.toString();

                cnt++;

                csvFile.write(insertSql);

                if (!rowString.isEmpty() && rowString.endsWith(",")) {
                    rowString = rowString.substring(0, rowString.length() - 1);
                }

                csvFile.write("(" + rowString + ") ; \n");
                csvFile.flush();  // 매 BATCH_SIZE마다 파일에 flush

            }


        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage() + "\ncatch:" + sp_query);
            throw e;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getLocalizedMessage() + "\ncatch :" + sp_query);
                throw ex;
            }
        }
        return cnt;
    }


    // 쿼리 결과를 바로 CSV 덤프파일 만들기 (CSV)
    // @@ FetchSize를 설정하여 OutOfMemoryError 방지
    public static String dumpQueryToCSV(Connection conn, String tableName, String whereString, String backupName, String[] exceptField, String sep, boolean zipOption) throws Exception {


        System.out.println("\n\n//////////////////////////////////////////////////////////////////");
        System.out.println("//  make SQL To CSVFile ");

        String resultName = null;
        
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int BATCH_SIZE = 1000 ;
        int cnt = 0;


        OutputStreamWriter csvFile = null;
        try{
            if(backupName == null || backupName.isEmpty()){
                backupName = "export.csv";
            }
            csvFile = new OutputStreamWriter(new FileOutputStream(new File(backupName), false));
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            throw e;
        }

        if(sep == null || sep.isEmpty()){
            sep = ",";
        }

        try {

            // 메타데이터를 가져오기 위한 쿼리
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, null);

            List<String> columnNames = new ArrayList<>();

            // 컬럼명 목록 가져오기
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                if (!contains(exceptField, columnName)) {
                    columnNames.add(columnName);
                }
            }

            String selectQuery = "SELECT " + String.join(", ", columnNames) +
                    " FROM " + tableName + " " +
                    whereString + " ; ";

            pstmt = conn.prepareStatement(selectQuery);
            pstmt.setFetchSize(BATCH_SIZE);   // 1000개씩 가져오기


            boolean hasResults = pstmt.execute();
            rs = pstmt.getResultSet();


            /*----------------------------------------------------------------*/
            ResultSetMetaData rsMetaData = rs.getMetaData();


            StringBuffer rowBuffer = new StringBuffer();

            // @@ column 입력
            for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {

                String columnName = rsMetaData.getColumnLabel(i);
                if (!contains(exceptField, columnName)) {
                    rowBuffer.append(rsMetaData.getColumnLabel(i) + sep);
                }

            }

            if(rowBuffer.length() > 0) {
                rowBuffer.setLength(rowBuffer.length()-1);
                csvFile.write(rowBuffer.toString()+"\n");
            }



            // 데이터 입력
            while (rs.next()) {
                rowBuffer.setLength(0);

                // @@ row 입력
                for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {

                    String columnName = rsMetaData.getColumnLabel(i);
                    if (!contains(exceptField, columnName)) {
                        if (rs.getObject(i) != null) {
                            rowBuffer.append(rs.getObject(i) + sep);
                        } else {
                            rowBuffer.append("" + sep);
                        }
                    }

//                    if(rs.getObject(i) != null ) {
//                        rowBuffer.append(rs.getObject(i) + sep);
//                    }else{
//                        rowBuffer.append("" + sep);
//                    }
                }

                if(rowBuffer.length() > 0) {
                    rowBuffer.setLength(rowBuffer.length() - sep.length()); // 마지막 콤마 제거
                }


                String rowString = rowBuffer.toString();


                cnt++;

                if (cnt % BATCH_SIZE == 0 || rs.isLast()) {
                    // 매 BATCH_SIZE마다 파일에 작성하고 flush
                    csvFile.write(rowString + "\n");
                    csvFile.flush();  // 매 BATCH_SIZE마다 파일에 flush
                } else if(!rs.isLast()) {
                    csvFile.write(rowString + "\n");
                }

            }

            /*----------------------------------------------------------------*/
            csvFile.write("\n");
            csvFile.flush();



            if(zipOption == true){
                String curPath = FilenameUtils.getFullPath(backupName);
                String fileNameNoExt = FilenameUtils.getBaseName(backupName);
                String zipFileName = curPath + fileNameNoExt + ".zip";
                System.out.println("압축을 시작합니다 : " + zipFileName);

                if(ZipUtils.zipFile(backupName, zipFileName, "")){
                    System.out.println("Zip File size = " + UtilString.convertNumberToCommaString(UtilFile.getFileSize(zipFileName)) + " bytes");

                    UtilFile.deleteFile(backupName);
                    
                    resultName = zipFileName;
                }else{
                    System.out.println("압축 실패");
                    
                }

            }else{
                resultName = backupName;
            }


        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage() + "\ncatch " + whereString);
            throw e;
            
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getLocalizedMessage() + "\ncatch :" + whereString);
                throw ex;
            }
        }
        
        
        
        return resultName;
    }



    private static boolean contains(String[] array, String target) {
        if (array == null || target == null) {
            return false;
        }
        String lowerCaseTarget = target.toLowerCase();
        return Arrays.stream(array).anyMatch(s -> lowerCaseTarget.equals(s.toLowerCase()));
    }




    //<editor-fold  desc="record 단위 변환 함수들">

    ////////////////////////////////////////////////////////////////////
    // record 단위

    // Json 문자열 --> .json file
    public static boolean writeStringToFile(String jsonStr, String jsonFilName) {

        try {
            return UtilJSON.writeJsonTextToFile(jsonStr, jsonFilName);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Json --> .json file
    public static boolean writeJSonToFile(JSONObject jsonObj, String jsonFilName) {

        try {
            return UtilJSON.writeJsonToFile(jsonObj, jsonFilName);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 1 json text file --> JSON
    public static JSONObject readFileToJSon(String fileName) {

        JSONObject jObj = null;
        try {
            jObj = UtilFile.readTextToJSonObject(fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return jObj;
    }


    // ResultMap --> JSON
    public static JSONObject mapToJSon(ResultMap payload){
        JSONObject jObj = null;

        try {
            jObj = UtilJSON.mapToJSon((Map<String, Object>)payload);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return jObj;
    }

    // JSON --> ResultMap
    public static ResultMap jsonToMap(JSONObject jObj){
        ResultMap map = null;

        try {
            map = UtilJSON.jsonToMap(jObj);
        } catch (Exception ex) {  }

        return map;
    }


    //</editor-fold>


    //<editor-fold  desc="ResultMap list 단위 변환 함수들">

    ///////////////////////////////////////////////////////////////////////
    // list단위

    // json 파일 --> JSONArray
    public static JSONArray readJsonFileToJSONArray(String fileName){

        JSONArray arr = UtilFile.readTextToJSonArray(fileName);
        return arr;
    }

    // csv 파일 --> ArrayList<ResultMap>
    public static ArrayList<ResultMap> readCSVFileToMaplist(String fileName, String separator){

        ArrayList<ResultMap> mapList = CollectionUtils.readCSVFileToMaplist(fileName, separator);
        return mapList;
    }


    // ArrayList<ResultMap> --> .json 저장 + 압축
    public static String makeListToJSonZipFile(ArrayList<ResultMap> list, String backupName, boolean isZip){
        String fileName = "";

        String jsonStr = CollectionUtils.makeArrayListToJson(list,false);

        if(jsonStr == null || jsonStr.equals("")){
            return fileName;
        }

//         System.out.println("jsonStr ");

        String filePath = "";
        String fileNameNoExt = "";

        try {

            filePath = FilenameUtils.getFullPath(backupName);
            fileNameNoExt = FilenameUtils.getBaseName(backupName);
            fileName = filePath + fileNameNoExt + ".json";

            ExportImportUtils.writeStringToFile(jsonStr, fileName);

            if(isZip) {
                String zipFileName = backupName;
                if (UtilFile.zip(fileName, zipFileName)) {
                    File nfile = new File(fileName);

                    if (nfile.exists()) {
                        nfile.delete();
                    }

                    System.out.println("File size = " + UtilString.convertNumberToCommaString(UtilFile.getFileSize(zipFileName)) + " bytes");
                    fileName = zipFileName;
                }
            }


        }catch (Exception e){
            e.printStackTrace();
            fileName = "";
        }

        return fileName;

    }


    //  ArrayList<ResultMap> ( 1개 컬럼 지정 )  --> .json 파일들
    public static String makeJsonColumnToJSonZipFile(ArrayList<ResultMap> list, String jsonField, String backupDir, boolean isZip){
        String fileName = "";

        String filePath = "";
        String fileNameNoExt = "";

        try {

            filePath = backupDir;

            // 디렉토리가 없으면 생성
            File dir = new File(filePath);
            if(!dir.exists()){
                dir.mkdir();
            }

            int idx = 1;
            for(ResultMap map : list){
                fileNameNoExt = jsonField ;
                fileName = filePath + fileNameNoExt + "_" + idx + ".json";

                String jsonStr = map.getString(jsonField);
                jsonStr = UtilJSON.JSonBeautify(jsonStr);

                writeStringToFile(jsonStr, fileName);

                idx++;
            }

//            System.out.println("jsonFile size = " + UtilFile.getFileSize(fileName));

            if(isZip) {
                String zipFileName = filePath + jsonField + ".zip";
                if (ZipUtils.zipFiles(filePath, zipFileName, ".json")) {

                    System.out.println("Zip File size = " + UtilString.convertNumberToCommaString(UtilFile.getFileSize(zipFileName)) + " bytes");
                    fileName = zipFileName;
                }
            }


        }catch (Exception e){
            e.printStackTrace();
            fileName = "";
        }

        return fileName;

    }


    //  ArrayList<ResultMap> ( 1개 컬럼 지정 )  --> .json 파일들
    public static String makeJsonColumnToZipFile(ArrayList<ResultMap> list, String jsonField, String backupDir, boolean isZip){
        String fileName = "";

        String filePath = "";
        String fileNameNoExt = "";

        try {

            filePath = backupDir;

            // 디렉토리가 없으면 생성
            File dir = new File(filePath);
            if(!dir.exists()){
                dir.mkdir();
            }

            int idx = 1;
            for(ResultMap map : list){
                fileNameNoExt = jsonField ;
                fileName = filePath + fileNameNoExt + "_" + idx + ".json";

                String jsonStr = "{ \"" + jsonField + "\" : \"" +  map.getString(jsonField) + "\"}\n";

                UtilFile.writeTextToFile(jsonStr, fileName);

                idx++;
            }

//            System.out.println("jsonFile size = " + UtilFile.getFileSize(fileName));

            if(isZip) {
                String zipFileName = filePath + jsonField + ".zip";
                if (ZipUtils.zipFiles(filePath, zipFileName, ".json")) {

                    System.out.println("Zip File size = " + UtilString.convertNumberToCommaString(UtilFile.getFileSize(zipFileName)) + " bytes");
                    fileName = zipFileName;
                }
            }


        }catch (Exception e){
            e.printStackTrace();
            fileName = "";
        }

        return fileName;

    }


    // ArrayList<ResultMap> --> .csv 저장 + 압축
    public static String makeListToCSVFile(ArrayList<ResultMap> list, String separator, String backupName, String exceptField, boolean zipOption ){
        String fileName = "";
        if(list.size() < 1){
            return "";
        }

        String zipFileName = "";
        boolean zipResult = false;

        String fileNameNoExt = "";

        try {
            String curPath = "";

            curPath = FilenameUtils.getFullPath(backupName);
            fileNameNoExt = FilenameUtils.getBaseName(backupName);
            fileName = curPath + fileNameNoExt + ".csv";

            System.out.println("curPath = " + curPath);
            System.out.println("fileNameNoExt = " + fileNameNoExt);
            System.out.println("fileName = " + fileName);

            OutputStreamWriter csvFile = new OutputStreamWriter(new FileOutputStream(new File(fileName), false));

            String csvStr = "" ;
            String fieldName = "" ;
            String fieldValue = "";
            String subValue = "";
            ResultMap map = list.get(0);
            int i=1;
            int j=1;

            // 필드 채우기
            for( Object key : map.keySet() ){

                // 제외 필드
                if (!exceptField.isEmpty() && key.toString().equals(exceptField)) {
                    i++;
                    continue;
                }

                if(i == map.size()){
                    fieldName = fieldName + key + "\n";
                }else{
                    fieldName = fieldName + key + separator + "";
                }
                i++;
            }
            csvFile.write(fieldName);

            for(ResultMap element : list) {
                i=1;
                subValue = "" ;
                for (Object key : element.keySet()) {


                    // 제외 필드
                    if (!exceptField.isEmpty() && key.toString().equals(exceptField)) {
                        i++;
                        continue;
                    }

                    if (i == element.size()) {
                        subValue = subValue + element.get(key) + "";
                    } else {
                        subValue = subValue + element.get(key) +  separator + "";
                    }
                    i++;
                }

                if(j == list.size()){
                    csvFile.write(subValue + "");

                }else{
                    csvFile.write(subValue + "\n");
                }
                j++;
            }

            csvFile.flush();
            csvFile.close();

            if(zipOption == true){
                zipFileName = curPath + fileNameNoExt + ".zip";
                if(UtilFile.zip(fileName, zipFileName)){
                    File nfile = new File(fileName);
                    if (nfile.exists()) {
                        nfile.delete();
                    }
                    fileName = zipFileName;
                }

            }

        }catch (Exception e){
            e.printStackTrace();
            fileName = "";
        }

        return fileName;
    }

    // ArrayList<ResultMap> --> .sql 저장 + 압축
    public static String makeListToSqlDump(ArrayList<ResultMap> list, String backupName, String dbName, String tableName, String exceptField, boolean zipOption ){
        String fileName = "";
        if(list.size() < 1){
            return "";
        }

        String separator = "|";
        String zipFileName = "";
        boolean zipResult = false;

        String fileNameNoExt = "";

        try {
            String curPath = "";

            curPath = FilenameUtils.getFullPath(backupName);
            fileNameNoExt = FilenameUtils.getBaseName(backupName);
            fileName = curPath + fileNameNoExt + ".sql";

            System.out.println(fileName);

            try (OutputStreamWriter csvFile = new OutputStreamWriter(new FileOutputStream(new File(fileName), false))){

                csvFile.write("USE `"+ dbName +"`; \n");
                csvFile.write("LOCK TABLES `" + tableName + "` WRITE; \n\n");

                String sqlStr = makeListToInsertSQL( list,  tableName, exceptField);
//                System.out.println(sqlStr);

                csvFile.write(sqlStr);
                csvFile.write("\nUNLOCK TABLES;\n");

                csvFile.close();
            }


            if(zipOption == true){
                zipFileName = curPath + fileNameNoExt + ".zip";
                System.out.println(zipFileName);
                if(ZipUtils.zipFile(fileName, zipFileName, "")){
                    System.out.println("Zip File size = " + UtilString.convertNumberToCommaString(UtilFile.getFileSize(zipFileName)) + " bytes");
                    fileName = zipFileName;
                }

            }

        }catch (Exception e){
            e.printStackTrace();
            fileName = "";
        }

        return fileName;
    }


    // ArrayList<ResultMap> --> sql 문(스트링)
    // ResultMap 리스트로 인서트 문 만들기
    // exceptField : 제외할 필드
    private static String makeListToInsertSQL(ArrayList<ResultMap> list, String tableName, String exceptField) {

        if (list.size() < 1) {
            return "No data";
        }

        String InsertSQL = "";
        String sqlStr = "";
        String fieldName = "(";
        String fieldValue = "";
        String subValue = "";
        ResultMap map = list.get(0);
        int i = 1; // PK를 위한 예비
        int j = 0;
        int count = list.size();


        // 필드 채우기
        for (Object key : map.keySet()) {

            if (!exceptField.isEmpty() && key.toString().equals(exceptField)) {
                i++;
                continue;
            }

            if (i == map.size()) {
                fieldName = fieldName + "`" + key + "`) \n";
            } else {
                fieldName = fieldName + "`" + key + "`, ";
            }
            i++;
        }

        sqlStr = "INSERT INTO " + tableName + " " + fieldName + "VALUES ";
        ;
        InsertSQL += sqlStr;

        int bundle = 3;
        for (ResultMap element : list) {
            i = 1;
            subValue = "(";
            for (Object key : element.keySet()) {

                if(!exceptField.isEmpty() && key.toString().equals(exceptField)){
                    i++;
                    continue;
                }

                if (i == element.size()) {
                    if (UtilString.isValidNumeric(element.getString(key.toString())) > 0) {
                        subValue = subValue + "" + element.get(key) + ")";

                    } else {
                        subValue = subValue + "'" + element.get(key) + "')";

                    }
                } else {
                    if (UtilString.isValidNumeric(element.getString(key.toString())) > 0) {
                        subValue = subValue + "" + element.get(key) + ", ";

                    } else {
                        subValue = subValue + "'" + element.get(key) + "', ";
                    }

                }
                i++;
            }


            if (j % bundle == (bundle - 1) || j == (count - 1)) {
                InsertSQL += subValue + ";\n\n";

                if (j < (count - 1)) {  // 맨 마지막 행은 안 붙임
                    InsertSQL += sqlStr;
                }

            } else {
                InsertSQL += subValue + ",\n";
            }
            j++;
        }

        return InsertSQL;
    }



    //</editor-fold>


    
}
