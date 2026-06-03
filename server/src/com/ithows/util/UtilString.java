/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

import com.ithows.ResultMap;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

/**
 *
 * @author dreamct
 */
public class UtilString extends StringUtils {

    /*
     *   StringUtils.isBlank(null)      = true
     StringUtils.isBlank("")        = true
     StringUtils.isBlank(" ")       = true
     StringUtils.isBlank("bob")     = false
     StringUtils.isBlank("  bob  ") = false
     */

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String FILE_SEPARATOR = File.pathSeparator ;    
    
    public static int ERROR_INT_VALUE = -123456;
    public static String ERROR_STRING_VALUE = "ERROR_STRING_VALUE";

    public static void main(String[] args) {
//        String ddd = "183_1550_0,183_1694_10,183_1550_0,469_1694_2,482_450_2";
//        String ddd2 = "-100,-75,-23,-33,-770";
//        System.out.println(ddd);
//        System.out.println(">>  "  + UtilString.elementDistinctAndSort(ddd, ","));
//        
//        String res[] = new String[2];
//        res = UtilString.elementGroupDistinctAndSortByOther(ddd, ddd2, ",", false);
//        
//        System.out.println(ddd);
//        System.out.println(ddd2);
//        System.out.println("__________________________________ ");
//        System.out.println(res[0]);
//        System.out.println(res[1]);
//        
//        String sts = "asdasd|asdasd|33rr";
//        String[] list = parseCSV(sts, "|");
//        System.out.println(list[0]);
//        System.out.println(list[1]);
//        System.out.println(list[2]);
//
//
//        String lpciKeyStr = "159_3050_2,16_3050_2,18_3050_2,52_3050_0,159_3050_2,16_3050_2,18_3050_2,52_3050_0,159_3050_2,18_3050_0,52_3050_2,159_3050_2,16_3050_2,18_3050_0,52_3050_2,159_3050_2,16_3050_2,18_3050_0,52_3050_2,159_3050_2,16_3050_2,18_3050_0,239_3050_2,52_3050_2,159_3050_2,16_3050_2,18_3050_0,239_3050_2,52_3050_2,16_3050_2,18_3050_0,239_3050_2,52_3050_2,16_3050_2,18_3050_0,239_3050_2,52_3050_2,16_3050_2,18_3050_0,239_3050_2,52_3050_2,16_3050_2,18_3050_0,239_3050_2,52_3050_2,16_3050_2,18_3050_0,239_3050_2,52_3050_2,171_1694_0,272_1694_2,171_1694_0,272_1694_2,171_1694_0,272_1694_2,138_3050_2,159_2600_2,16_100_2,16_3050_2,18_2600_2,18_3050_0,206_100_2,239_3050_2,52_3050_2,159_2600_2,16_100_2,16_3050_2,18_2600_2,18_3050_0,206_100_2,239_3050_2,52_3050_2,159_2600_2,16_100_2,16_3050_2,18_2600_2,18_3050_0,206_100_2,52_3050_2,159_2600_2,16_100_2,18_2600_2,18_3050_0,206_100_2,52_3050_2,18_3050_0,52_3050_2,235_1350_2,235_2850_2,235_3200_2,344_275_2,428_275_2,82_1350_2,82_2500_2,82_2850_0,82_3200_2,235_1350_2,235_2850_2,235_3200_2,344_2";              
//        String mccStr = "198499_4,NoEnb_Sector,199147_4,199147_1,198499_4,NoEnb_Sector,199147_4,199147_1,198499_4,199147_4,199147_1,198499_4,NoEnb_Sector,199147_4,199147_1,198499_4,NoEnb_Sector,199147_4,199147_1,198499_4,NoEnb_Sector,199147_4,NoEnb_Sector,199147_1,198499_4,NoEnb_Sector,199147_4,NoEnb_Sector,199147_1,NoEnb_Sector,199147_4,NoEnb_Sector,199147_1,NoEnb_Sector,199147_4,NoEnb_Sector,199147_1,NoEnb_Sector,199147_4,NoEnb_Sector,199147_1,NoEnb_Sector,199147_4,NoEnb_Sector,199147_1,NoEnb_Sector,199147_4,NoEnb_Sector,199147_1,21134_7,20566_8,21134_7,20566_8,21134_7,20566_8,199147_0,NoEnb_Sector,199147_14,NoEnb_Sector,NoEnb_Sector,199147_4,NoEnb_Sector,NoEnb_Sector,199147_1,NoEnb_Sector,199147_14,NoEnb_Sector,NoEnb_Sector,199147_4,NoEnb_Sector,NoEnb_Sector,199147_1,NoEnb_Sector,199147_14,NoEnb_Sector,NoEnb_Sector,199147_4,NoEnb_Sector,199147_1,NoEnb_Sector,199147_14,NoEnb_Sector,199147_4,NoEnb_Sector,199147_1,199147_4,199147_1,1637_47,1637_127,1637_167,3056_99,3052_87,135086_59,NoEnb_Sector,135086_139,135086_179,1637_47,1637_127";              
//        
//        System.out.println("lpciKeyStr = " + UtilString.countElement(lpciKeyStr, ",") );
//        System.out.println("mccStr = " + UtilString.countElement(mccStr, ",") );
//        
//        String srcStr1 = ",,00:07:89:34:25:94,00:07:89:4a:50:6b";
//        String srcStr2 = "-100,-100,-82.0,-82.0";
//    
//        res = UtilString.elementGroupDistinctAndSortByOther(srcStr1, srcStr2, ",", false);
//        System.out.println(res[0]);
//        System.out.println(res[1]);
//        
        
//         String csvStr = "34582075"; // ,34582155,34582195";
//    //  00:26:66:a9:e5:48,88:3c:1c:cb:e4:c7,08:5d:dd:47:f5:b5,de:03:98:3a:e9:dd,90:9f:33:2b:54:30,f4:fd:2b:20:15:97,b4:a9:4f:a6:eb:7d,00:07:89:6a:c3:16
//        System.out.println( "" + convertFindStringFromCSV(csvStr, "lcellid")) ;

        System.out.println( removeNullCsv("333,222,333,444,,5555,", ",")) ;
        
        
    }
    
     /**
     * 첫 번째 CSV 문자열의 요소들이 두 번째 CSV 문자열에 포함되는지 확인하고,
     * 포함되는 요소들만 CSV 문자열로 반환합니다.
     * 
     * @param sourceStr 검사할 요소들이 포함된 CSV 문자열
     * @param targetStr 대상 CSV 문자열
     * @return 매칭되는 요소들로 구성된 CSV 문자열
     */
    public static String matchCSVElements(String sourceStr, String targetStr) {
        if (sourceStr == null || targetStr == null || sourceStr.isEmpty() || targetStr.isEmpty()) {
            return "";
        }

        String[] sourceElements = sourceStr.split(",");
        String[] targetElements = targetStr.split(",");
        
        StringBuilder result = new StringBuilder();
        boolean isFirst = true;

        for (String source : sourceElements) {
            source = source.trim();
            for (String target : targetElements) {
                target = target.trim();
                if (source.equals(target)) {
                    if (!isFirst) {
                        result.append(",");
                    }
                    result.append(source);
                    isFirst = false;
                    break;
                }
            }
        }

        return result.toString();
    }

     // 첫 번째 CSV 문자열의 요소들이 두 번째 CSV 문자열에 포함되는지 확인하고,
     // 포함되는 요소의 index들만 CSV 문자열로 반환합니다.
    public static String matchCSVElementIndex(String sourceStr, String targetStr) {
        if (sourceStr == null || targetStr == null || sourceStr.isEmpty() || targetStr.isEmpty()) {
            return "";
        }

        String[] sourceElements = sourceStr.split(",");
        String[] targetElements = targetStr.split(",");
        
        StringBuilder result = new StringBuilder();
        boolean isFirst = true;

        int no = 0;
        for (String source : sourceElements) {
            source = source.trim();
            no = 0;
            for (String target : targetElements) {
                target = target.trim();
                if (source.equals(target)) {
                    if (!isFirst) {
                        result.append(",");
                    }
                    result.append(no);
                    isFirst = false;
                    break;
                }
                no++;
            }
        }

        return result.toString();
    }

     // 첫 번째 CSV 문자열의 요소들이 두 번째 CSV 문자열에 포함되는지 확인하고,
     // 포함되는 요소의 수를 반환합니다.    
    public static int matchCSVElementsCount(String sourceStr, String targetStr) {
        if (sourceStr == null || targetStr == null || sourceStr.isEmpty() || targetStr.isEmpty()) {
            return 0;
        }

        int cnt = 0;
        
        String[] sourceElements = sourceStr.split(",");
        String[] targetElements = targetStr.split(",");
        

        for (String source : sourceElements) {
            source = source.trim();
            for (String target : targetElements) {
                target = target.trim();
                if (source.equals(target)) {
                    cnt++;
                    break;
                }
            }
        }

        return cnt;
    }
    
    
    // csv 리스트에 특정 element 포함여부 확인 
    public static int containCSVElementsCount(String listStr, String findStr) {
        if (listStr == null || findStr == null || listStr.isEmpty() || findStr.isEmpty()) {
            return 0;
        }

        int cnt = 0;
        
        String[] sourceElements = listStr.split(",");
        

        for (String source : sourceElements) {
            source = source.trim();
            findStr = findStr.trim();
            if (source.equals(findStr)) {
                cnt++;
                break;
            }
        }

        return cnt;
    }
    
    
    
    
     public static String[] convertListToStringArray(ArrayList<String> list){
         String[] array = list.toArray(new String[0]);
         // String[] array = list.stream().toArray(String[]::new);
         return array;
     }
     
     
     public static ArrayList<String> convertStringArrayToList(String[] array){
         ArrayList<String> mList = new ArrayList(Arrays.asList(array));
         return mList;
     }


    // 신호 형식을 FIND_IN_SET 쿼리 열로 바꾸어 주는 함수 
    // @@ 쿼리 최적화  
    // 34582075,34582155,34582195
    //  00:26:66:a9:e5:48,88:3c:1c:cb:e4:c7,08:5d:dd:47:f5:b5,de:03:98:3a:e9:dd,90:9f:33:2b:54:30,f4:fd:2b:20:15:97,b4:a9:4f:a6:eb:7d,00:07:89:6a:c3:16
    public static String convertFindStringFromCSV(String csvStr, String fieldName){
        
        if(csvStr == null || csvStr.length() == 0){
            return "";
        }        
        
        String result = " ( ";
        
        ArrayList<String> elelist = UtilString.parseListCSV(csvStr, ",");
        
        if(elelist != null && !elelist.isEmpty() && elelist.size() > 0){
            for (String ele : elelist) {
                result += "FIND_IN_SET('" + ele + "', "+ fieldName + ") > 0 or ";
                
            }
        }
        
        result = UtilString.trimString(result, 3, false);
        result += " ) ";
    
        return result; 
    }
     
    
    public static String convertQuotationStringFromCSV(String csvStr){
        
        if(csvStr == null || csvStr.length() == 0){
            return "";
        }        
        
        String result = "";
        
        ArrayList<String> elelist = UtilString.parseListCSV(csvStr, ",");
        
        if(elelist != null && !elelist.isEmpty() && elelist.size() > 0){
            for (String ele : elelist) {
                result += convertQuotationString(ele) + ",";
                
            }
        }
        
        result = UtilString.trimString(result, 1, false);
    
        return result; 
    }
     
    

    
    
    //<editor-fold desc="일반 문자열 변환">
    
    /**
     * 데이터가 숫자형식인지 확인
     * 
     * @param data
     * @return  : 숫자 여부 (1 : 정수, 2: 실수, 0 : 문자열, -1: 널)
     */
    public static int isValidNumeric(String data) {
        if (StringUtils.isBlank(data)) {//null, 빈문자, 공백문자
            return -1;
        }
        data = data.trim();//좌우 공백 제거

        try {
            int i = Integer.parseInt(data);
            return 1;
        } catch (NumberFormatException e1) {
            
        }
        
        try {
            double d = Double.parseDouble(data);
            return 2;
        } catch (NumberFormatException e2) {
            
        }
        
        return 0;
    }

    public static boolean isValidString(String data) {
        if (StringUtils.isBlank(data)) {//null, 빈문자, 공백문자
            return false;
        }
        return true;
    }

    public static int convertInt(String data) {
        if (isValidNumeric(data)==1) {
            return Integer.parseInt(data);
        } else {
            return ERROR_INT_VALUE;
        }
    }


    // 정수를 자리수에 따라 출력 (앞에 0을 붙여 줌)
    public static String convertIntToString(int value, int numberCount) {
        String format = "%0" + numberCount + "d";
        String s = String.format(format, value);
        return s;
    }

    public static String convertIntToMoneyString(long value) {
        DecimalFormat df = new DecimalFormat("###,###");
        String money = df.format(value);
        return money;
    }

    public static String convertQuotationString(String originalString) {
        String quotedString = "\"" + originalString + "\"";
        System.out.println(quotedString); // 출력: "Hello"

        return quotedString;
    }
    
    
    /**
     * double 형 스트링 변환
     * @param value
     * @param dfCount : 소수점 자리수
     * @return 
     */
    public static String convertDoubleToString(double value, int dfCount) {
        String result;
        StringBuilder fmStr = new StringBuilder("0");
        int accumValue = 1;

        if (dfCount > 0) {
            fmStr.append(".");
            for (int i = 0; i < dfCount; i++) {
                fmStr.append("0");  // '0'은 자릿수를 고정하고 0도 표시
                accumValue *= 10;
            }
        }

        value = Math.round(value * accumValue) / (double) accumValue;
        DecimalFormat format = new DecimalFormat(fmStr.toString());
        format.setGroupingUsed(false);  // 천단위 쉼표 제거
        result = format.format(value);

        return result;
    }
    
    public static String convertNumberToCommaString(double value) {
        String result;
        
        DecimalFormat format = new DecimalFormat("###,###");
        result = format.format(value);

        return result;
    }
    
    /**
     * 문자열 숫자만큼 trim하기 
     * @param source
     * @param trimCount
     * @param fromFront  : 앞에서 자르기
     * @return 
     */
    public static String trimString(String source, int trimCount, boolean fromFront){
        String result;
        
        if(source == null || source.length() == 0 ){
            return source;
        }
        
        if(fromFront){
            result = source.substring(trimCount, source.length());
        }else{
            result = source.substring(0, source.length()-trimCount);
        }
        return result;
    }
    
   

    public static String trimHead(String source, int trimCount){
        return trimString(source, trimCount, true);
    }
    
    public static String trimTail(String source, int trimCount){
        return trimString(source, trimCount, false);
    }    
    
    public static String removeSpace(String source){
        String result;
        result = source.replaceAll(" ", "");
        return result;
    }
    
    // 쉼표 구분 파싱 - 빈 값 처리
    public static String[] parseCSV(String srcStr, String charValue){
        
        ArrayList<String> list = parseListCSV(srcStr, charValue);
        
        String[] elements = list.toArray(new String[0]);
        
        return elements;
    }
    
    // 쉼표 구분 파싱 - 빈 값 처리
    public static String parseCSV(String srcStr){
        String charValue = ",";
                
        String[] elements = srcStr.split(charValue);
        
        StringBuilder result = new StringBuilder();
        boolean isFirst = true;

        for (String target : elements) {
            target = target.trim();
            if (!target.isEmpty()) {
                if (!isFirst) {
                    result.append(",");
                }
                result.append(target);
                isFirst = false;
            }
        }
        
        return result.toString();
    }
    
     // 쉼표 구분 파싱 - 빈 값 처리
    public static ArrayList<String> parseListCSV(String srcStr, String charValue){
        ArrayList<String> list =  new ArrayList<String>();

        if(charValue == null || charValue.equals("")){
            charValue = ",";
        }
        
        if(charValue.equals("|")){
            charValue = "\\|";
        }
        
        String[] elements = srcStr.split(charValue);
        for(int i=0 ; i<elements.length ; i++){
            // 비어있지 않은 것만 추가 
            if(!elements[i].trim().isEmpty()){
                list.add(elements[i]);
            }
        }
        return list;
    }
    
    // 쉼표 파싱 카운트 
    public static int countElementCSV(String srcStr){
        String charValue = ",";
                
        if(srcStr == null || srcStr.isEmpty()){
            return 0;
        }
        
        String[] elements = srcStr.split(charValue);
        
        int cnt = 0;
        for (String target : elements) {
            target = target.trim();
            if (!target.isEmpty()) {
                cnt++;
            }
        }
        
        return cnt;
    }    
    
    
    // 쉼표 구분된 2개의 리스트를 요소단위로 머지하는 함수
    // 앞에 붙인 스트링의 크기(길이)로 맞춤  
    public static String mergeCSVString(String srcStr, String srcStr2){
        String mergeStr = "";

        if(srcStr.equals("") && srcStr2.equals("")){
            return "";
        }else if(!srcStr.equals("") && srcStr2.equals("")){
            return srcStr;
        }else if(srcStr.equals("") && !srcStr2.equals("")){
            return srcStr2;
        }
                       
        String[] elements = srcStr.split(",");
        String[] elements2 = srcStr2.split(",");
        
        int lengthNum = elements.length  ;
        
        for(int i=0 ; i<lengthNum ; i++){
            if(elements2.length > i){
                mergeStr += elements[i] + "_" + elements2[i] + ",";
            }else{
                mergeStr += elements[i] + "_,";
            }
        }
        
        if(!mergeStr.equals("")){
            mergeStr = UtilString.trimTail(mergeStr, 1);
        }
        
        return mergeStr;
    }
    
    
     // 첫인자는 값을 가진 csv, 두번째는 인덱스 csv를 매칭하여 인덱스 해당값만 csv로 리턴 
    public static String parseGetValueCSV(String valueStr, String indexStr, String charValue){
        String result = "";
        if(charValue == null || charValue.equals("")){
            charValue = ",";
        }
        
        if(valueStr == null || valueStr.equals("")){
            return result;
        }else if(indexStr == null || indexStr.equals("")){
            return result;
        }
        
        String[] elements = valueStr.split(charValue);
        String[] indexes = indexStr.split(charValue);
        
        for(int i=0 ; i<indexes.length ; i++){
            int idx = Integer.parseInt(indexes[i]) ;
            result += elements[idx] + ",";
        }
        
        trimString(result, 1, false);
        
        return result;
    }
    

    // 쉼표 구분 파싱후 소팅Map 리턴 (키와 값이 같음)
    public static LinkedHashMap<String, String> parseSortedMapCSV(String srcStr, String charValue){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();


        if(charValue == null || charValue.equals("")){
            charValue = ",";
        }
        String[] elements = srcStr.split(charValue);
        for(int i=0 ; i<elements.length ; i++){
            if(elements[i] != null && !elements[i].equals("")){
                map.put(elements[i], elements[i]);
            }
        }
        return sortMapByKey(map, true);
    }

    // 쉼표 구분 파싱후 소팅Map 리턴 (키는 첫번째 인자 값은 두번째 인자로 채움)
    public static LinkedHashMap<String, String> parseSortedMapCSV(String srcStr1, String srcStr2, String charValue){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();


        if(charValue == null || charValue.equals("")){
            charValue = ",";
        }
        String[] elements1 = srcStr1.split(charValue);
        String[] elements2 = srcStr2.split(charValue);

        for(int i=0 ; i<elements1.length ; i++){

            if(elements1[i] != null && !elements1[i].equals("") && elements2[i] != null && !elements2[i].equals("")){

                if( map.get(elements1[i]) != null && !map.get(elements1[i]).equals("")){

                    try {
                        double preValue = Double.parseDouble(map.get(elements1[i]));
                        double nowValue = Double.parseDouble(elements2[i]);

                        // @@ 이전값의 조건에 따라 값을 변경 : 업데이트 정책  (지금은 큰 값으로 남김)
                        if(preValue <= nowValue){
                            map.put(elements1[i], elements2[i]);
                        }

                    } catch (Exception e) {
                        System.out.println("parseSortedMapCSV : Value Parse Error");
                    }

                }else{
                    map.put(elements1[i], elements2[i]);
                }

            }
        }
        return sortMapByKey(map, true);
    }

    
    // 쉼표 구분 파싱후 소팅Map 리턴 (키는 첫번째 인자 값은 두번째 인자로 채움)
    // 키로 소팅
    public static LinkedHashMap<String, String> parseSortedMapCSVByKey(String srcStr1, String srcStr2, String charValue){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        
        
        if(charValue == null || charValue.equals("")){
            charValue = ",";
        }
        String[] elements1 = srcStr1.split(charValue);
        String[] elements2 = srcStr2.split(charValue);
        
//        System.out.println("srcStr1 = " + srcStr1);
//        System.out.println("srcStr2 = " + srcStr2);
        int cnt = elements1.length;
        cnt = cnt > elements2.length ? elements2.length : cnt;
        
        // 중복 제거
        for(int i=0 ; i<cnt ; i++){
            
            if(elements1[i] != null && !elements1[i].equals("") && elements2[i] != null && !elements2[i].equals("")){
                if( map.get(elements1[i]) != null && !map.get(elements1[i]).equals("")){
                    
                    try {
                        double preValue = Double.parseDouble(map.get(elements1[i]));  // map에 기존에 담긴 값
                        double nowValue = Double.parseDouble(elements2[i]);

                        // @@ 이전값의 조건에 따라 값을 변경 : 중복제거시 값 선택 정책  (지금은 큰 값으로 남김)
                        if(preValue <= nowValue){
                            map.put(elements1[i], elements2[i]);
                        }
                        
                    } catch (Exception e) {
                        // 유효한 값이 아니거나 키워드 인 경우로 그냥 스킵함
//                        System.out.println(e.getLocalizedMessage());
//                        System.out.println("parseSortedMapCSV : Value Parse Error");
                    }
                    
                }else{
                    map.put(elements1[i], elements2[i]);
                }
                
            }
        }
        return sortMapByKey(map, true);
    }
 
    // 쉼표 구분 파싱후 소팅Map 리턴 (키는 첫번째 인자 값은 두번째 인자로 채움)
    public static LinkedHashMap<String, String> parseSortedMapCSVByValue(String srcStr1, String srcStr2, String charValue){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        
        
        if(charValue == null || charValue.equals("")){
            charValue = ",";
        }
        String[] elements1 = srcStr1.split(charValue);
        String[] elements2 = srcStr2.split(charValue);
        
        // 중복 제거
        for(int i=0 ; i<elements1.length ; i++){

            if(elements1[i] != null && !elements1[i].equals("") && elements2[i] != null && !elements2[i].equals("")){
                
                if( map.get(elements1[i]) != null && !map.get(elements1[i]).equals("")){
                    
                        try {
                            double preValue = Double.parseDouble(map.get(elements1[i]));   // map에 기존에 담긴 값
                            double nowValue = Double.parseDouble(elements2[i]);

                            // @@ 이전값의 조건에 따라 값을 변경 : 중복제거시 값 선택 정책  (지금은 큰 값으로 남김)
                            if(preValue <= nowValue){
                                map.put(elements1[i], elements2[i]);
                            }

                        } catch (Exception e) {
                            // 유효한 값이 아니거나 키워드 인 경우로 그냥 스킵함
    //                        System.out.println(e.getLocalizedMessage());
    //                        System.out.println("parseSortedMapCSV : Value Parse Error");
                        }
                    
                }else{
                    map.put(elements1[i], elements2[i]);
                }
                
            }
        }
        return sortMapByValue(map, true);
    }
 
    // 쉼표 구분 파싱후 소팅Map 리턴 (키는 첫번째 인자 값은 두번째 인자로 채움)
    public static LinkedHashMap<String, String> parseSortedMapCSVByValueString(String srcStr1, String srcStr2, String charValue){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        
        
        if(charValue == null || charValue.equals("")){
            charValue = ",";
        }
        String[] elements1 = srcStr1.split(charValue);
        String[] elements2 = srcStr2.split(charValue);
        
        // 중복 제거
        for(int i=0 ; i<elements1.length ; i++){

            if(elements1[i] != null && !elements1[i].equals("") && elements2[i] != null && !elements2[i].equals("")){
                
                if( map.get(elements1[i]) != null && !map.get(elements1[i]).equals("")){
                    
                        try {
                            String preValue = map.get(elements1[i]);   // map에 기존에 담긴 값
                            String nowValue = elements2[i];

                            // @@ 이전값의 조건에 따라 값을 변경 : 중복제거시 값 선택 정책  (지금은 큰 값으로 남김)
                            if(preValue.compareTo(nowValue) < 0 ){
                                map.put(elements1[i], elements2[i]);
                            }

                        } catch (Exception e) {
                            // 유효한 값이 아니거나 키워드 인 경우로 그냥 스킵함
    //                        System.out.println(e.getLocalizedMessage());
    //                        System.out.println("parseSortedMapCSV : Value Parse Error");
                        }
                    
                }else{
                    map.put(elements1[i], elements2[i]);
                }
                
            }
        }
        return sortMapByValue(map, true);
    }
 
    
    /**
     * 키에 의한 Map 소팅
     * @param map
     * @param isDesc  : true 내림차순 
     * @return 
     */
    private static LinkedHashMap<String, String> sortMapByKey(Map<String, String> map, boolean isDesc) {
        List<Map.Entry<String, String>> entries = new LinkedList<>(map.entrySet());
        
        if(isDesc){
            Collections.sort(entries, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
        }else{
            Collections.sort(entries, (o1, o2) -> o2.getKey().compareTo(o1.getKey()));
        }

        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    /**
     * 값에 의한 Map 소팅
     * @param map
     * @param isDesc  : true 내림차순 
     * @return 
     */
    private static LinkedHashMap<String, String> sortMapByValue(Map<String, String> map, boolean isDesc) {
        List<Map.Entry<String, String>> entries = new LinkedList<>(map.entrySet());
        
        if(isDesc){
            Collections.sort(entries, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));
        }else{
            Collections.sort(entries, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        }

        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    
    // 쉼표 구분 스트링의 중복제거 및 소팅
    public static String elementDistinctAndSort(String srcStr, String charValue){
        
       String result = "";

        StringBuffer sb = new StringBuffer();
        
        srcStr = srcStr != null ? removeNullCsv(srcStr, charValue) : null ;

        if(srcStr == null || srcStr.length() == 0 ){
            return srcStr;
        }

        Map<String, String> map = parseSortedMapCSV(srcStr, charValue);

        if(map != null && !map.isEmpty() && map.size() > 0){
            for (Map.Entry<String, String> entry : map.entrySet()) {
                sb.append(entry.getKey() + ",");
            }
            result = sb.toString();
            if(result.length() > 0){
                result = trimString(result, 1, false);
            }
        }else{
            result = srcStr;
        }

        return result;
    }
    
    // 쉼표 구분 스트링의 중복제거 및 소팅 2 : 삭제할 엘리먼트 지정 
    public static String elementDistinctAndSortDelete(String srcStr, String charValue, String deleteStr){
        
       String result = "";

        StringBuffer sb = new StringBuffer();

        srcStr = srcStr != null ? removeNullCsv(srcStr, charValue) : null ;
        
        if(srcStr == null || srcStr.length() == 0 ){
            return srcStr;
        }

        Map<String, String> map = parseSortedMapCSV(srcStr, charValue);

        if(map != null && !map.isEmpty() && map.size() > 0){
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if(!entry.getKey().equals(deleteStr)){
//                    result += entry.getKey() + ",";
                    sb.append(entry.getKey() + ",");
                }

            }
            result = sb.toString();
            if(result.length() > 0){
                result = trimString(result, 1, false);
            }
            
        }else{
            result = srcStr;
        }

        return result;
    }
    

    /**
     * 첫번째 인자 리스트 순서로(중복제거) 두번째 인자의 값을 배치하기
     * 첫번째 인자의 소팅 순서대로 두번째 인자의 값을 배치하기 
     * 
     * @param srcStr1
     * @param srcStr2
     * @param charValue
     * @return  : 첫번째 요소는 소팅된 srcStr1, 두번째 요소는 첫번째 키에 따른 값 리스트 
     */
    public static String[] elementGroupDistinctAndSort(String srcStr1, String srcStr2, String charValue){
        
        String result[] = new String[2];
        result[0] = "";
        result[1] = "";

        srcStr1 = srcStr1 != null ? removeNullCsv(srcStr1, charValue) : null ;
        srcStr2 = srcStr2 != null ? removeNullCsv(srcStr2, charValue) : null ;
        

        if(srcStr1 == null || srcStr1.length() == 0 || srcStr2 == null || srcStr2.length() == 0){
            return result;
        }
        
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();

        Map<String, String> map = parseSortedMapCSVByKey(srcStr1, srcStr2, charValue);

        if(map != null && !map.isEmpty() && map.size() > 0){
            for (Map.Entry<String, String> entry : map.entrySet()) {
//                result[0] += entry.getKey() + ",";
//                result[1] += entry.getValue() + ",";
                sb1.append(entry.getKey() + ",");
                sb2.append(entry.getValue() + ",");
            }

            if(sb1.length() > 0){
                result[0] = sb1.toString();
                result[0] = trimString(result[0], 1, false);
            }

            if(sb2.length() > 0){
                result[1] = sb2.toString();
                result[1] = trimString(result[1], 1, false);
            }

        }

        return result;
    }
    
    
    /**
     * 두 번째 인자 값으로 첫번째 인자를 소팅
     * @param srcStr1
     * @param srcStr2
     * @param charValue
     * @param isString : 스트링 여부
     * @return 
     */
    public static String[] elementGroupDistinctAndSortByOther(String srcStr1, String srcStr2, String charValue, boolean isString){
        
        String result[] = new String[2];
        result[0] = "";
        result[1] = "";

        srcStr1 = srcStr1 != null ? removeNullCsv(srcStr1, charValue) : null ;
        srcStr2 = srcStr2 != null ? removeNullCsv(srcStr2, charValue) : null ;
        
        if(srcStr1 == null || srcStr1.length() == 0 || srcStr2 == null || srcStr2.length() == 0){
            return result;
        }
        
        
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();

        Map<String, String> map = null;
        if(isString){
            map = parseSortedMapCSVByValueString(srcStr1, srcStr2, charValue);
        }else{
            map = parseSortedMapCSVByValue(srcStr1, srcStr2, charValue);

        }

        if(map != null && !map.isEmpty() && map.size() > 0){
            for (Map.Entry<String, String> entry : map.entrySet()) {
//                result[0] += entry.getKey() + ",";
//                result[1] += entry.getValue() + ",";
                sb1.append(entry.getKey() + ",");
                sb2.append(entry.getValue() + ",");
            }

            if(sb1.length() > 0){
                result[0] = sb1.toString();
                result[0] = trimString(result[0], 1, false);
            }

            if(sb2.length() > 0){
                result[1] = sb2.toString();
                result[1] = trimString(result[1], 1, false);
            }
        }

        return result;
    }
    
    
    /**
     * CSV로 된 문자열을 파싱해서 엘리먼트수 만큼만 자르기
     * @param srcStr
     * @param elementCount
     * @return 
     */
    public static String trimArray(String srcStr, int elementCount){
        String[] array = parseCSV(srcStr, ",");
        int cnt = array.length > elementCount ? elementCount : array.length ;
        String result = "";
        
        for(int i=0; i<cnt; i++){
            result += array[i] + ",";
        }
        
        result = trimString(result, 1, false);
        
        return result;
    }
    
    public static String[] trimArray(String[] srcStr, int elementCount){
        
        if(srcStr == null || srcStr.length ==0){
            return null;
        }
        int arrayCount = srcStr.length;
        String[] result = new String[arrayCount];
        for(int i=0 ; i<arrayCount ; i++){
            result[i] = trimArray(srcStr[i], elementCount);
        }
        
        return result;
    }
    
//    public static String elementGroupSort(String srcStr1, String srcStr2, String charValue){
//        
//        String result[] = new String[2];
//        result[0] = "";
//        result[1] = "";
//        
//        if(srcStr1 == null || srcStr1.length() == 0 || srcStr2 == null || srcStr2.length() == 0){
//            return result;
//        }        
//        
//        Map<String, String> map = parseSortedMapCSV(srcStr1, srcStr2, charValue);
//        
//        if(map != null && !map.isEmpty() && map.size() > 0){
//            for (Map.Entry<String, String> entry : map.entrySet()) {
//                result[0] += entry.getKey() + ",";
//                result[1] += entry.getValue() + ",";
//            }
//            result[0] = trimString(result[0], 1, false);
//            result[1] = trimString(result[1], 1, false);
//        }
//       
//        return result;
//    }
    
    // 쉼표 구분 스트링의 중복제거 카운트 
    public static int countElementDistinct(String srcStr, String charValue){
        
        String result = "";
        int cnt = 0;
        
        if(srcStr == null || srcStr.length() == 0 ){
            return cnt;
        }        
        
        Map<String, String> map = parseSortedMapCSV(srcStr, charValue);
        
        if(map != null && !map.isEmpty() && map.size() > 0){
            cnt = map.size();
        }
       
        return cnt;
    }
    

    public static String removeNullCsv(String str, String charValue){

        if(str.isEmpty()){
            return "";
        }

        if(charValue == null || charValue.equals("")){
            charValue = ",";
        }

        String[] elements = str.split(charValue);

        StringBuffer buf = new StringBuffer();

        for(String ele : elements){
            if(ele.equals("")){
                continue;
            }
            buf.append(ele + ",");
        }

        String result = buf.toString();
        if(result.length() > 0){
            result = trimTail(result, 1);
        }

        return result;
    }
    
    public static String removeDuplicates(String csv, String charValue) {
        if (csv == null || csv.isEmpty()) {
            return csv;
        }

        if(charValue == null || charValue.equals("")){
            charValue = ",";
        }
        
        String[] items = csv.split(charValue);
        Set<String> uniqueItems = new LinkedHashSet<>();

        for (String item : items) {
            uniqueItems.add(item.trim());
        }

        StringJoiner joiner = new StringJoiner(charValue);
        for (String item : uniqueItems) {
            joiner.add(item);
        }

        return joiner.toString();
    }    
    
    
    public static int countElement(String srcStr, String charValue){
        
        String result = "";
        int cnt = 0;
        
        if(srcStr == null || srcStr.length() == 0 ){
            return cnt;
        }        
        
        ArrayList<String> list =  parseListCSV(srcStr, charValue);
        
        if(list != null && !list.isEmpty() && list.size() > 0){
            cnt = list.size();
        }
       
        return cnt;
    }
    
     // 쉼표 구분 스트링안에 검색 스트링이 포함되어 있는지 확인
    public static int withInString(String srcStr, String tgtStr){
        int res = -1;
        
        String[] elements = srcStr.split(",");
        for(int i=0 ; i<elements.length ; i++){
            if(tgtStr.equals(elements[i])){
                res = i;
                break;
            }
        }
        return res;
    }

    // 쉼표 구분 스트링안에 검색 값이 포함되어 있는지 확인
    public static int withInString(String srcStr, int value){
        int res = -1;
        
        String[] elements = srcStr.split(",");
        for(int i=0 ; i<elements.length ; i++){
            if(value == Integer.parseInt(elements[i])){
                res = i;
                break;
            }
        }
        return res;
    }

    
    

    public static String replaceAll(String src, String oldstr, String newstr) {
        if (src == null) {
            return null;
        }

        StringBuilder dest = new StringBuilder("");
        int len = oldstr.length();
        int srclen = src.length();
        int pos = 0;
        int oldpos = 0;

        while ((pos = src.indexOf(oldstr, oldpos)) >= 0) {
            dest.append(src.substring(oldpos, pos));
            dest.append(newstr);
            oldpos = pos + len;
        }

        if (oldpos < srclen) {
            dest.append(src.substring(oldpos, srclen));
        }

        return dest.toString();
    }

    // 스트링 내 숫자 추출
    public static String extractInt(String str) {
        String numeral = "", temp = "";
        if (str == null) {
            numeral = null;
        } else {
            for (int i = 0; i < str.length(); i++) {
                temp = str.substring(i, i + 1);
                if (Character.isDigit(str.charAt(i))) {//isDigit
                    numeral += temp;
                }
            }
        }
        return numeral;
    }

    // 반복 문자열 만들기
    public static String repeatString(String textStr, int time_num) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < time_num; i++) {
            sb.append(textStr);
        }
        return sb.toString();
    }
    
    
    // 구분자로 구분된 스트링을 특정 엘리먼트 부분만 추출해서 스트링으로 리턴
    public static String extractString(String textStr, String suffix, int start_num) {
        return extractString(textStr, suffix, start_num, false) ;
    }
    
    public static String extractString(String textStr, String suffix, int start_num, boolean quotation) {
        String result = "";
        String regSuffix = "";
        
        if(suffix.equals(".")){
            regSuffix = "\\.";
        }else if(suffix.equals("|")){
            regSuffix = "\\|";
        }else{
            regSuffix = suffix;
        }
        
        String[] partList = textStr.split(suffix);

        if(partList != null && partList.length > 0 ){
            for(int i=start_num ; i < partList.length ; i++){
                
                if(quotation){
                    result += "\"" +  partList[i] + "\"" + suffix;
                    
                }else{
                    result += partList[i] + suffix;
                }
                
            }
            
            if(result.length() > 0){
                result = trimString(result, suffix.length(), false);
            }
        }
        
        return result;
        
    }
    
    // 스트링 배열을 하나의 문자열로 합치기
    public static String extractString(String[] partList, String suffix, int start_num) {
        return extractString(partList, suffix, start_num, false);
    }
    
    // 스트링 배열을 하나의 문자열로 합치기(구분자, " 추가 여부 지정)
    public static String extractString(String[] partList, String suffix, int start_num, boolean quotation) {
        String result = "";
        String regSuffix = "";
        
        if(suffix.equals(".")){
            regSuffix = "\\.";
        }else if(suffix.equals("|")){
            regSuffix = "\\|";
        }else{
            regSuffix = suffix;
        }

        if(partList != null && partList.length > 0 ){
            for(int i=start_num ; i < partList.length ; i++){
                if(quotation){
                    result += "\"" +  partList[i] + "\"" + suffix;
                    
                }else{
                    result += partList[i] + suffix;
                }
            }
            
            if(result.length() > 0){
                result = trimString(result, suffix.length(), false);
            }
        }
        
        return result;
        
    }
    
    
    public static JSONObject mapStringToJson(String payload) throws Exception{
           JSONObject jObj = new JSONObject();
           payload = payload.replace("{", "");
           payload = payload.replace("}", "");

            String[] keyVals = payload.split(", ");
            for(String keyVal:keyVals)
            {
              String[] parts = keyVal.split("=",2);

              if(UtilString.isValidNumeric(parts[1]) == 1){
                  jObj.put(parts[0], Integer.parseInt(parts[1]));
              }else if(UtilString.isValidNumeric(parts[1]) == 2){
                  jObj.put(parts[0], Double.parseDouble(parts[1]));

              }else{

                 jObj.put(parts[0],parts[1]);
              }

            }
            
           return jObj;
           
    }
    
    // key=value& 스트링을 Map으로 변환 
    public static ResultMap mapStringToMap(String payload) throws Exception{
           ResultMap map = new ResultMap();
           
            String[] keyVals = payload.split("&");
            for(String keyVal:keyVals)
            {
              String[] parts = keyVal.split("=",2);

              if(UtilString.isValidNumeric(parts[1]) == 1){
                  map.put(parts[0], Integer.parseInt(parts[1]));
              }else if(UtilString.isValidNumeric(parts[1]) == 2){
                  map.put(parts[0], Double.parseDouble(parts[1]));

              }else{
                 map.put(parts[0],parts[1]);
              }

            }
            
           return map;
           
    }
    
    // "를 \"로 치환 
    public static String replaceQutation(String sourceStr){
        if(sourceStr.equals("")){
            return "";
        }
        
        return sourceStr.replaceAll("\"", "\\\\\"");
    }
    
    //</editor-fold>




    //<editor-fold defaultstate="collapsed" desc="HTML 문자열 변환">
    public static String string2html(String args) {
        args = args.replace("\r\n", "");
        args = args.replace("\n\n", "");
        args = args.replace("\n", "");
        args = args.replace("'", "&#39;");
        args = args.replace("`", "&#39;");
        args = args.replace("\"", "&quot;");
        args = args.replace("&", "&amp;");
        args = args.replace(">", "&gt;");
        args = args.replace("<", "&lt;");
        args = args.replace("\\", "&backslush;");
        args = args.replace("@", "&golbang;");
        return args;
    }


    public static String string2javascript(String raw) {
        String result = raw;
        result = result.replace("\r\n", "");
        result = result.replace("\n\n", "");
        result = result.replace("<", "~^lt;");
        result = result.replace(">", "~^gt;");
        result = result.replace("'", "~^quote;");
        result = result.replace("\"", "~^doublequote;");
        result = result.replace("&", "~_amp;");
        result = result.replace("%", "~_pecent;");
        return result;
    }

    public static String removeHTMLTag(String text) {

        // 정규표현식으로 제거
        text = text.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", "");

        // 한줄로 할려면 아래 추가
        // text = text.replaceAll("("\r|\n|&nbsp;","");
        return text;
    }
 

    /**
     * # Base64 encode/decode
     */
    public String convertBase64(String type, String str) {
        String result = null;
        if (type.equals("enc")) {
            result = new String(Base64.encodeBase64(str.getBytes()));
        } else if (type.equals("dec")) {
            result = new String(Base64.decodeBase64(str.getBytes()));
        }
        return result;
    }

    /**
     * # URL encode/decode
     * 웹 리퀘스트시 특수기호 변환처리
     */
    public static String encodeUrl(String str, String charset) {
        if(charset.equals("")){
            charset = "UTF-8";
        }
        return convertUrl("enc", str, charset);
    }
    
    public static String decodeUrl(String str, String charset) {
        if(charset.equals("")){
            charset = "UTF-8";
        }
        return convertUrl("dec", str, charset);
    }
    
    public static String convertUrl(String type, String str, String charset) {
        String result = null;
        URLCodec urlCodec = new URLCodec();
//        System.out.println("# urlCodec.getDefaultCharset() :"+urlCodec.getDefaultCharset());
        try {
            if (type.equals("enc")) {
                result = urlCodec.encode(str, charset);
            } else if (type.equals("dec")) {
                result = urlCodec.decode(str, charset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String convertQutString(String source) {
        String result = "";

        source = source.replace(" ", "");
        String[] array = source.split(",");

        for (int i = 0; i < array.length; i++) {
            System.out.println(array[i]);

            if (i == array.length - 1) {
                result += "'" + array[i] + "'";
            } else {
                result += "'" + array[i] + "',";
            }

        }

        return result;
    }
    
    public static String convertMacString(String mac){
        
        if(mac.contains(":")){
            return mac.toLowerCase();
        }

        if(mac.contains("-")){
            String newStr = mac.replace("-", ":");
            return newStr.toLowerCase();
        }
        
        StringBuffer macstr = new StringBuffer(mac);
        StringBuffer str = new StringBuffer();
        
        for (int i = 0; i < macstr.length() ; i++) {
            str.append(macstr.charAt(i));
            if(i < (macstr.length()-1) && i%2 == 1){
                str.append(":");
            }
        }
        
        return str.toString().toLowerCase();
    }

    public static String decode(String uni) {
        StringBuffer str = new StringBuffer();
        for (int i = uni.indexOf("\\u"); i > -1; i = uni.indexOf("\\u")) {// euc-kr(%u), utf-8(//u)
            str.append(uni.substring(0, i));
            str.append(String.valueOf((char) Integer.parseInt(uni.substring(i + 2, i + 6), 16)));
            uni = uni.substring(i + 6);
        }
        str.append(uni);
        return str.toString();
    }
    
    
    
    // 유니코드에서 String으로 변환
    public static String convertUniToString(String escaped) {
        
        if(escaped.indexOf("\\u")==-1)
        return escaped;

        String processed="";

        int position=escaped.indexOf("\\u");
        while(position!=-1) {
            if(position!=0)
                processed+=escaped.substring(0,position);
            String token=escaped.substring(position+2,position+6);
            escaped=escaped.substring(position+6);
            processed+=(char)Integer.parseInt(token,16);
            position=escaped.indexOf("\\u");
        }
        processed+=escaped;

        return processed;
    }

    // 유니코드(\\uXXXXX) 부분만 삭제에서 String으로 변환
    public static String deleteUnicode(String escaped) {
        if(escaped.indexOf("\\u")>0){
            return "";
        }
        
        // 특수문자는 다지운다.
        escaped =escaped.replaceAll("(\r\n|\r|\n|\n\r)", "");

        String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
        escaped =escaped.replaceAll(match, "");

        
        return escaped;
    }

    
    
    //</editor-fold>
   
    // IP 어드레스 형식 확인 
    public static boolean isValidIP(final String s) {
        final String IPV4_REGEX = "(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))";
        Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);
        return IPV4_PATTERN.matcher(s).matches();
    }
    
    // Mac 어드레스 형식 확인 
    public static boolean isValidMAC(final String s) {
        Pattern p = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
        Matcher m = p.matcher(s);
        return m.find();
    }
    
    
    public static int getMacAddressPart(String mac) {
        if (mac == null) {
            return 0;
        }

        String[] parts = mac.split(":");

        return parts.length;
    }
    

}
