/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ithows.util;

/**
 *
 * @author mailt
 */

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    //압축파일 작성 : 하위 폴더 압축 지원
    //            String sourceFolder = "path/to/your/folder"; // JSON 파일이 있는 폴더 경로
    //            String outputZipFile = "output.zip"; // 생성될 ZIP 파일의 이름과 경로(FullPath)
    //  String extStr = ".json";
    public static boolean zipFiles(String sourceFolder, String outputZipFile, String extStr) throws IOException {

        try(
                FileOutputStream fos = new FileOutputStream(outputZipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            zipFile(Paths.get(sourceFolder), zos, Paths.get(sourceFolder), extStr);
        }
        return true;
    }

    public static boolean zipFile(String sourceFile, String outputZipFile, String password) throws IOException {

        boolean res = false;
        try {
            // 압축할 파일
            File file = new File(sourceFile);

            // ZIP 파일 생성
            ZipFile zipFile = null;
            ZipParameters parameters = null;

            if (password.equals("")) {
                zipFile = new ZipFile(outputZipFile);
                parameters = new ZipParameters();
                parameters.setCompressionLevel(CompressionLevel.NORMAL); // 압축 수준 설정
                parameters.setCompressionMethod(CompressionMethod.DEFLATE); // 압축 방법 설정


            } else {
                zipFile = new ZipFile(outputZipFile, password.toCharArray());
                parameters = new ZipParameters();
                parameters.setCompressionLevel(CompressionLevel.NORMAL);
                parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
                parameters.setEncryptFiles(true);
            }

            // 파일을 ZIP 파일에 추가
            if(zipFile != null && parameters!= null) {
                zipFile.addFile(file, parameters);
                res = true;
                System.out.println("파일이 성공적으로 압축되었습니다.");
            }else{
                System.out.println("파일 압축 실패");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    //  String extStr = ".json";
    private static void zipFile(Path folderToZip, ZipOutputStream zos, Path sourceFolderPath, String extStr) throws IOException {
        File[] files = folderToZip.toFile().listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                zipFile(file.toPath(), zos, sourceFolderPath, extStr);
            } else {
                if (!extStr.equals("")  && file.getName().endsWith(extStr)) {
                    Path filePath = file.toPath();
                    ZipEntry zipEntry = new ZipEntry(sourceFolderPath.relativize(filePath).toString());
                    zos.putNextEntry(zipEntry);

                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) >= 0) {
                            zos.write(buffer, 0, length);
                        }
                    }
                }else if(extStr.equals("") ){
                    Path filePath = file.toPath();
                    ZipEntry zipEntry = new ZipEntry(sourceFolderPath.relativize(filePath).toString());
                    zos.putNextEntry(zipEntry);

                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) >= 0) {
                            zos.write(buffer, 0, length);
                        }
                    }
                }
            }
        }
    }


    // 암호 압축
    //            String sourceFolder = "path/to/your/folder"; // JSON 파일이 있는 폴더 경로
    //            String outputZipFile = "output.zip"; // 생성될 ZIP 파일의 이름과 경로(FullPath)
    //  String extStr = ".json";
    public static boolean zipFilesWithPW(String sourceFolder, String outputZipFile,  String password, String extStr) {

        ZipFile zipFile = new ZipFile(outputZipFile, password.toCharArray());
        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionLevel(CompressionLevel.NORMAL);
        parameters.setEncryptFiles(true);
        parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

        try {
            addFolderToZip(zipFile, Paths.get(sourceFolder), parameters, Paths.get(sourceFolder), extStr);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    //            String zipFilePath = "output.zip"; // ZIP 파일의 이름과 경로(FullPath)
    public static boolean checkZipEncrypted(String zipFilePath)  {
        ZipFile zipFile = new ZipFile(zipFilePath);

        try {
            return zipFile.isEncrypted();
        } catch (ZipException e) {
            return false;
        }
    }

    // 하위 폴더까지 압축하기
    //  String extStr = ".json";
    private static void addFolderToZip(ZipFile zipFile, Path folderToZip, ZipParameters parameters, Path sourceFolderPath, String extStr) throws Exception {
        File[] files = folderToZip.toFile().listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                addFolderToZip(zipFile, file.toPath(), parameters, sourceFolderPath, extStr);
            } else {
                if (!extStr.equals("")  && file.getName().endsWith(extStr)) {
                    Path filePath = file.toPath();
                    zipFile.addFile(file, parameters);
                }else if(extStr.equals("") ){
                    Path filePath = file.toPath();
                    zipFile.addFile(file, parameters);

                }
            }
        }
    }

    // zip 파일을 암호zip 파일로 변경
    public static void encryptZipFile(String sourceZipFile, String outputZipFile, String password) throws Exception {
        ZipFile zipFile = new ZipFile(outputZipFile, password.toCharArray());
        ZipParameters zipParameters = new ZipParameters();

        // 암호화와 압축 방법 설정
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
        zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);

        // 파일 추가
        zipFile.addFile(new File(sourceZipFile), zipParameters);

    }

    public static void extractEncryptedZip(String sourceZipFile, String destinationFolder, String password) {
        try {
            ZipFile zipFile = new ZipFile(sourceZipFile);

            // 파일이 암호화되었는지 확인하고, 비밀번호를 설정합니다.
            if (zipFile.isEncrypted()) {
                zipFile.setPassword(password.toCharArray());
            }

            // 지정된 폴더로 파일들을 압축 해제합니다.
            zipFile.extractAll(destinationFolder);
            System.out.println("ZIP 파일이 성공적으로 압축 해제되었습니다.");
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

}
