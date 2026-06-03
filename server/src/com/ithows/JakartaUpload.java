package com.ithows;

import com.ithows.BaseDebug;
import java.util.*;
import java.io.*;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.SeekableStream;


public class JakartaUpload extends Hashtable {

    private long uploadSize;
    private String uploadEncode;
    private String uploadDir;

 
    public JakartaUpload(HttpServletRequest request, long uploadSize, String uploadDir, String uploadEncode, boolean checkImageValidation) {
        try {
            this.checkUploadDir(uploadDir);//디렉토리 체킹 및 백업
            this.uploadEncode = uploadEncode;
            this.uploadSize = uploadSize; // 최대 업로드 사이즈

            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            if (isMultipart) {                       
                DiskFileItemFactory factory = new DiskFileItemFactory();
                //factory.setSizeThreshold(uploadSize);
                factory.setRepository(new File(uploadDir));
                ServletFileUpload fileUpload = new ServletFileUpload(factory);
                fileUpload.setSizeMax(uploadSize);
                fileUpload.setHeaderEncoding(uploadEncode);

                List fileItemList = fileUpload.parseRequest(request);                
                //FormField Data Extraction
                for (int i = 0; i < fileItemList.size(); i++) {
                    FileItem fileItem = (FileItem) fileItemList.get(i);
                    if (fileItem.isFormField()) {
                        this.put(this.toEuckr(fileItem.getFieldName()), this.toEuckr(fileItem.getString()));//필드 정보 저장
                        //DreamCTDebug.info(this.toEuckr(fileItem.getFieldName()) + ":" + this.toEuckr(fileItem.getString()));                        
                    }                    
                }//for
                
                //File Extraction
                for (int i = 0; i < fileItemList.size(); i++) {
                    FileItem fileItem = (FileItem) fileItemList.get(i);
                    if (!fileItem.isFormField()) {
                        String fieldName = fileItem.getFieldName();
                        String fileName = "";
                        long fileSize = fileItem.getSize();//1. 파일 사이즈 얻기
                        if (fileSize < uploadSize) {
                            if (fileSize > 0) {
                                int idx = fileItem.getName().lastIndexOf("/");
                                if (idx == -1) {
                                    idx = fileItem.getName().lastIndexOf("\\");
                                }
                                fileName = fileItem.getName().substring(idx + 1);//2. 파일 이름얻기
                                fileName = getCheckedFileName(fileName); //3. 파일 이름 중복 체크
                                File uploadFile = new File(this.uploadDir + fileName);
                                fileItem.write(uploadFile); //4. 파일 기록하기
                                
                                FileInfo fileInfo = null;
                                //이미지인지 확인 하는 작업
                                //------------------------------------------------------------
                                if (checkImageValidation == true && isValidImage(fileName) == false) {
                                    fileItem.delete(); //잘못된 이미지는 삭제한다.
                                    fileInfo = new FileInfo(fileName, 0, fileItem); //파일사이즈가 0이면 잘못된 이미지로
                                } else {
                                    fileInfo = new FileInfo(fileName, fileSize, fileItem);
                                    
                                    //------------------------------------------------------------
                                    this.put(fieldName, fileInfo);//5. 정보 저장
                                }
                            }
                        }
                    }//for
                }
            }//생성자
        } catch (Exception e) {
            BaseDebug.log(e);
        }
    }

    public JakartaUpload(HttpServletRequest request, long uploadSize, String uploadDir, String uploadEncode) {
        try {
            
            this.checkUploadDir(uploadDir);//디렉토리 체킹 및 백업
            this.uploadEncode = uploadEncode;
            this.uploadSize = uploadSize; // 최대 업로드 사이즈

            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            if (isMultipart) {
                DiskFileItemFactory factory = new DiskFileItemFactory();
                //factory.setSizeThreshold(uploadSize);
                factory.setRepository(new File(uploadDir));
                ServletFileUpload fileUpload = new ServletFileUpload(factory);
                fileUpload.setSizeMax(uploadSize);
                fileUpload.setHeaderEncoding(uploadEncode);

                List fileItemList = fileUpload.parseRequest(request);
                //FormField Data Extraction
                for (int i = 0; i < fileItemList.size(); i++) {
                    FileItem fileItem = (FileItem) fileItemList.get(i);
                    if (fileItem.isFormField()) {
                        this.put(this.toEuckr(fileItem.getFieldName()), this.toEuckr(fileItem.getString()));//필드 정보 저장
                        //DreamCTDebug.info(this.toEuckr(fileItem.getFieldName()) + ":" + this.toEuckr(fileItem.getString()));
                    }
                }//for
                //File Extraction
                for (int i = 0; i < fileItemList.size(); i++) {
                    FileItem fileItem = (FileItem) fileItemList.get(i);
                    if (!fileItem.isFormField()) {
                        String fieldName = fileItem.getFieldName();
                                                
                        
                        String fileName = "";
                        long fileSize = fileItem.getSize();//1. 파일 사이즈 얻기
                        if (fileSize < uploadSize) {
                            if (fileSize > 0) {
                                int idx = fileItem.getName().lastIndexOf("/");
                                if (idx == -1) {
                                    idx = fileItem.getName().lastIndexOf("\\");
                                }
                                fileName = fileItem.getName().substring(idx + 1);//2. 파일 이름얻기
                                fileName = getCheckedFileName(fileName); //3. 파일 이름 중복 체크

                                File uploadFile = new File(this.uploadDir + fileName);
                                fileItem.write(uploadFile); //4. 파일 기록하기
                                this.put(fieldName, new FileInfo(fileName, fileSize, fileItem));//5. 정보 저장
                            }
                        }
                    }
                }//for
            }
        } catch (Exception e) {
            BaseDebug.log(e);
        }
    }//생성자

    protected boolean isValidImage(String fileName) {
        SeekableStream ss = null;
        File file = null;
        String[] ext = null;
        String[] images = {"gif", "jpe", "png", "tiff", "bmp"};
        boolean check1 = false;
        boolean check2 = false;
        try {
            file = new File(this.uploadDir + fileName);
            ss = new FileSeekableStream(file);
            ext = ImageCodec.getDecoderNames(ss);
            for (int i = 0; i < ext.length; i++) {//디코딩을 해서 확인
                for (int j = 0; j < images.length; j++) {
                    if (ext[i].indexOf(images[j]) > -1) {
                        check1 = true;
                        break;
                    }
                }
            }
            if (fileName.lastIndexOf(".") > -1) {//확장자 확인
                String extName = fileName.substring(fileName.lastIndexOf(".") + 1).trim();
                if (extName.equalsIgnoreCase("gif") || extName.equalsIgnoreCase("jpg")
                        || extName.equalsIgnoreCase("bmp") || extName.equalsIgnoreCase("png")) {
                    check2 = true;
                }
            }
        } catch (Exception e) {
            BaseDebug.log(e);
            check1 = false;
            check2 = false;
        }finally{
            try{
            ss.close();
            }catch(Exception e2){
                e2.printStackTrace();
            }
        }
        return check1 && check2;
    }

    public String getParameter(String value) {
        return (String) this.get(value);
    }

    public FileInfo getFileInfo(String value) {
        return (FileInfo) this.get(value);
    }
    /*처음 나오는 FileInfo를 리턴한다. */
    public FileInfo getScalarFileInfo(){
        Iterator iter = this.values().iterator();
        if(iter.hasNext()){
            Object obj = iter.next();
            if(obj instanceof FileInfo){
                return (FileInfo)obj;
            }
        }
        return null;
    }
    private void checkUploadDir(String uploadDir) throws IOException {
        if (!uploadDir.endsWith(File.separator)) {
            this.uploadDir = uploadDir + File.separator;
        } else {
            this.uploadDir = uploadDir;
        }
        File f = new File(this.uploadDir);
        if (!f.exists()) {
            f.mkdirs();
            //DreamCTDebug.info(f.getAbsolutePath() + "를 만들었습니다.");
        }
    }
    //파일이 존재하는지 확인해서 존재하지 않는다면 Absolute Path를 리턴하고
    // 존재하면 파일을 지우고 절대 경로를 만들어 리턴
    // @@ 
    
    private String getCheckedFileName(String filename) {
        int count = 1;
        String absPath = this.uploadDir + filename;
        while (new File(absPath).exists()) {
//            filename = getOtherFileName(filename, count++); //존재한다면 새로운 이름을 생성한 후 Absolute Path로 만들어서 리턴한다.
//            absPath = this.uploadDir + filename;
              new File(absPath).delete();   // 파일을 삭제 해 버림
        }
        return filename;
    }
    //새로운 파일 이름을 생성한다.

    private String getOtherFileName(String filename, int count) {
        int pos = 0;
        String temp;
        if ((pos = filename.lastIndexOf(".")) != -1) {
            temp = filename.substring(0, pos);
            temp = temp.replaceAll("_" + (count - 1), "");
            return temp + "_" + count + filename.substring(pos);
        } else {
            temp = filename.replaceAll("_" + (count - 1), "");
            return temp + "_" + count;
        }
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
