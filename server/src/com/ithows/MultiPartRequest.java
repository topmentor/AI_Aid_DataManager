/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * 멀티파트, Form request 데이터를 읽어내어서 처리 할 수 있는 클래스
 * @author ksyuser
 */
public class MultiPartRequest extends Hashtable {

 

    public MultiPartRequest(HttpServletRequest request) {
        ServletFileUpload fileUpload = null;
        try {

            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            if (isMultipart) {
                DiskFileItemFactory factory = new DiskFileItemFactory();
                //factory.setSizeThreshold(uploadSize);
//                factory.setRepository(new File(uploadDir));
                  fileUpload = new ServletFileUpload(factory);
//                fileUpload.setSizeMax(uploadSize);
//                fileUpload.setHeaderEncoding(uploadEncode);

                List fileItemList = fileUpload.parseRequest(request);
                //FormField Data Extraction
                for (int i = 0; i < fileItemList.size(); i++) {
                    FileItem fileItem = (FileItem) fileItemList.get(i);
                    if (fileItem.isFormField()) {
                        this.put(this.toEuckr(fileItem.getFieldName()), this.toEuckr(fileItem.getString()));//필드 정보 저장
                    }
                }//for
                
                
                //File Extraction
                for (int i = 0; i < fileItemList.size(); i++) {
                    FileItem fileItem = (FileItem) fileItemList.get(i);
                    if (!fileItem.isFormField()) {
                        String fieldName = fileItem.getFieldName();
                        String fileName = "";
                        long fileSize = fileItem.getSize();//1. 파일 사이즈 얻기
                        if (fileSize > 0) {
                            int idx = fileItem.getName().lastIndexOf("/");
                            if (idx == -1) {
                                idx = fileItem.getName().lastIndexOf("\\");
                            }
                            fileName = fileItem.getName().substring(idx + 1);//2. 파일 이름얻기

                            this.put(fieldName, new FileInfo(fileName, fileSize, fileItem));//5. 정보 저장
                        }
                        
                    }
                }//for
            }
        } catch (Exception e) {
            BaseDebug.log(e);
        }
        
    }//생성자


    public String getParameter(String value) {
        return (String) this.get(value);
    }

    public FileInfo getFileInfo(String value) {
        return (FileInfo) this.get(value);
    }

    //한글 인코딩
    private String toEuckr(String str) {
        if (str == null) {
            return null;
        }
        String result = null;
        try {
            result = new String(str.getBytes("8859_1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            BaseDebug.log(e);
        }
        return result;
    }

    
}
