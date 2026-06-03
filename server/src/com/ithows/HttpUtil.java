/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows;


import com.ithows.util.UtilString;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

/**
 *
 * @author dreamct
 */
public class HttpUtil {

    public static SessionInfo getSessionInfo(HttpSession session) {
        SessionInfo sInfo = null;
        Object obj = session.getAttribute("sessionInfo");
        if (obj != null) {  //로그인 정보있을 경우 세션의 회원 정보를 저장한다
            sInfo = (SessionInfo) obj;
        } else {  //세션의 정보가 없을경우 새로운 세션정보를 만든다
            sInfo = new SessionInfo();
            session.setAttribute("sessionInfo", sInfo);
        }
        return (SessionInfo) sInfo;
    }

    public static long getParameterLong(HttpServletRequest request, String param) {
        return getParameterLong(request, param, 0);
    }

    public static long getParameterLong(HttpServletRequest request, String param, int defaultValue) {
        long result = defaultValue;
        String _no = request.getParameter(param);
        if (_no != null && !_no.isEmpty()) {
            try {
                result = Long.parseLong(_no);
            } catch (Exception e) {
                result = defaultValue;
                BaseDebug.log(e);
            }
        }
        return result;
    }

    public static int getParameterInt(HttpServletRequest request, String param) {
        return getParameterInt(request, param, 0);
    }

    public static int getParameterInt(HttpServletRequest request, String param, int defaultValue) {
        int result = defaultValue;
        String _no = request.getParameter(param);
        if (_no != null && !_no.isEmpty()) {
            try {
                result = Integer.parseInt(_no);
            } catch (Exception e) {
                result = defaultValue;
                BaseDebug.log(e);
            }
        }
        return result;
    }

    public static double getParameterDouble(HttpServletRequest request, String param) {
        return getParameterDouble(request, param);
    }
    public static double getParameterDouble(HttpServletRequest request, String param, double defaultValue) {
        double result = defaultValue;
        String _no = request.getParameter(param);
        if (_no != null && !_no.isEmpty()) {
            try {
                result = Double.parseDouble(_no);
            } catch (Exception e) {
                result = defaultValue;
                BaseDebug.log(e);
            }
        }
        return result;
    }

    public static String getParameterString(HttpServletRequest request, String param, String defaultValue) {
        String result = defaultValue;
        String data = request.getParameter(param);
        if (data == null) {
            result = defaultValue;
        } else {
            result = data;
        }
        return result;
    }

    public static boolean isParamEmpty(String param) {
        if (param != null && !param.equals("undefined") && !param.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean isParamValid(String param) {
        return !isParamEmpty(param);
    }

    public static boolean getParameterBoolean(HttpServletRequest request, String param) {
        boolean result = false;
        String _pa = request.getParameter(param);

        if (_pa != null && !_pa.isEmpty()) {
            try {
                result = Boolean.parseBoolean(_pa);
            } catch (Exception e) {
                BaseDebug.log(e);
            }
        }
        return result;
    }

    public static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        Cookie returnCookie = null;
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(name)) {
                    returnCookie = cookies[i];
                    break;
                }
            }
        }
        return returnCookie;
    }

   
    /**
     * HttpUtils.getBody(request)로 보내려면 클라이언트에서 
     * data: JSON.stringify(form_data),
     * contentType: "application/json",
     * 형식으로 보내야 한다.
     */
    // 'application/json' request에 대해 파라미터를 받기 위해 반드시 필요한 함수
    public static String getBody(HttpServletRequest request) throws IOException {

        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                
                char[] charBuffer = new char[2048];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        body = stringBuilder.toString();
        
        return body;
    }
    
    /**
     * HttpUtils.getBody(request)로 보내려면 클라이언트에서 
     * data: JSON.stringify(form_data),
     * contentType: "application/json",
     */
    // 'application/json' request에 대해 파라미터를 받기 위해 반드시 필요한 함수
    public static String getBodyUTF(HttpServletRequest request) throws IOException {

        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                
                char[] charBuffer = new char[2048];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        body = stringBuilder.toString();
        body = UtilString.decodeUrl(body, "UTF-8");   // @@ 페이지가 utf-8이 아닌 경우 인코딩을 수정해야 함
        
        return body;
    }
    
    // datatype이 json이 아닌 경우에도 json 객체로 받음 
    public static JSONObject getBodyJsonUTF(HttpServletRequest request) {

        JSONObject jObj = null;
        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                char[] charBuffer = new char[2048];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        body = stringBuilder.toString();
        body = UtilString.decodeUrl(body, "UTF-8");   // @@ 페이지가 utf-8이 아닌 경우 인코딩을 수정해야 함

        if(body == null || body.equals("")){
            return jObj;
        }
        
        
        try{
            jObj = new JSONObject(body);
        }catch(Exception ex1){
            try{
                jObj = mapStringToJson(body);
            }catch(Exception ex2){ 
                jObj = null;
            }
        }        
        
        
        return jObj;
    }
    
    // datatype이 json이 아닌 경우에도 map객체로 받음 
    public static ResultMap getBodyMapUTF(HttpServletRequest request) {


        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                char[] charBuffer = new char[2048];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        body = stringBuilder.toString();
        body = UtilString.decodeUrl(body, "UTF-8");   // @@ 페이지가 utf-8이 아닌 경우 인코딩을 수정해야 함

        
        if(body == null || body.equals("")){
            return null;
        }
        
        ResultMap Obj = payloadToMap(body);
        
        return Obj;
    }
    
    // datatype이 json이 아닌 경우에도 json 객체로 받음 
    // UTF 인코딩을 안 함
    public static JSONObject getBodyJson(HttpServletRequest request) {

        JSONObject jObj = null;
        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                char[] charBuffer = new char[2048];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        
        body = stringBuilder.toString();
        
        if(body == null || body.equals("")){
            return jObj;
        }
        
        try{
            jObj = new JSONObject(body);
        }catch(Exception ex1){
            try{
                jObj = mapStringToJson(body);
            }catch(Exception ex2){ 
                jObj = null;
            }
        }        
        
        return jObj;
    }
    
    private static JSONObject mapStringToJson(String payload) throws Exception{
            JSONObject jObj = new JSONObject();

            String[] keyVals = payload.split("&");
            for(String keyVal:keyVals)
            {
              String[] parts = keyVal.split("=",2);

              parts[1] = UtilString.decodeUrl(parts[1], "UTF-8");  
                 jObj.put(parts[0],parts[1]);
            }
            
           return jObj;
           
    }
//    private static JSONObject mapStringToJson(String payload) throws Exception{
//           JSONObject jObj = new JSONObject();
//           payload = payload.replace("{", "");
//           payload = payload.replace("}", "");
//
//            String[] keyVals = payload.split(", ");
//            for(String keyVal:keyVals)
//            {
//              String[] parts = keyVal.split("=",2);
//
//              if(UtilString.isValidNumeric(parts[1]) == 1){
//                  jObj.put(parts[0], Integer.parseInt(parts[1]));
//              }else if(UtilString.isValidNumeric(parts[1]) == 2){
//                  jObj.put(parts[0], Double.parseDouble(parts[1]));
//
//              }else{
//
//                 jObj.put(parts[0],parts[1]);
//              }
//
//            }
//            
//           return jObj;
//           
//    }
    
    private static ResultMap payloadToMap(String payload) {

        ResultMap Obj = new ResultMap();
        if(payload == null || payload.equals("")){
            return Obj;
        }
        
        
        try{
            String[] params = payload.split("&");
            if(params.length == 0){
                return null;
            }
            
            for(String param : params){
                String[] keyvalue = param.split("=");
                
                keyvalue[1] = UtilString.decodeUrl(keyvalue[1], "UTF-8");  
                
                if(keyvalue.length == 2){
                    
                    if(UtilString.isValidNumeric(keyvalue[1]) == 1){
                        Obj.put(keyvalue[0], Integer.parseInt(keyvalue[1]));
                    }else if(UtilString.isValidNumeric(keyvalue[1]) == 2){
                        Obj.put(keyvalue[0], Double.parseDouble(keyvalue[1]));
                    }else{
                       Obj.put(keyvalue[0], keyvalue[1]);
                    }
                }
            }
            
        }catch(Exception ex1){
            Obj = null;
        }        
            
        return Obj;
           
    }
    
    public static String sendFileToClient(HttpServletRequest request, HttpServletResponse response, String sendFileFullName) throws Exception {
        
        String filePath = FilenameUtils.getFullPath(sendFileFullName);
        String realFilNm = FilenameUtils.getName(sendFileFullName);
        String viewFileNm = realFilNm;

        File file = new File( filePath + realFilNm);

        if (file.exists() && file.isFile()) {
                response.setContentType("application/octet-stream; charset=utf-8");
                response.setContentLength((int) file.length());
                String browser = getBrowser(request);
                String disposition = getDisposition(viewFileNm, browser);
                response.setHeader("Content-Disposition", disposition);
                response.setHeader("Content-Transfer-Encoding", "binary");
                OutputStream out = response.getOutputStream();
                FileInputStream fis = null;
                fis = new FileInputStream(file);

                int byteCount = 0;
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        byteCount += bytesRead;
                }
                out.flush();


                if (fis != null)
                        fis.close();
                out.flush();
                out.close();
        }
        return filePath + realFilNm;
    }
    
    
    

    // 1. 파일로 보내기
    // HttpUtils.sendBinaryFileToClient(request, response, path);
    public static void sendBinaryFileToClient(HttpServletRequest request, HttpServletResponse response, String sendFileFullName) throws IOException {
		
        String filePath = FilenameUtils.getFullPath(sendFileFullName);
        String realFilNm = FilenameUtils.getName(sendFileFullName);
        String viewFileNm = realFilNm;

        File file = new File( filePath + realFilNm);

        if (file.exists() && file.isFile()) {
                response.setContentType("application/octet-stream; charset=utf-8");
                response.setContentLength((int) file.length());
                String browser = getBrowser(request);
                String disposition = getDisposition(viewFileNm, browser);
                response.setHeader("Content-Disposition", disposition);
                response.setHeader("Content-Transfer-Encoding", "binary");
                OutputStream out = response.getOutputStream();
                FileInputStream fis = null;
                fis = new FileInputStream(file);

                int byteCount = 0;
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        byteCount += bytesRead;
                }
                out.flush();


                if (fis != null)
                        fis.close();
                out.flush();
                out.close();
        }
    }

    
    public static String sendImageInImageDir(HttpSession session, HttpServletRequest request, HttpServletResponse response, String imgFilePath) throws IOException {

        ServletContext sc = request.getServletContext();
        String servletPath = session.getServletContext().getRealPath("/");
        String imagePath = servletPath + AppConfig.getConf("user_image_dir");

        File image = new File(imagePath, URLDecoder.decode(imgFilePath, "UTF-8"));

        if (!image.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return "No file";
        }

        String contentType = sc.getMimeType(image.getName());
        response.reset();
        response.setContentType(contentType);
        response.setHeader("Content-Length", String.valueOf(image.length()));

        FileInputStream in = new FileInputStream(image);
        OutputStream out = response.getOutputStream();
        // Files.copy(sendFile.toPath(), out);

        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = in.read(buf)) >= 0) {
            out.write(buf, 0, count);
        }
        out.close();
        in.close();

        return "send image";
    }

    public static String sendImage2(HttpServletRequest request, HttpServletResponse response, String imgFilePath) throws IOException {

        ServletContext sc = request.getServletContext();

//        String imagePath = imgFilePath;
        File image = new File(URLDecoder.decode(imgFilePath, "UTF-8"));

        if (!image.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            System.out.println(">>>> " + imgFilePath);
            return "No file";
        }

        String contentType = sc.getMimeType(image.getName());
        response.reset();
        response.setContentType(contentType);
        response.setHeader("Content-Length", String.valueOf(image.length()));

        FileInputStream in = new FileInputStream(image);
        OutputStream out = response.getOutputStream();
        // Files.copy(sendFile.toPath(), out);

        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = in.read(buf)) >= 0) {
            out.write(buf, 0, count);
        }
        out.close();
        in.close();

        return "send image";
    }

    public static String sendCanvas(HttpSession session, HttpServletRequest request, HttpServletResponse response, BufferedImage image) throws IOException {

        ServletContext sc = request.getServletContext();

        String servletPath = session.getServletContext().getRealPath("/");
//        String imagePath = servletPath + AppConfig.getConf("temp_dir");

        response.setContentType("image/png");

        OutputStream os = response.getOutputStream();
        ImageIO.write(image, "png", os);

        return "send image";
    }

    // request의 헤더 정보를 맵에 담음
    public static Map<String, String> getRequestHeadersInMap(HttpServletRequest request) {

        Map<String, String> result = new HashMap<>();

        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            result.put(key, value);
        }

        return result;
    }

    // request로 들어온 클라이언트의 IP를 얻음
    public static String getClientIp(HttpServletRequest request) {

        String remoteAddr = "";

        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }

        return remoteAddr;
    }


     // 기기의 외부 IP을 얻음
     // 외부와 연결되어 있어야 함
    public static String getPublicIP(){

        String ip = "";
        URL whatismyip = null;
        
        try {
            whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));

            ip = in.readLine(); //you get the IP as a String
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ip;
    }

    // URL에서 IP를 얻어냄
    public static String[] getIPAddressFromURL(String urlAddr) throws Exception {

        //대기업들은 서버의 과부하를 막기 위해서 하나의 도메인에 여러 개의 컴퓨터 IP를 등록할 수 있습니다. 
        InetAddress[] iaArr = InetAddress.getAllByName(urlAddr);

        String[] addrArr = new String[iaArr.length];

        for(int i=0; i<iaArr.length ; i++) {
            addrArr[i] = iaArr[i].getHostAddress();
            System.out.println("" + i + " : " + addrArr[i] );
        }

        return addrArr;
    }
    
    
    /**
     * Comment : 현재 페이지의 서블릿 URL 전체 경로를 추출.
     */
    public static String getCurrentlyURL(HttpServletRequest req) {
        Enumeration<?> param = req.getParameterNames();

        StringBuffer strParam = new StringBuffer();
        StringBuffer strURL = new StringBuffer();

        if (param.hasMoreElements()) {
            strParam.append("?");
        }

        while (param.hasMoreElements()) {
            String name = (String) param.nextElement();
            String value = req.getParameter(name);

            strParam.append(name + "=" + value);

            if (param.hasMoreElements()) {
                strParam.append("&");
            }
        }

        String url;
        if (req.getAttribute("javax.servlet.forward.request_uri") == null) {
            url = req.getRequestURI().toString();
        } else {
            url = req.getAttribute("javax.servlet.forward.request_uri").toString();
        }
        // contextPath 제거, 필요한 값(/index.do)
        url = url.replace(req.getContextPath(), "");

        //# URL 에서 URI 를 제거, 필요 값만 사용(프로토콜, 호스트, 포트)
        String getUrl = req.getRequestURL().toString().replace(req.getRequestURI(), "");
        strURL.append(getUrl);
        strURL.append(url); // servlet 경로 : /index.do 
        strURL.append(strParam); // getQueryString() 값

        // 전체 추출 경로 : http://www.aaa.co.kr/index.do?type=aaa(쿼리스트링)
        return strURL.toString();
    }

    
    // IP 주소에서 호스트 이름을 얻음
    public static String getHostName(String ipStr){
        InetAddress ip;
        String hostname = "";
        String temp = "";
        try {
            ip = InetAddress.getByName(ipStr);
            hostname = ip.getHostName();
            
            StringTokenizer token = new StringTokenizer(hostname);
            temp = token.nextToken(".");
            
            if(Integer.parseInt(temp) >= 0){
                return hostname;
            }
            
//            System.out.println("Your current IP address : " + ip);
//            System.out.println("Your current Hostname : " + hostname);
 
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            hostname = temp;
        }
        return hostname;
    }
    
    public static HashMap getNameIpMap(HashMap<String, String> macIpMap){
        HashMap<String, String> nameIpMap = new HashMap();
        
        for(String key : macIpMap.keySet()){
            nameIpMap.put(getHostName(macIpMap.get(key)), macIpMap.get(key));
        }
        return nameIpMap;
    }
    
    
    
    /**
     *  Json 데이터 요청 (Get 방식)
     *  Usage
     *         String params = "consumer_key=204ed64b782e4c94967c&consumer_secret=b01fe6c9f7ce489a8350";
     *         String tt2 = NetUtils.getURLString("https://sgisapi.kostat.go.kr/OpenAPI3/auth/authentication.json", params, "", "");
     */
    public static String ajaxGetJson(String targetUrl, String paramString, String HeaderName, String HeaderValue) throws Exception {
        StringBuffer response = new StringBuffer();

        try {
            String apiURL = targetUrl + "?" + paramString;
            
            System.out.println("apiURL = " + apiURL);
            
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept","application/json");
            con.setRequestProperty("Cache-Control","no-cache");

            if(!HeaderName.equals("") && !HeaderValue.equals("") ) {
                con.setRequestProperty(HeaderName, HeaderValue);
            }

            int responseCode = con.getResponseCode();
            BufferedReader br;
            if(responseCode==200) { // 정상 호출
                br = new BufferedReader(new InputStreamReader((InputStream)con.getContent(), "UTF-8"));   // @@ 스트림에 인코딩을 지정해야 한글이 깨지지 않음
            } else {  // 에러 발생
                br = new BufferedReader(new InputStreamReader((InputStream)con.getContent(), "UTF-8"));
            }
            String inputLine;

            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();


        } catch (Exception e) {
            System.out.println(e);
        }
        return response.toString();
    }


    /**
     * Json 데이터 요청 (Post 방식)
     *  : parameter는 JSON 객체에 넣는다.
     *
     * Usage
     *   paramObj.put("edgeId", CommonDoc.ap_ssid);
     *   paramObj.put("Time", DateTimeUtils.getTimeDateNow());
     *   paramObj.put("sigData", resultObj);
     * JSONObject paramObj = new JSONObject();
     *  String url = "http://" + masterAddress + URL_SENDSIGDATA;
     *  String resultStr = NetUtils.ajaxPostJson(url, paramObj);
     *
     */
    public static String ajaxPostJson(String targetUrl, JSONObject param) throws Exception {

        URL url = new URL(targetUrl);

        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");
        httpConn.setRequestProperty("Content-Type", "application/json");
        httpConn.setRequestProperty("Accept","application/json");
        httpConn.setDoInput(true);
        httpConn.setDoOutput(true);
        httpConn.connect();

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(httpConn.getOutputStream(), "UTF-8"));
//        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(httpConn.getOutputStream(), "EUC-KR"));
        bw.write(param.toString());
        bw.flush();
        bw.close();

        BufferedReader br = new BufferedReader(new InputStreamReader((InputStream)httpConn.getContent(), "UTF-8"));
        String tmp = null;
        StringBuilder sb = new StringBuilder();
        while ((tmp = br.readLine()) != null) {
            sb.append(tmp);
        }

        br.close();

        return sb.toString();
    }

    /**
     * 파일을 요청
     *
     * String result = NetUtils.ajaxFile("http://www.apache.org/images/asf_logo_wide.gif", "" , "C:\\Users\\mailt\\Desktop\\image.png");
     *
     * @param targetUrl
     * @param paramString
     * @param saveFileName
     * @return
     * @throws Exception
     */
    public static String ajaxFile(String targetUrl, String paramString, String saveFileName) throws Exception {
        String urlstring = targetUrl + "?" + paramString;
        
        System.out.println("urlstring = " + urlstring);
        
        InputStream inputStream = null;
        OutputStream outputStream = null;
        String result = "";
        
        /* https 도메인에 대한 접근을 위한 코드   Start of the fix */
       TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
           public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
           public void checkClientTrusted(X509Certificate[] certs, String authType) { }
           public void checkServerTrusted(X509Certificate[] certs, String authType) { }

       } };

        try {
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) { return true; }
            };
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            /* https 도메인에 대한 접근을 위한 코드   End of the fix*/
            
            
            URL url = new URL(urlstring);
            String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";
            URLConnection con = url.openConnection();
            con.setConnectTimeout(10000);
            
            con.setRequestProperty("User-Agent", USER_AGENT);

            // Requesting input data from server
            inputStream = con.getInputStream();
            outputStream = new FileOutputStream(saveFileName);

            byte[] buffer = new byte[2048];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            result = saveFileName;

        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println("Load Fail : " + urlstring );
            result = null;
        }

        return result;
    }

    public static String getBrowser(HttpServletRequest request) {
            String header = request.getHeader("User-Agent");
            if (header.indexOf("MSIE") > -1 || header.indexOf("Trident") > -1)
                    return "MSIE";
            else if (header.indexOf("Chrome") > -1)
                    return "Chrome";
            else if (header.indexOf("Opera") > -1)
                    return "Opera";
            else if (header.indexOf("Firefox") > -1)
                return "Firefox";
        return "Others";            
    }

    public static String getDisposition(String filename, String browser) throws UnsupportedEncodingException {
            String dispositionPrefix = "attachment;filename=";
            String encodedFilename = null;
            if (browser.equals("MSIE")) {
                    encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll(
                                    "\\+", "%20");
            } else if (browser.equals("Firefox")) {
                    encodedFilename = "\""
                                    + new String(filename.getBytes("UTF-8"), "8859_1") + "\"";
            } else if (browser.equals("Opera")) {
                    encodedFilename = "\""
                                    + new String(filename.getBytes("UTF-8"), "8859_1") + "\"";
            } else if (browser.equals("Chrome")) {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < filename.length(); i++) {
                            char c = filename.charAt(i);
                            if (c > '~') {
                                    sb.append(URLEncoder.encode("" + c, "UTF-8"));
                            } else {
                                    sb.append(c);
                            }
                    }
                    encodedFilename = sb.toString();
            }
            return dispositionPrefix + encodedFilename;
    }
}
