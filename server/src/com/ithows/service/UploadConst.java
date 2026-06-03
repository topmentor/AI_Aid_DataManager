/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.service;

import com.ithows.AppConfig;
import java.io.File;
import javax.servlet.http.HttpSession;

/**
 *
 * @author ksyuser
 */
public class UploadConst {
    
    
    public static String contextPath(){
        String tempPath = "";
        String OS = System.getProperty("os.name").toLowerCase();
        if(OS.indexOf("win") >= 0){ 
           tempPath = AppConfig.getConf("context_win_dir") ;

        }else{
           tempPath = AppConfig.getConf("context_dir") ;
        } 
        return tempPath;
    }
    
    public static String resourcePath(String key){
        String path = "";
        path = contextPath() + AppConfig.getConf(key);
        return path;
    }
    
    public static String getSessionDir(HttpSession session) {
        
        String sessionDir = session.getServletContext().getRealPath("/");
        return sessionDir;
    }
    
    // 프리젠테이션(맵) 업로드 경로
    // web 폴더가 root 경로임
    public static String getDashboardUploadDir(String mapGuid) {
        return AppConfig.getConf("config_dashboard_upload_url") ;
    }

    // 프리젠테이션(맵)이 저장되어 있는 모든 경로
    // 유저를 구분하지 않는다
    public static String getDashboardUploadDir(HttpSession session) {
        //여기서의 session은 현재의 디렉터리만 얻어낸다. getServletContext()
        String mapPath = getDir(session, AppConfig.getConf("config_dashboard_upload_url"),  "");
        return mapPath;
    }
    
    // Geo2 파일 저장 경로
    public static String getGeo2FileUploadDir(HttpSession session) {
        
        String mapPath = null;
        mapPath = getDir(session, AppConfig.getConf("config_geo2_download_url"),  "");

        return mapPath;
    }
    
    public static String getGeo2FileUploadDir() {
        
        String pathName = "";
        String OS = System.getProperty("os.name").toLowerCase();
        if(OS.indexOf("win") >= 0){ 
           pathName = AppConfig.getConf("context_win_dir") + AppConfig.getConf("config_geo2_download_url") ;

        }else{
           pathName = AppConfig.getConf("context_dir") + AppConfig.getConf("config_geo2_download_url") ;
        } 
        
        return pathName  ;
        
    }
    
    // 결과 로그 파일 저장 경로
    public static String getResultLogFileDir(HttpSession session) {
        
        String filePath = null;
        filePath = getDir(session, AppConfig.getConf("config_resultlog_url"), ""); 

        return filePath;
    }
    
    // 결과 로그 파일 저장 경로
    public static String getResultLogFileDir() {  
        
        String filePath = null;
        filePath = contextPath() + AppConfig.getConf("config_resultlog_url"); 

        return filePath;
    }
    
    // NMF 업로드 경로
    // web 폴더가 root 경로임
    public static String getNmfFileUploadDir(String mapGuid, int nmftype) {
        
        if(nmftype == 0){
            return AppConfig.getConf("config_cnmf_upload_url") ;
        }else{
            return AppConfig.getConf("config_dnmf_upload_url") ;
        }
    }
    
    
    // NMF이 저장되어 있는 모든 경로
    // 유저를 구분하지 않는다
    public static String getNmfFileUploadDir(HttpSession session, int nmftype) {
        //여기서의 session은 현재의 디렉터리만 얻어낸다. getServletContext()
        String mapPath = null;
        if(nmftype == 0){
            mapPath = getDir(session, AppConfig.getConf("config_cnmf_upload_url"),  "");
        }else{
            mapPath = getDir(session, AppConfig.getConf("config_dnmf_upload_url"),  "");
        }

        return mapPath;
    }
    
    public static String getNmfZipFileUploadDir(HttpSession session, int nmftype) {
        //여기서의 session은 현재의 디렉터리만 얻어낸다. getServletContext()
        String mapPath = null;
        if(nmftype == 0){
            mapPath = getDir(session, AppConfig.getConf("config_cnmf_upload_url") + "zip/",  "");
        }else{
            mapPath = getDir(session, AppConfig.getConf("config_dnmf_upload_url") + "zip/",  "");
        }

        return mapPath;
    }
    
    
    
    
    // 업로드 템프 경로
    // ** web 폴더가 root 경로임
    public static String getTempUploadDir() {
        
        
        String pathName = "";
        String OS = System.getProperty("os.name").toLowerCase();
        if(OS.indexOf("win") >= 0){ 
           pathName = AppConfig.getConf("context_win_dir") + AppConfig.getConf("temp_dir") ;

        }else{
           pathName = AppConfig.getConf("context_dir") + AppConfig.getConf("temp_dir") ;
        } 
        
        return pathName  ;
    }
    
    public static String getTempUploadDir(String userId) {
        
        return AppConfig.getConf("temp_dir") + userId  ;
    }

    // 업로드 된 임시 파일이 저장되어 있는 모든 경로
    public static String getTempUploadDir(HttpSession session, String userId) {
        //여기서의 session은 현재의 디렉터리만 얻어낸다. getServletContext()
        String imagePath = getDir(session, AppConfig.getConf("temp_dir"),  userId);
        return imagePath;
    }
       
    // 업로드 이미지 경로
    // ** web 폴더가 root 경로임
    public static String getImageUploadDir(String userId) {
        return AppConfig.getConf("config_image_upload_url") + userId  ;
    }

    // 업로드 이미지가 저장되어 있는 모든 경로
    public static String getImageUploadDir(HttpSession session, String userId) {
        //여기서의 session은 현재의 디렉터리만 얻어낸다. getServletContext()
        String imagePath = getDir(session, AppConfig.getConf("config_image_upload_url"),  userId);
        return imagePath;
    }
    
    // 업로드 파일 경로
    // ** web 폴더가 root 경로임
    public static String getFileUploadDir(String userId) {
        return AppConfig.getConf("config_file_upload_url") + userId  ;
    }

    // 업로드 파일이 저장되어 있는 모든 경로
    public static String getFileUploadDir(HttpSession session, String userId) {
        //여기서의 session은 현재의 디렉터리만 얻어낸다. getServletContext()
        String imagePath = getDir(session, AppConfig.getConf("config_file_upload_url"),  userId);
        return imagePath;
    }
    
    
    // 업로드 Map 경로
    // ** web 폴더가 root 경로임
    public static String getGISMapUploadDir(String userId) {
        return AppConfig.getConf("config_gismap_upload_url") + userId  ;
    }

    // 업로드 Map이 저장되어 있는 모든 경로
    public static String getGISMapUploadDir(HttpSession session, String userId) {
        //여기서의 session은 현재의 디렉터리만 얻어낸다. getServletContext()
        String imagePath = getDir(session, AppConfig.getConf("config_gismap_upload_url"),  userId);
        return imagePath;
    }
    
    
    // 업로드 Document 경로
    // ** web 폴더가 root 경로임  config_document_upload_url
    public static String getDocumentUploadDir(String userId) {
        return AppConfig.getConf("config_document_upload_url") + userId  ;
    }

    // 업로드 Map이 저장되어 있는 모든 경로
    public static String getDocumentUploadDir(HttpSession session, String userId) {
        //여기서의 session은 현재의 디렉터리만 얻어낸다. getServletContext()
        String imagePath = getDir(session,  AppConfig.getConf("config_document_upload_url"),  userId);
        return imagePath;
    }
    
    
    
    
    public static String getDir(HttpSession session, String subDir, String childDir) {

        String servletPath = session.getServletContext().getRealPath("/");

        //Tomcat 8.0에서는 마지막 경로에 '/'를 붙여주지 않기 때문에 이런 처리를 해야 함.        
//        if(!servletPath.endsWith("\\")){
//            servletPath += "\\";
//        }
//        if(!servletPath.endsWith("/")){    // @@ 리눅스 경로 문제
//            servletPath += "/";
//        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(servletPath);
        sb.append(subDir);
        if (childDir != null && childDir.equals("") == false ) {
            sb.append(childDir);
//            sb.append("\\");
            sb.append("/");
        }
        
        File file = new File(sb.toString());
        if (!file.exists()) {
            file.mkdirs();
        }
        
        return sb.toString();
    }
}
