/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ithows.util;

/**
 *
 * @author mailt
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class RequestUtils {
    
    public enum RequestMethod {
        GET("GET"),
        POST("POST"),
        PUT("PUT"),
        DELETE("DELETE");
    
        private final String method;
        
        RequestMethod(String method) {
            this.method = method;   
        }
        
        public String method() {
            return method;
        }
    }
    
    /**
     * HTTP Reuqest 전송
     * @param method            Request Method
     * @param url               URL
     * @param queryParams       Request Query String Parameter
     * @return                  HTTP 응답 결과
     * @throws IOException 
     */
    public static String sendRequest(RequestMethod method, String url, Map<String, ?> queryParams) throws IOException {
        return sendRequest(method, url, queryParams, null);
    }

    
    /**
     * HTTP Reuqest 전송
     * @param method            Request Method
     * @param url               URL
     * @param queryParams       Request Query String Parameter
     * @param body              Request Body
     * @return                  HTTP 응답 결과
     * @throws IOException 
     */
    public static String sendRequest(RequestMethod method, String url, Map<String, ?> queryParams, String body) throws IOException {
        // append Query String
        if (queryParams != null && !queryParams.isEmpty()) {
            url = String.format("%s?%s", url, mapToQueryString(queryParams));
        }
        
        URL requestUrl = new URL(url);
        
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod(method.method());
        connection.setRequestProperty("charset", "utf-8");
        
        if (RequestMethod.GET.equals(method) || RequestMethod.DELETE.equals(method)) {
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        } else if (RequestMethod.POST.equals(method) || RequestMethod.PUT.equals(method)) {
            connection.setRequestProperty("Content-Type", "application/json");
        }
        
        // append Body
        if (body != null) {
            connection.setDoOutput(true);
            OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            osw.write(body);
            osw.flush();
            osw.close();
        }
        
        BufferedReader br;
        int status = connection.getResponseCode();
        
        if (status == HttpURLConnection.HTTP_OK) {
            br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        } else {
            br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
        }
        
        StringBuilder sb = new StringBuilder();
        String str;
        while ((str = br.readLine()) != null) {
            sb.append(str);
        }
        
        return sb.toString();
    }
    
    
    /**
     * HTTP Request 전송 및 파일 다운로드
     * @param url                       URL
     * @param queryParams               Request Query String Parameter
     * @param dest                      파일 저장 경로
     * @return                          저장 여부
     * @throws IOException 
     */
    public static boolean sendRequestAndDownloadFile(String url, Map<String, ?> queryParams, String dest) throws IOException {
        // append Query String
        if (queryParams != null && !queryParams.isEmpty()) {
            url = String.format("%s?%s", url, mapToQueryString(queryParams));
        }
        
        URL requestUrl = new URL(url);
        
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");

        int status = connection.getResponseCode();
        
        if (status == HttpURLConnection.HTTP_OK) {
            FileOutputStream fos = new FileOutputStream(new File(dest));
            InputStream is = connection.getInputStream();
            
            byte[] buffer = new byte[2048];
            int length;
            while ((length = is.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            
            fos.close();
            is.close();
            
            return new File(dest).exists();
        }
        
        return false;
    }
    
    
    /**
     * HTTP Request 전송 및 파일 다운로드
     * @param method                    Request Method
     * @param url                       URL
     * @param queryParams               Request Query String Parameter
     * @param body                      Request Body
     * @param dest                      파일 저장 경로
     * @return
     * @throws IOException 
     */
    public static boolean sendRequestAndDownloadFile(RequestMethod method, String url, Map<String, ?> queryParams, String body, String dest) throws IOException {
        // append Query String
        if (queryParams != null && !queryParams.isEmpty()) {
            url = String.format("%s?%s", url, mapToQueryString(queryParams));
        }
        
        URL requestUrl = new URL(url);
        
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");

        // append Body
        if (body != null) {
            connection.setDoOutput(true);
            OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            osw.write(body);
            osw.flush();
            osw.close();
        }
        
        int status = connection.getResponseCode();
        
        if (status == HttpURLConnection.HTTP_OK) {
            FileOutputStream fos = new FileOutputStream(new File(dest));
            InputStream is = connection.getInputStream();
            
            byte[] buffer = new byte[2048];
            int length;
            while ((length = is.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            
            fos.close();
            is.close();
            
            return new File(dest).exists();
        }
        
        return false;
    }
    
    
    /**
     * Query String Parameter 형식으로 변경
     * @param queryParams       Query String
     * @return                  
     */
    private static String mapToQueryString(Map<String, ?> queryParams) {
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry map : queryParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
         
            sb.append(String.format("%s=%s", urlEncode(map.getKey()), urlEncode(map.getValue())));
        }
        
        return sb.toString();
    }
    
    
    /**
     * URL 인코딩
     * @param value     인코딩 할 문자
     * @return 
     */
    private static String urlEncode(Object value) {
        try {
            return URLEncoder.encode(String.valueOf(value), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return String.valueOf(value);
        }
    }
}


/**
 *          boolean isSucceed = false;                                          // 파일 다운로드 여부
            
            String number = edgeMap.getString("no");
            String ip = edgeMap.getString("ip");
            String url = String.format("http://%s%s", ip, DOWNLOAD_TODAY_SIGNAL_URI);
            
            // 파일 이름 : backup_{no}_20230609080000_20230609085959_csv.zip
            String fileName = String.format("backup_%s_%s_csv.zip", number, timeRange);
            String filePath = String.format("%s\\%s", path, fileName);
            
            try {
                isSucceed = RequestUtils.sendRequestAndDownloadFile(RequestUtils.RequestMethod.POST, url, null, params, filePath);
            } catch (IOException e) {
                String message = LogUtils.exceptionToString(e);
                
                LogUtils.write(message);
            } finally {
                String message = String.format("Edge %s download is %s \n", number, isSucceed);
                
                LogUtils.write(message);
                
                if (isSucceed) {
                    fileList.add(filePath);
                }
            }
 * 
 * 
 */
