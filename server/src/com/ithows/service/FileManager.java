/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.service;

import com.ithows.FileInfo;
import com.ithows.HttpUtil;
import com.ithows.JakartaUpload;
import com.ithows.ResultMap;
import com.ithows.SessionInfo;
import com.ithows.dao.FileDAO;
import com.ithows.dao.UserDAO;
import com.ithows.util.DateTimeUtils;
import com.ithows.util.UtilFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

/**
 *
 * @author ksyuser
 */
public class FileManager {

    
    /**
     * NMF파일 업로드 하기.
     */
    public static JSONObject saveNmfFile(HttpSession session, HttpServletRequest request, int nmftype) {

        String mapDir = UploadConst.getNmfFileUploadDir(session, nmftype); // 업로드할 디렉터리 (URL 경로)

        JSONObject fileInfoMap = null;

        try {
            // 1. 업로드 객체 생성
            JakartaUpload mRequest = new JakartaUpload(request, 1000 * 1024 * 1024, mapDir, "UTF-8");

//        System.out.println("mRequest ------------- " + mRequest);
            // 2. 파일info에서 업로드 파일명을 받아 옴. 업로드 페이지의 <input type="file" name="qs_file" ...>와 name을 맞추어 주어야 한다.
//        FileInfo fInfo = mRequest.getFileInfo("uploadfile");
            FileInfo fInfo = mRequest.getFileInfo("file");
            String saveFileName = fInfo.getFileName();
            String mapGuid = FilenameUtils.getBaseName(saveFileName);

            if (saveFileName == null || saveFileName.equals("")) {
                saveFileName = "default";
            }

            if (saveFileName != null) {

                // 서버상 물리적 저장 경로를 가져옴
                String targetPath = mapDir + mapGuid;

//            System.out.println("filename = " + mapDir + saveFileName);
                saveFileName = mapDir + saveFileName;
                File newfile = new File(saveFileName);
                if (newfile.exists()) {
//                System.out.println("파일 존재  : "   + saveFileName);

                    // 디렉토리 확인 후 있으면 제거
                    File oldMapPath = new File(mapDir + mapGuid);
                    if (oldMapPath.exists()) {
                        UtilFile.deleteDir(oldMapPath);
//                    System.out.println("기존 맵 삭제  : " + mapGuid);
                    }

                    fileInfoMap = new JSONObject();
                    fileInfoMap.put("saveFileName", saveFileName);

                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        return fileInfoMap;
    }
    
    
    public static JSONObject saveNmfZipFile(HttpSession session, HttpServletRequest request, int nmftype) {

        String mapDir = UploadConst.getNmfZipFileUploadDir(session, nmftype); // 업로드할 디렉터리 (URL 경로)

        JSONObject fileInfoMap = null;

        try {
            // 1. 업로드 객체 생성
            JakartaUpload mRequest = new JakartaUpload(request, 1000 * 1024 * 1024, mapDir, "UTF-8");

//        System.out.println("mRequest ------------- " + mRequest);
            // 2. 파일info에서 업로드 파일명을 받아 옴. 업로드 페이지의 <input type="file" name="qs_file" ...>와 name을 맞추어 주어야 한다.
//        FileInfo fInfo = mRequest.getFileInfo("uploadfile");
            FileInfo fInfo = mRequest.getFileInfo("file");
            String saveFileName = fInfo.getFileName();
            String mapGuid = FilenameUtils.getBaseName(saveFileName);

            if (saveFileName == null || saveFileName.equals("")) {
                saveFileName = "default";
            }

            if (saveFileName != null) {

                // 서버상 물리적 저장 경로를 가져옴
                String targetPath = mapDir + mapGuid;

//            System.out.println("filename = " + mapDir + saveFileName);
                saveFileName = mapDir + saveFileName;
                File newfile = new File(saveFileName);
                if (newfile.exists()) {
                    fileInfoMap = new JSONObject();
                    fileInfoMap.put("saveFileName", saveFileName);
                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        return fileInfoMap;
    }
    
    public static String saveApplogZipFile(HttpSession session, HttpServletRequest request) {

        String jsonZipDir = UploadConst.getTempUploadDir(); // 업로드할 디렉터리 (URL 경로)

        JSONObject fileInfoMap = null;
        String targetPath = "";

        try {
            // 1. 업로드 객체 생성
            JakartaUpload mRequest = new JakartaUpload(request, 1000 * 1024 * 1024, jsonZipDir, "UTF-8");

//        System.out.println("mRequest ------------- " + mRequest);
            // 2. 파일info에서 업로드 파일명을 받아 옴. 업로드 페이지의 <input type="file" name="qs_file" ...>와 name을 맞추어 주어야 한다.
//        FileInfo fInfo = mRequest.getFileInfo("uploadfile");
            FileInfo fInfo = mRequest.getFileInfo("file");
            String saveFileName = fInfo.getFileName();
            String upzipName = FilenameUtils.getBaseName(saveFileName);

            if (saveFileName == null || saveFileName.equals("")) {
                return targetPath;
                
            }else{
                // 서버상 물리적 저장 경로를 가져옴
                targetPath = jsonZipDir + upzipName + "/";

//            System.out.println("filename = " + mapDir + saveFileName);
                saveFileName = jsonZipDir + saveFileName;
                File newfile = new File(saveFileName);
                if (newfile.exists()) {
                    UtilFile.unzip(saveFileName, targetPath, false);
                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        return targetPath;
    }
    
    
    public static String saveJsonZipFile(HttpSession session, HttpServletRequest request) {

        String jsonZipDir = UploadConst.getTempUploadDir(); // 업로드할 디렉터리 (URL 경로)

        JSONObject fileInfoMap = null;
        String targetPath = "";

        try {
            // 1. 업로드 객체 생성
            JakartaUpload mRequest = new JakartaUpload(request, 1000 * 1024 * 1024, jsonZipDir, "UTF-8");

//        System.out.println("mRequest ------------- " + mRequest);
            // 2. 파일info에서 업로드 파일명을 받아 옴. 업로드 페이지의 <input type="file" name="qs_file" ...>와 name을 맞추어 주어야 한다.
//        FileInfo fInfo = mRequest.getFileInfo("uploadfile");
            FileInfo fInfo = mRequest.getFileInfo("file");
            String saveFileName = fInfo.getFileName();
            String upzipName = FilenameUtils.getBaseName(saveFileName);

            if (saveFileName == null || saveFileName.equals("")) {
                return targetPath;
                
            }else{
                // 서버상 물리적 저장 경로를 가져옴
                targetPath = jsonZipDir + upzipName + "/";

//            System.out.println("filename = " + mapDir + saveFileName);
                saveFileName = jsonZipDir + saveFileName;
                File newfile = new File(saveFileName);
                if (newfile.exists()) {
                    UtilFile.unzip(saveFileName, targetPath, false);
                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        return targetPath;
    }
    
    public static JSONObject saveJsonOneFile(HttpSession session, HttpServletRequest request) {

        String timeGuid = DateTimeUtils.getTimeDateNow2() ;
        String jsonOneDir = UploadConst.getTempUploadDir() + "/"+ timeGuid; // 업로드할 디렉터리 (URL 경로)


        JSONObject fileInfoMap = null;

        try {
            // 1. 업로드 객체 생성
            JakartaUpload mRequest = new JakartaUpload(request, 1000 * 1024 * 1024, jsonOneDir , "UTF-8");

//        System.out.println("mRequest ------------- " + mRequest);
            // 2. 파일info에서 업로드 파일명을 받아 옴. 업로드 페이지의 <input type="file" name="qs_file" ...>와 name을 맞추어 주어야 한다.
//        FileInfo fInfo = mRequest.getFileInfo("uploadfile");
            FileInfo fInfo = mRequest.getFileInfo("file");
            String saveFileName = fInfo.getFileName();
            String saveBaseFileName = saveFileName;

            if (saveFileName == null || saveFileName.equals("")) {
                saveFileName = "default";
            }

            if (saveFileName != null) {

                // 서버상 물리적 저장 경로를 가져옴

                saveFileName = jsonOneDir + "/" + saveFileName;
//             System.out.println("filename = " + mapDir + saveFileName);

                File newfile = new File(saveFileName);
                if (newfile.exists()) {
                    fileInfoMap = new JSONObject();
                    fileInfoMap.put("saveFileName", saveFileName);
                    fileInfoMap.put("saveBaseFileName", saveBaseFileName);
                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        return fileInfoMap;
    }
    
    public static JSONObject saveCollectDBFile(HttpSession session, HttpServletRequest request) {

        String timeGuid = DateTimeUtils.getTimeDateNow2() ;
        String jsonOneDir = UploadConst.getTempUploadDir() ; // 업로드할 디렉터리 (URL 경로)


        JSONObject fileInfoMap = null;

        try {
            // 1. 업로드 객체 생성
            JakartaUpload mRequest = new JakartaUpload(request, 1000 * 1024 * 1024, jsonOneDir , "UTF-8");

//        System.out.println("mRequest ------------- " + mRequest);
            // 2. 파일info에서 업로드 파일명을 받아 옴. 업로드 페이지의 <input type="file" name="qs_file" ...>와 name을 맞추어 주어야 한다.
//        FileInfo fInfo = mRequest.getFileInfo("uploadfile");
            FileInfo fInfo = mRequest.getFileInfo("file");
            String saveFileName = fInfo.getFileName();
            String saveBaseFileName = saveFileName;

            if (saveFileName == null || saveFileName.equals("")) {
                saveFileName = "default";
            }

            if (saveFileName != null) {

                // 서버상 물리적 저장 경로를 가져옴

                saveFileName = jsonOneDir + "/" + saveFileName;
//             System.out.println("filename = " + mapDir + saveFileName);

                File newfile = new File(saveFileName);
                if (newfile.exists()) {
                    fileInfoMap = new JSONObject();
                    fileInfoMap.put("saveFileName", saveFileName);
                    fileInfoMap.put("saveBaseFileName", saveBaseFileName);
                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        return fileInfoMap;
    }
    
    
    
    
    public static JSONObject saveCollectgridFile(HttpSession session, HttpServletRequest request) {

        String tempDir = UploadConst.getTempUploadDir() ; // 업로드할 디렉터리 (URL 경로)


        JSONObject fileInfoMap = null;

        try {
            // 1. 업로드 객체 생성
            JakartaUpload mRequest = new JakartaUpload(request, 1000 * 1024 * 1024, tempDir , "UTF-8");

//        System.out.println("mRequest ------------- " + mRequest);
            // 2. 파일info에서 업로드 파일명을 받아 옴. 업로드 페이지의 <input type="file" name="qs_file" ...>와 name을 맞추어 주어야 한다.
//        FileInfo fInfo = mRequest.getFileInfo("uploadfile");
            FileInfo fInfo = mRequest.getFileInfo("file");
            String saveFileName = fInfo.getFileName();
            String saveBaseFileName = saveFileName;

            if (saveFileName == null || saveFileName.equals("")) {
                saveFileName = "default";
            }

            if (saveFileName != null) {

                // 서버상 물리적 저장 경로를 가져옴

                saveFileName = tempDir + "/" + saveFileName;

                File newfile = new File(saveFileName);
                if (newfile.exists()) {
                    fileInfoMap = new JSONObject();
                    fileInfoMap.put("saveFileName", saveFileName);
                    fileInfoMap.put("saveBaseFileName", saveBaseFileName);
                    fileInfoMap.put("saveUnzipFileName", tempDir + "/" + FilenameUtils.getBaseName(saveBaseFileName) + ".sql" );
                    
                    
//                    System.out.println("saveFileName = " + saveFileName);
                    
                    UtilFile.unzip(saveFileName, tempDir, true);
                    
//                    System.out.println("saveUnzipFileName = " + tempDir + "/" + FilenameUtils.getBaseName(saveBaseFileName) + ".sql");
                    
                    
                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        return fileInfoMap;
    }
    
    
    
    public static JSONObject saveGeo2ZipFile(HttpSession session, HttpServletRequest request) {

        String geo2Dir = UploadConst.getGeo2FileUploadDir() ; // 업로드할 디렉터리 (URL 경로)


        JSONObject fileInfoMap = null;

        try {
            // 1. 업로드 객체 생성
            JakartaUpload mRequest = new JakartaUpload(request, 1000 * 1024 * 1024, geo2Dir , "UTF-8");


            FileInfo fInfo = mRequest.getFileInfo("file");
            String saveFileName = fInfo.getFileName();
            String saveBaseFileName = saveFileName;


            if (saveFileName != null) {

                // 서버상 물리적 저장 경로를 가져옴
                saveFileName = geo2Dir + saveFileName;

                System.out.println("filename = " + saveFileName);

                if (UtilFile.checkExist(saveFileName) ) {
                    fileInfoMap = new JSONObject();
                    fileInfoMap.put("saveFileName", saveFileName);
                    fileInfoMap.put("saveBaseFileName", saveBaseFileName);
                    fileInfoMap.put("saveDir", geo2Dir);
                    
                }

            }
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        return fileInfoMap;
    }
}
