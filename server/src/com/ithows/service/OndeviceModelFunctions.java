/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ithows.service;

import com.ithows.AppConfig;
import com.ithows.JdbcDao;
import com.ithows.ResultMap;
import com.ithows.util.DateTimeUtils;
import com.ithows.util.ExportImportUtils;
import com.ithows.util.UtilFile;
import com.ithows.util.UtilJSON;
import com.ithows.util.UtilString;
import com.ithows.util.ZipUtils;
import com.sox.ltex.model.primeConst;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import com.sox.ltex.model.shape.MBR;

/**
 *
 * @author mailt
 */
public class OndeviceModelFunctions {

    
    private static final int EXPORT_TYPE_CSV = 1;
    private static final int EXPORT_TYPE_SQL = 2;
    
    
    public static String exportOndeviceModel(MBR mbr) {

        
        System.out.println("\n\n//////////////////////////////////////////////////////////////////");
        System.out.println("//  exportOndeviceModel ");


        String OS = System.getProperty("os.name").toLowerCase();
        String curPath = "";

        if (OS.indexOf("win") >= 0) {
            if( AppConfig.getConf("context_win_dir") != null && !AppConfig.getConf("context_win_dir").equals("")) {
                curPath = AppConfig.getConf("context_win_dir") ;
            }else{
                curPath = "C:/locationService/";
            }

        } else {
            if( AppConfig.getConf("context_linux_dir") != null && !AppConfig.getConf("context_linux_dir").equals("")) {
                curPath = AppConfig.getConf("context_dir") ;
            }else{
                curPath = "/locationService/";
            }
        }
        
        if( AppConfig.getConf("temp_dir") != null && !AppConfig.getConf("temp_dir").equals("")) {
            curPath += AppConfig.getConf("temp_dir")  ;
        }else{
            curPath += "temp/";
        }

        System.out.println("curPath = " + curPath);

        Connection conn = JdbcDao.getConnection(); // DriverManager.getConnection(jdbcUrl1, username1, password1);
        if (conn == null) {
            System.out.println("Connect Error");
            return null;
        }
        
        String fileName = "oflp_model_" + DateTimeUtils.getTimeDateNow2() ;

        String dbName = "ondeviceflp";
        String tableName = "globalgrid" + primeConst.nationCode +"";
        
        String extentString = "";
        if(mbr != null){
            extentString = "ST_Intersects(extent, ST_PolyFromText('POLYGON(("
                    + mbr.minX + " " + mbr.minY + ","
                    + mbr.minX + " " + mbr.maxY + ","
                    + mbr.maxX + " " + mbr.maxY + ","
                    + mbr.maxX + " " + mbr.minY + ","
                    + mbr.minX + " " + mbr.minY + "))')) ";
        }
        
        String sql = "WHERE " + extentString  ;
        

        // 리스트에 대한 export
        String[] exceptField = {"id", "extent", "genlogid", "corpcount", "lpciKey_b"};
        String resultFilename = ExportImportUtils.exportNormalSQL( conn, sql, dbName, tableName, exceptField, curPath, fileName, EXPORT_TYPE_SQL, false, true) ;


        return resultFilename;
    }

    // 압축 여부 : isZip
    // 비번 설정 : passwd
    public static String makeRequestLogToJSonZipFile(ArrayList<ResultMap> list, String backupDir, boolean isZip, String passwd) {

        String jsonField = "requestparam";
        String requestTime = "";

        String fileName = "";
        String filePath = "";
        String fileNameNoExt = "";

        try {

            filePath = backupDir;

            // 디렉토리가 없으면 생성
            File dir = new File(filePath);
            if (!dir.exists()) {
                dir.mkdir();
            }

            for (ResultMap map : list) {
                fileNameNoExt = jsonField;
                requestTime = map.getString("requestTime");
                requestTime = requestTime.replaceAll("-", "");
                requestTime = requestTime.replaceAll(":", "");
                requestTime = requestTime.replaceAll(" ", "_");
                fileName = filePath + "requestlog_" + requestTime + ".json";

                String jsonStr = map.getString(jsonField);
                jsonStr = UtilJSON.JSonBeautify(jsonStr);

                ExportImportUtils.writeStringToFile(jsonStr, fileName);

            }

            if (isZip) {
                String zipFileName = backupDir + jsonField + ".zip";

                if (passwd.equals("")) {

                    if (ZipUtils.zipFiles(backupDir, zipFileName, ".json")) {
                        System.out.println("Zip File size = " + UtilString.convertNumberToCommaString(UtilFile.getFileSize(zipFileName)) + " bytes");
                        fileName = zipFileName;
                    }
                } else {

                    if (ZipUtils.zipFilesWithPW(backupDir, zipFileName, "1234", ".json")) {

                        System.out.println("Encrypt Zip File size = " +  UtilString.convertNumberToCommaString(UtilFile.getFileSize(zipFileName)) + " bytes");
                        fileName = zipFileName;
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            fileName = "";
        }

        return fileName;

    }    
}
