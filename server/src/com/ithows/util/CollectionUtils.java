/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

import com.ithows.ResultMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;


/**
 * Class CollectionUtils
 *
 * @author Roi Kim <S.O.X Co. Ltd.>
 */




public class CollectionUtils {

    /**
     * ArrayList Merge    
     * 
     * ArrayList<Integer> list1 = new ArrayList<Integer>();   // Create a list
     * Collections.addAll(list1, 1, 5, 6, 11, 3, 15, 7, 8);   // Fill the list
     * ArrayList<Integer> list2 = new ArrayList<Integer>();
     * Collections.addAll(list2, 1, 8, 6, 21, 53, 5, 67, 18);
     * ArrayList<Integer> result = new ArrayList<Integer>();
     * result.addAll(list1);   // Add all values from each list to the new list
     * result.addAll(list2);
     */
    
    public static void main(String[] args) {
        
        String[] vals = {"100", "22", "33", "44", "55"};
        

        int[] intArray = { 1, 2, 3, 4, 5 };
 
        String[] strArray = Arrays.stream(intArray)
                                .mapToObj(String::valueOf)
                                .toArray(String[]::new);
        
        for(String k : strArray){
            System.out.println(""+ k) ;
        }
        
        String filename = "C:\\Users\\mailt\\Desktop\\RnD_Work\\20210819\\data\\test1_dev1_radio.csv";
        ArrayList<ResultMap> list =  CollectionUtils.readCSVFileToMaplist(filename, ";");
        System.out.println("list = " + list);
        
    }
    
    private final Class<?>[] ARRAY_PRIMITIVE_TYPES = { 
        int[].class, float[].class, double[].class, boolean[].class, 
        byte[].class, short[].class, long[].class, char[].class };
    
    
    
    public static JSONArray readJsonFileToJSONArray(String fileName){
        
       JSONArray arr = UtilFile.readTextToJSonArray(fileName);
        
        return arr;
        
        
    }
    public static ArrayList<ResultMap> readCSVFileToMaplist(String fileName, String separator){
        
        if(separator.equals("")){
            separator = ",";
        }
        
        ArrayList<ResultMap> mapList = new ArrayList<ResultMap>();
        List<String[]> strList = UtilFile.readFromCsvFile(separator, fileName);
        String[] fieldName = strList.get(0);
        
        for(int i=1; i < strList.size(); i++){
            String[] parts = strList.get(i);
            ResultMap map = new ResultMap();
            for(int j=0; j < parts.length ; j++){
                if(UtilString.isValidNumeric(parts[j]) == 1){
                    map.put(fieldName[j], Integer.parseInt(parts[j]));
                } else if(UtilString.isValidNumeric(parts[j]) == 2){
                    map.put(fieldName[j], Double.parseDouble(parts[j]));
                }else{
                    map.put(fieldName[j], parts[j]);
                }
            }
            mapList.add(map);
        }
        
        return mapList;
    }
    
    
    public static String makeListToCSV(ArrayList<ResultMap> list, String separator){

        if(list.size() < 1){
            return "No data";
        }
        
        if(separator.equals("")){
            separator = ",";
        }

        String csvStr = "" ;
        String fieldName = "";
        String fieldValue = "";
        String subValue = "";
        ResultMap map = list.get(0);
        int i=1;
        int j=1;

        // 필드 채우기
        for( Object key : map.keySet() ){
            if(i == map.size()){
                fieldName = fieldName + key + "\n";
            }else{
                fieldName = fieldName + key + separator ;
            }
            i++;
        }

        for(ResultMap element : list) {
            i=1;
            subValue = "";
            for (Object key : element.keySet()) {
                if (i == element.size()) {
                    subValue = subValue + element.get(key) + "";
                } else {
                    subValue = subValue + element.get(key) +  separator + "";
                }
                i++;
            }

            if(j == list.size()){
                fieldValue = fieldValue + subValue + "";

            }else{
                fieldValue = fieldValue + subValue + "\n";
            }
            j++;
        }
        csvStr = csvStr + fieldName + fieldValue + "\n" ;


        return csvStr;
    } 
    
    
    public static String makeListToCSVFile(ArrayList<ResultMap> list, String separator, String fileName ){

        if(list.size() < 1){
            return "";
        }
        
        if(separator.equals("")){
            separator = ",";
        }

        try {
            OutputStreamWriter csvFile = new OutputStreamWriter(new FileOutputStream(new File(fileName), false));

            String csvStr = makeListToCSV(list, separator);
            csvFile.write(csvStr);

            csvFile.flush();
            csvFile.close();

            fileName = FilenameUtils.getName(fileName);

        }catch (Exception e){
            e.printStackTrace();
            fileName = "";
        }
        
        return fileName;
    }
    
    public static void printArrayListToCSV(ArrayList<ResultMap> list, String separator) {
        String str = makeListToCSV(list,  separator);
        System.out.println(str);
    }
    
    public static void printArrayListToJson(ArrayList<ResultMap> list, boolean arrange) {
            String str = makeArrayListToJson(list, arrange);
            System.out.println(str);
    }
    
    public static String makeArrayListToJson(ArrayList<ResultMap> list, boolean arrange) {

        if(list.size() < 1){
            return "";
        }
        String str = "";
        JSONArray jArr = UtilJSON.convertArrayListToJSONArray(list);
        
        if(arrange){
            str = UtilJSON.JSonBeautify(jArr.toString());
        }else{
            str = jArr.toString();
        }
        
        return str ;
    }  
    
    
    public static JSONArray makeArrayListToJson(ArrayList<ResultMap> list) {

        if(list.size() < 1){
            return null;
        }
        String str = "";
        JSONArray jArr = UtilJSON.convertArrayListToJSONArray(list);
        
        return jArr ;
    }  
    
    

    private Object[] getArray(Object val){
        Class<?> valKlass = val.getClass();
        Object[] outputArray = null;

        for(Class<?> arrKlass : ARRAY_PRIMITIVE_TYPES){
            if(valKlass.isAssignableFrom(arrKlass)){
                int arrlength = Array.getLength(val);
                outputArray = new Object[arrlength];
                for(int i = 0; i < arrlength; ++i){
                    outputArray[i] = Array.get(val, i);
                                }
                break;
            }
        }
        if(outputArray == null) // not primitive type array
            outputArray = (Object[])val;

        return outputArray;
    }
    
    public static String[] convertStringArray(Object val){
        
       String[] strArray = null;
        if (val instanceof int[]){
            strArray = Arrays.stream((int[])val)
                        .mapToObj(String::valueOf)
                        .toArray(String[]::new);
        }else if (val instanceof long[]){
            strArray = Arrays.stream((long[])val)
                        .mapToObj(String::valueOf)
                        .toArray(String[]::new);
        }else if (val instanceof double[]){
            strArray = Arrays.stream((double[])val)
                        .mapToObj(String::valueOf)
                        .toArray(String[]::new);
        }else if (val instanceof Object[]){
            strArray = Arrays.stream((Object[])val)
                          .toArray(String[]::new);
        }
        
        return strArray;
    }
    
    public static Object[] convertObjectArray(Object val){
        if (val instanceof Object[])
           return (Object[])val;
        int arrlength = Array.getLength(val);
        Object[] outputArray = new Object[arrlength];
        for(int i = 0; i < arrlength; ++i){
           outputArray[i] = Array.get(val, i);
        }
        return outputArray;
    }
    
     public static int[] convertIntArray(String[] vals){
         int[] array = Arrays.stream(vals)
                        .mapToInt(Integer::parseInt)
                        .toArray();
        
//        for(int v : array){
//            System.out.println(""+ v) ;
//        }
        
        return array;
     }
    
     public static double[] convertDoubleArray(String[] vals){
         double[] array = Arrays.stream(vals)
                            .mapToDouble(Double::parseDouble)
                            .toArray();
        
//        for(double v : array){
//            System.out.println(""+ v) ;
//        }
        
        return array;
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
     
     
    
    // Array 중복 제거
    public static List<Object> removeDuplicates(ArrayList<Object> list)
    {
        List<Object> newList = list.stream().distinct().collect(Collectors.toList());
        return newList;
    }

    // Map 소팅 (중복을 제거해 줌)
    public static LinkedHashMap<String, String> sortMapByKey(Map<String, String> map, boolean isAsc) {
        List<Map.Entry<String, String>> entries = new LinkedList<>(map.entrySet());

        if(isAsc){
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
    
    // 리스트 소팅
    public static void sortResultMapList(ArrayList<ResultMap> list, String sortField, boolean desc){
        
        if(list.size()== 0 || list.get(0).get(sortField) == null){
            
            return;
        }
        
        ResultMap.compareField = sortField;
        
        if(desc){
            list.sort(Comparator.reverseOrder());  // 내림 차순 정렬
            
        }else{
            list.sort(Comparator.naturalOrder());  // 오름 차순 정렬
            
        }
        
    }


    // String 배열 리스트에서 특정 열의 데이터만 String 배열로 받아 냄
    public static List<String> getCSVColumn(List<String[]> dataSet, int colNum, boolean isHeader){
        if(isHeader){
            dataSet.remove(0);
        }
        List<String> columnDataList = dataSet.stream().map(x -> x[colNum]).collect(Collectors.toList());
        return columnDataList;
    }

    // String 리스트를 double 배열로 변환
    public static double[] parseDouble(List<String> dataList){
        double [] array;
        array = dataList.stream().mapToDouble(Double::parseDouble).toArray();
        return array;
    }

    // String 리스트를 int 배열로 변환
    public static int[] parseInt(List<String> dataList){
        int [] array;
        array = dataList.stream().mapToInt(Integer::parseInt).toArray();
        return array;
    }

    /**
     * 2차원 구조 CSV 파일을 읽어서 String 배열 리스트로 담음
     * @param fileName
     * @param separator
     * @return
     */
    public static List<String[]> readFromCsvFile(String fileName, String separator) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            List<String[]> list = new ArrayList<>();
            String line = "";
            while ((line = reader.readLine()) != null) {
                String[] array = line.split(separator);
                list.add(array);
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    public static boolean isEmpty(Object obj) {
                if (obj instanceof String) return obj == null || "".equals(obj.toString().trim());
                else if (obj instanceof List) return obj == null || ((List) obj).isEmpty();
                else if (obj instanceof Map) return obj == null || ((Map) obj).isEmpty();
                else if (obj instanceof Object[]) return obj == null || Array.getLength(obj) == 0;
                else return obj == null;
    }
    
    public static boolean isNotEmpty(Object obj) {
            return !isEmpty(obj);
    }

        // 리스트 복사
    public static List<Object> copyList(List<Object> listOne) throws Exception 
    {
        List<Object> copyList = new ArrayList<>();
        copyList.addAll(listOne);
        return copyList;
    }

    public static List<Object> mergeList(List<Object> listOne, List<Object> listTwo) throws Exception 
    {
        List<Object> combinedList = ListUtils.union(listOne, listTwo);
        return combinedList;
    }
    
    public static ArrayList<ResultMap> mergeList(ArrayList<ResultMap> sourceList, ArrayList<ResultMap> addList) throws Exception 
    {
        sourceList.addAll(addList);
        return sourceList;
    }
}
