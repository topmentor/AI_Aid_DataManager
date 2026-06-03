/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

import com.ithows.AppConfig;
import com.ithows.service.UploadConst;
import java.io.File;

/**
 *
 * @author dreamct
 */
public class DirNaming {

    public static String getBoardImageUploadDirectory(String dirName) {
        String uploadDir = UploadConst.resourcePath("config_image_upload_url") +  dirName;
        File dir = new File(uploadDir);               //directory의 존재 여부를 확인하고 없을시 생성
        if (dir.exists() == false) {
            dir.mkdirs();
        }
        return uploadDir;
    }
    public static String getBoardFileUploadDirectory(String dirName) {
        String uploadDir = UploadConst.resourcePath("config_file_upload_url") +  dirName;
        File dir = new File(uploadDir);               //directory의 존재 여부를 확인하고 없을시 생성
        if (dir.exists() == false) {
            dir.mkdirs();
        }
        return uploadDir;
    }
}
