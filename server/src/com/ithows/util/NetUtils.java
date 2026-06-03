package com.ithows.util;


import com.ithows.CommonUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Class NetUtils
 *
 * @author Roi Kim <S.O.X Co. Ltd.>
 */
public class NetUtils {

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
     * 외부 URL에서 스트링 형식 데이터 받아오기 (OpenAPI 호출시)
     *
     *   String params = "consumer_key=204ed64b782e4c94967c&consumer_secret=b01fe6c9f7ce489a8350";
     *   String tt2 = NetUtils.getURLString("https://sgisapi.kostat.go.kr/OpenAPI3/auth/authentication.json", params, "", "");
     *
     * @param targetUrl
     * @param paramString
     * @param HeaderName
     * @param HeaderValue
     * @return
     * @throws Exception
     */
    public static String getURLString(String targetUrl, String paramString, String HeaderName, String HeaderValue) throws Exception {
        StringBuffer response = new StringBuffer();

        try {
            
            String apiURL = targetUrl ;
            
            if(!paramString.equals("")){
                apiURL += "?" + paramString;
            }
                    
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


    // 서비스 가능 여부 확인 (살아있는 주소인가 확인)
    public static boolean checkURL(String urlstring) {

        InputStream inputStream = null;

        try {
            URL url = new URL(urlstring);
            String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";
            URLConnection con = url.openConnection();
            con.setRequestProperty("User-Agent", USER_AGENT);
            inputStream = con.getInputStream();

            byte[] buffer = new byte[2048];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
            }
            inputStream.close();

        } catch (Exception var8) {
            return false;
        }

        return true;
    }


    
 
    // 좌표를 주소로 변환
    public static String getNaverAddress(double longitude, double latitude) {
        String result = "";
        String clientId = "2cf6sqw1i8 ";  // @@ clientId 
        String clientSecret = "oO2bpbgv4KwRachXuERc7VvcMIVbUcqvqkLzUQd3 ";  // @@ clientSecret 
         
        try {
            String apiURL = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?coords=" + longitude + "," + latitude + "&output=json&orders=roadaddr,addr,admcode,legalcode" ; //json
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
            con.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);
            int responseCode = con.getResponseCode();
            BufferedReader br;
            if(responseCode==200) { 
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {  
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            System.out.println(response.toString());
            
            try {
                
                JSONObject jAddress = new JSONObject(response.toString());
                
                String addr1 = jAddress.getJSONArray("results").getJSONObject(0).getJSONObject("region").getJSONObject("area1").getString("name") ;
                String addr2 = jAddress.getJSONArray("results").getJSONObject(0).getJSONObject("region").getJSONObject("area2").getString("name") ;
                String addr3 = jAddress.getJSONArray("results").getJSONObject(0).getJSONObject("region").getJSONObject("area3").getString("name") ;
                String addr4 = jAddress.getJSONArray("results").getJSONObject(0).getJSONObject("region").getJSONObject("area4").getString("name") ;
                String addr5 = jAddress.getJSONArray("results").getJSONObject(0).getJSONObject("land").getString("name") ;
                String poiName = jAddress.getJSONArray("results").getJSONObject(0).getJSONObject("land").getJSONObject("addition0").getString("value") ;
                
                result = addr1 + " " + addr2 + " " +addr3 + " " + addr4 + " " + addr5 + " (" + poiName + ")";
                
                
            } catch (Exception e) {
                
                result = "";
            }
            
            
            
            
        } catch (Exception e) {
            System.out.println(e);
            result = "";
        }
        
        return result;
    }
    
    
    // @@ 현재 키가 유효하지 않아 에러 임
    public static String getNaverGeoLocation(String addrStr) {
        String result = "";
        String clientId = "2cf6sqw1i8 ";  // @@ clientId 
        String clientSecret = "oO2bpbgv4KwRachXuERc7VvcMIVbUcqvqkLzUQd3 ";  // @@ clientSecret 
         
        try {
            String addr = URLEncoder.encode("대전광역시 유성구", "UTF-8");  //주소입력
            String apiURL = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=" + addr; //json
            //String apiURL = "https://openapi.naver.com/v1/map/geocode.xml?query=" + addr; // xml
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
            con.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);
            int responseCode = con.getResponseCode();
            BufferedReader br;
            if(responseCode==200) { 
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {  
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            System.out.println(response.toString());
            result = response.toString();
        } catch (Exception e) {
            System.out.println(e);
            result = "";
        }
        
        return result;
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

        try{

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

            BufferedReader br = new BufferedReader(new InputStreamReader((InputStream)httpConn.getContent(), "UTF-8"));
            String tmp = null;
            StringBuilder sb = new StringBuilder();
            while ((tmp = br.readLine()) != null) {
                sb.append(tmp);
            }

            int code = httpConn.getResponseCode();
    //        System.out.println("code  = " + code);

            bw.close();
            br.close();

            if(code > 200){
                return "";
            }
            return sb.toString();

        }catch (Exception ex){
            throw ex;
        }

    }
     
     
     // json을 쿼리스트링으로 변환 
    public static String jsonToQueryString(JSONObject json) throws Exception {
        StringBuilder queryString = new StringBuilder();
        Iterator<String> keys = json.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);

            // URL 인코딩
            String encodedKey = URLEncoder.encode(key, "UTF-8");
            String encodedValue;

            if (value instanceof String) {
                encodedValue = URLEncoder.encode((String) value, "UTF-8");
            } else {
                encodedValue = value.toString();  // 인코딩 생략
            }

            if (queryString.length() > 0) {
                queryString.append("&");
            }

            queryString.append(encodedKey)
                    .append("=")
                    .append(encodedValue);
        }

        return queryString.toString();
    }
    
    
    // form 형식 호출 보낼때 
    public static String ajaxPostFormJson(String targetUrl, JSONObject param) throws Exception {

        String res = "";
        try{

            URL url = new URL(targetUrl);

            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestProperty("Charset", "UTF-8");
            httpConn.setRequestProperty("Accept","application/json");
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
//            httpConn.connect();

            String urlParameters = jsonToQueryString(param);
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

            // 파라미터 전송
            try (OutputStream os = httpConn.getOutputStream()) {
                os.write(postData);
            }

            // 응답 코드 확인
            int responseCode = httpConn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if(responseCode > 200){
                return res ;
            }

            // 응답 읽기
            try(BufferedReader br = new BufferedReader(new InputStreamReader((InputStream)httpConn.getContent(), "UTF-8"))) {
                String tmp = null;
                StringBuilder sb = new StringBuilder();
                while ((tmp = br.readLine()) != null) {
                    sb.append(tmp);
                }

                res= sb.toString();
            }

            httpConn.disconnect();

        }catch (Exception ex){
            throw ex;
        }

        return res;
    }
    

    // 3번 까지 호출 
    public static String ajaxThreeTryPost(String targetUrl, JSONObject param) throws Exception {

        final int COUNT = 3;
        String result = "";

        for(int i=0; i<COUNT ; i++){
            try{
                result = ajaxPostJson(targetUrl, param);
                return result;

            }catch (Exception ex){
                CommonUtils.Sleep(0.5);
            }
        }
        throw new Exception("Connect Error");
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
        InputStream inputStream = null;
        OutputStream outputStream = null;
        String result = "";

        try {
            URL url = new URL(urlstring);
            String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";
            URLConnection con = url.openConnection();
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
        }

        return result;
    }


    /**
     * 파일을 전송. 업로드 함
     *
     *    String params = "id=CopleinAP";
     *    System.out.println("result = " + NetUtils.uploadFile("http://localhost:8080/CopleinMaster/service/normalFileUpload.do", params, "C:\\Users\\mailt\\Desktop\\samplefile.zip") );
     *
     * @param urlStr
     * @param paramString
     * @param fileName
     * @return
     */
    public static String uploadFile(String urlStr, String paramString, String fileName) {
        String charset = "UTF-8";
        File uploadFile = new File(fileName);
        String requestURL = urlStr + "?" + paramString;
        String response_string = "";

        if(!uploadFile.exists()){
            return response_string;
        }

        try {
            MultipartUtility multipart = new MultipartUtility(requestURL, charset);

            multipart.addHeaderField("User-Agent", "Slave Edge");
            multipart.addHeaderField("Test-Header", "Header-Value");

            multipart.addFormField("description", "Edge Backup File");
            multipart.addFormField("keywords", "Java,upload");

            multipart.addFilePart("file", uploadFile);   // @@ 필드네임을 받는 쪽과 맞추어 주어야 한다

            List<String> response = multipart.finish();

            System.out.println("SERVER REPLIED:");

            for (String line : response) {
                response_string += line;
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }

        return response_string;
    }

    public static void main(String[] args) throws Exception {
//        String  tt = NetworkUtils.getURLString("https://jsonplaceholder.typicode.com/users", "", "", "");
//        String  tt = NetworkUtils.getURLString("http://kosis.kr/openapi/statisticsData.do?method=getList&apiKey=OTRkODBkMTU1MWIyOGVjZTkxNzkyZjliY2U1OTZhMTQ=&format=json&jsonVD=Y&userStatsId=topmentor/116/DT_MLTM_5663/2/1/20190718103658&prdSe=Y&newEstPrdCnt=1", "", "", "");
//        System.out.println(tt);

//        String params = "consumer_key=204ed64b782e4c94967c&consumer_secret=b01fe6c9f7ce489a8350";
//        String tt2 = NetUtils.getURLString("https://sgisapi.kostat.go.kr/OpenAPI3/auth/authentication.json", params, "", "");

//        String params = "id=1";
//        String tt2 = NetUtils.getURLString("http://localhost:8080/CopleinMaster/health.do", params, "", "");
//        System.out.println(tt2);
//        String tt3 = NetUtils.ajaxJson("http://localhost:8080/CopleinMaster/health.do", params);
//        System.out.println(tt3);

//        String params = "id=CopleinAP";
//        System.out.println("result = " + NetUtils.uploadFile("http://localhost:8080/CopleinMaster/service/normalFileUpload.do", params, "C:\\Users\\mailt\\Desktop\\samplefile.zip") );

//        String result = NetUtils.ajaxFile("http://www.apache.org/images/asf_logo_wide.gif", "" , "C:\\Users\\mailt\\Desktop\\image.png");
//        System.out.println(result);

        System.out.println(Integer.parseInt("123"));

    }
}
