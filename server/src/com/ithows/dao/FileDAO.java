/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.dao;

import com.ithows.CommonUtils;
import com.ithows.JdbcDao2;
import com.ithows.ResultMap;
import com.ithows.service.UploadConst;
import com.ithows.util.UtilFile;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author ksyuser
 */
public class FileDAO {
    
    public final static int FILESORT_DATE_DESC = 1;
    public final static int FILESORT_DATE_ASC = 2;
    public final static int FILESORT_NAME_DESC = 3;
    public final static int FILESORT_NAME_ASC = 4;
    public final static int FILESORT_USER_DESC = 5;
    public final static int FILESORT_USER_ASC = 6;

    public final static int FILEPERMISSION_ALL = 7;
    public final static int FILEPERMISSION_DOWNLOAD = 1;
    public final static int FILEPERMISSION_VIEW = 2;
    public final static int FILEPERMISSION_DELETE = 4;    
    
    
    public static int FILE_TYPE_C_NMF = 0;
    public static int FILE_TYPE_D_NMF = 1;
    public static int FILE_TYPE_GEO2 = 10;
    
    
    public static void main(String[] args) {
     
        
        
    }
    
       
    
    /**
     * nmf 파일 삭제
     * @param fileId
     * @param filetype  : 0이면 수집데이터, 1이면 측위데이터, 10이면 지오투 데이터
     * @throws SQLException 
     */
    public static void deleteNMFFile(int fileId, int filetype) throws SQLException {
        
        String sql = "select * from nmf_file "
                + "where id=? ";
        
        ResultMap map = JdbcDao2.queryForMapObject(sql, new Object[]{fileId}); 
        String fileName = ""; 
        
        // 파일 삭제
        if(map.getInt("filetype") == FILE_TYPE_C_NMF){  // 수집 nmf
            fileName = UploadConst.resourcePath("config_cnmf_upload_url") +  map.getString("filename") ;
            UtilFile.deleteFile(fileName);

        }else if(map.getInt("filetype") == FILE_TYPE_GEO2){  // geo2 수집
            fileName = UploadConst.resourcePath("config_geo2_download_url") +  map.getString("filename") ;
            UtilFile.deleteFile(fileName);
        }else{  // 측위 nmf
            fileName = UploadConst.resourcePath("config_dnmf_upload_url") +  map.getString("filename") ;
            UtilFile.deleteFile(fileName);
        }
        
        JdbcDao2.update("DELETE FROM nmf_file where id = ? ; " ,  new Object[]{fileId});
        
        if(filetype == 1){
            JdbcDao2.update("DELETE FROM device_sig where nfile = ? ; " ,  new Object[]{fileId});
        }else{
            
            if(map.getInt("filetype") == FILE_TYPE_C_NMF){   // nmf 인 경우만 지움 
                JdbcDao2.update("DELETE FROM raw_sig where nfile = ? ; " ,  new Object[]{fileId});
                CommonUtils.Sleep(2);

            }
            JdbcDao2.update("DELETE FROM collectxy where nfile = ? ; " ,  new Object[]{fileId});
        }
        
    }
    
    public static void deleteDeviceSigItem(int Id) throws SQLException {
        JdbcDao2.update("DELETE FROM device_sig where id = ? ; " ,  new Object[]{Id});
    }
    
    public static void removeAllNMFFile() throws SQLException{
        // 물리적 파일 지우기
        String sql = "select * from nmf_file ;";
               
        ArrayList<ResultMap> list = (ArrayList<ResultMap>)JdbcDao2.queryForMapList(sql, new Object[]{}); 
        
        for(ResultMap map : list){
            if(map.getInt("filetype") == 0){
                String fileName = UploadConst.resourcePath("config_cnmf_upload_url") +  map.getString("filename") ;
                UtilFile.deleteFile(fileName);
                
            }else if(map.getInt("filetype") == 10){
                String fileName = UploadConst.resourcePath("config_geo2_download_url") +  map.getString("filename") ;
                UtilFile.deleteFile(fileName);
            }else{
                String fileName = UploadConst.resourcePath("config_dnmf_upload_url") +  map.getString("filename") ;
                UtilFile.deleteFile(fileName);
            }
        }
        
        UtilFile.deleteFile(UploadConst.resourcePath("config_geo2_download_url") +  "session.csv");
        UtilFile.deleteFile(UploadConst.resourcePath("config_geo2_download_url") +  "payload.csv");
        UtilFile.deleteFile(UploadConst.resourcePath("config_geo2_download_url") +  "lte.csv");
        UtilFile.deleteFile(UploadConst.resourcePath("config_geo2_download_url") +  "wifi.csv");
        UtilFile.deleteFile(UploadConst.resourcePath("config_geo2_download_url") +  "ble.csv");
        
    }
    
    
    public static void removeAllGeo2File() throws SQLException{
        // 물리적 파일 지우기
        String sql = "select * from nmf_file where filetype = 10 ;";
               
        ArrayList<ResultMap> list = (ArrayList<ResultMap>)JdbcDao2.queryForMapList(sql, new Object[]{}); 
        
        for(ResultMap map : list){
            String fileName = UploadConst.resourcePath("config_geo2_download_url") +  map.getString("filename") ;
            UtilFile.deleteFile(fileName);
        }
        
        UtilFile.deleteFile(UploadConst.resourcePath("config_geo2_download_url") + "session.csv");
        UtilFile.deleteFile(UploadConst.resourcePath("config_geo2_download_url") + "payload.csv");
        UtilFile.deleteFile(UploadConst.resourcePath("config_geo2_download_url") + "lte.csv");
        UtilFile.deleteFile(UploadConst.resourcePath("config_geo2_download_url") +  "wifi.csv");
        UtilFile.deleteFile(UploadConst.resourcePath("config_geo2_download_url") +  "ble.csv");
        
    }
    
    public static void deleteAllNMFFile() throws SQLException {
        
        removeAllNMFFile();
        // DB 비우기
        JdbcDao2.execute("truncate table raw_sig;");
        JdbcDao2.execute("truncate table device_sig;");
        JdbcDao2.execute("truncate table collectxy;");
        JdbcDao2.execute("truncate table collectgrid;");
        JdbcDao2.execute("truncate table nmf_file;");
        
    }
    
    public static void deleteAllNMFFile(int type) throws SQLException {
        
        String sql = "select * from nmf_file where filetype=? ;";
        
        ArrayList<ResultMap> list = (ArrayList<ResultMap>)JdbcDao2.queryForMapList(sql, new Object[]{type}); 
        if(type == 0){
            for(ResultMap map : list){
                String fileName = UploadConst.resourcePath("config_cnmf_upload_url") +  map.getString("filename") ;
                UtilFile.deleteFile(fileName);
            }
            JdbcDao2.update("DELETE FROM nmf_file where filetype = ? ; " ,  new Object[]{FILE_TYPE_C_NMF});
            JdbcDao2.execute("truncate table raw_sig;");

        }else if(type == 10){
            removeAllGeo2File();
            JdbcDao2.execute("truncate table raw_sig;");
        }else{
            for(ResultMap map : list){
                String fileName = UploadConst.resourcePath("config_dnmf_upload_url") +  map.getString("filename") ;
                UtilFile.deleteFile(fileName);
            }
            JdbcDao2.update("DELETE FROM nmf_file where filetype = ? ; " ,  new Object[]{FILE_TYPE_D_NMF});
            JdbcDao2.execute("truncate table device_sig;");
        }
    }
    
    public static void deleteUserFiles(String userId) throws SQLException {
        
       if(!userId.equals("")) {
            JdbcDao2.update("DELETE FROM file "
                    + "where fileUser=(select userNo from user where userId = '" + userId +  "' ) ");
        }
    }
    
    public static void updatePermission(long fileId, int newPermissionValue) throws SQLException {
        JdbcDao2.update("UPDATE file SET fileShare=? WHERE fileId = ? ", new Object[]{newPermissionValue, fileId});
    }
    
    /**
     * 파일이 유저의 소유인지 확인
     * @param userId
     * @param fileId
     * @return  : 어드민이면 2 리턴
     */
    public static int checkUserFile(String userId, int fileId){
        
        if(UserDAO.checkAdmin(userId)){
            return 2;
        }
        
        String sql = "select count(*) "
                + "from file, (select userNo from user where userId =? ) t "
                + "where fileUser=t.userNo and fileId=? ";
        int count = 0;
          
        try {
            count = JdbcDao2.queryForInt(sql, new Object[]{userId, fileId}); 
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        if(count>0){
            return 1;
        }
        return 0;
    }
    
       
    public static int updateShare(int fileId, int newValue) {
        
        try{
              JdbcDao2.update("UPDATE file SET fileShare = fileShare + (" + newValue + " ) WHERE fileId = ? ", new Object[]{fileId});
            
        }catch(SQLException e){
            System.out.println("updateShare Error");
            return 0;
        }
        return 1;
    }
    
    
    
    private static String addOrderBy(String query, int orderOption){
        String queryStr = "";
        
        if(orderOption == FileDAO.FILESORT_DATE_DESC ){
            queryStr = query + " order by fileRegisterDate desc " ;
            
        } else if(orderOption == FileDAO.FILESORT_DATE_ASC ){
            queryStr = query + " order by fileRegisterDate asc " ;
            
        } else if(orderOption == FileDAO.FILESORT_NAME_DESC ){
            queryStr = query + " order by fileName desc " ;
            
        } else if(orderOption == FileDAO.FILESORT_NAME_ASC ){
            queryStr = query + " order by fileName asc " ;
            
        } else if(orderOption == FileDAO.FILESORT_USER_DESC ){
            queryStr = query + " order by fileUserName desc " ;
            
        } else if(orderOption == FileDAO.FILESORT_USER_ASC ){
            queryStr = query + " order by fileUserName asc " ;
            
        }else{
            queryStr = query;
        }
        
        return queryStr;
    }
}
