/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.util;

import com.ithows.ResultMap;
import com.ithows.util.UtilJSON;
import com.ithows.util.UtilString;
import com.ithows.util.UtileXmlJson;
import com.sox.ltex.model.LTEKeyObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.json.JSONArray;

/**
 *
 * @author mailt
 */
public class LTEKeyParser {
    public static void main(String[] args) {
        String str = "166_1550_0_222,303_3200_2_0,428_3200_0_333,82_3200_2_0,138_100_2_0,138_2600_2_0,138_3050_0_777,59_2600_2_0";
        String keysCsv   = "5,6,6,6,5,8,6,6,5,5,5,5,5,8";
        String valuesCsv = "163_0_2_0,264_100_2_0,265_100_2_0,265_3050_2_0,266_0_2_0," +
                "407_1550_0_44147212,416_100_2_0,416_3050_0_37282334," +
                "458_0_2_0,458_2850_0_8886043,461_0_2_0,489_0_2_0,489_2850_2_0,69_1550_2_0";
//        String[] part = departKey(str);
//        System.out.println("part[0] = " + part[0]);
//        System.out.println("part[1] = " + part[1]);
//        System.out.println("part[2] = " + part[2]);
//        System.out.println("keyList = " + makeKey(part[0], part[1], part[2]));
//        String res = collectServingCellKey(str);
//        System.out.println(str + " ==> " + res);
//        System.out.println(" count ==> " + countServingCellKey(str) );


        String[] part = devideCorpLtekey(keysCsv, valuesCsv);
        System.out.println("part[0] = " + part[0]);
        System.out.println("part[1] = " + part[1]);
        System.out.println("part[2] = " + part[2]);
    }

    public static String[] devideCorpLtekey(String keysCsv, String valuesCsv) {

        String[] ltekeys = new String[3];
        ltekeys[0] = "";  // KT
        ltekeys[1] = "";  // SKT
        ltekeys[2] = "";  // LG

        ResultMap res = groupByCorp(keysCsv, valuesCsv);


        Map<Object, List<String>> resMap = (Map<Object, List<String>>) res;
        for (Map.Entry<Object, List<String>> entry : resMap.entrySet()) {
            int corpNum = Integer.parseInt(entry.getKey().toString());
            String valueListStr = String.join(", ", entry.getValue());

//            System.out.println("[" + corpNum + "]");
//            System.out.println("[" + valueListStr + "]");

            if(valueListStr.isEmpty()){
                continue;
            }

            if(corpNum == 8){  // KT
                ltekeys[0] = valueListStr + ",";

            }else if(corpNum == 5 || corpNum == 12){  // SKT
                ltekeys[1] = valueListStr + ",";

            }else if(corpNum == 6 || corpNum == 7){ // LG
                ltekeys[2] = valueListStr + ",";
            }

        }

        ltekeys[0] = UtilString.trimString(ltekeys[0], 1, false);
        ltekeys[1] = UtilString.trimString(ltekeys[1], 1, false);
        ltekeys[2] = UtilString.trimString(ltekeys[2], 1, false);

        return ltekeys;
    }


    // mnc corpNum 을 키로 해서 그룹핑 하는 로직
    private static ResultMap groupByCorp(String keysCsv, String valuesCsv) {
        String[] keys   = keysCsv.split(",");
        String[] values = valuesCsv.split(",");

        if (keys.length != values.length) {
            System.out.println("CSV 길이 불일치: keys=" + keys.length + ", values=" + values.length);
            return null;
        }

        ResultMap result = new ResultMap();

        for (int i = 0; i < keys.length; i++) {
            int key = Integer.parseInt(keys[i].trim());
            String value = values[i].trim();

            if(key > 0){
                if(result.containsKey(key)){
                    ArrayList<String> existingList = (ArrayList<String>) result.get(key);
                    existingList.add(value);
                } else {
                    List<String> newList = new ArrayList<>();
                    newList.add(value);
                    result.put(key, newList);
                }
            }

        }

        return result;
    }



    // 중복된 키 제거
    public static String makeDistinctKey(String keyListStr){
        return  UtilString.elementDistinctAndSort(keyListStr, ",");
    }
    
    // 키를 만들고 중복된 키 제거
    public static String makeDistinctKey(String pciListStr, String chnListStr, String ctListStr ){
        
        String keyListStr = makeKey(pciListStr, chnListStr, ctListStr);
        return  UtilString.elementDistinctAndSort(keyListStr, ",");
    }
    
    
    public static String makeKey(String pciListStr, String chnListStr, String ctListStr ){
        
        String keyListStr ="";
        
        int pCount = UtilString.countElement(pciListStr, ",");
        int cnCount = UtilString.countElement(chnListStr, ",");
        int ctCount = UtilString.countElement(ctListStr, ",");
        
        if(pCount <= 0 || pCount != cnCount || cnCount != ctCount  ||  pCount != ctCount){
            return "";
        }

        String[] pList = UtilString.parseCSV(pciListStr, ",");
        String[] cnList = UtilString.parseCSV(chnListStr, ",");
        String[] ctList = UtilString.parseCSV(ctListStr, ",");
        
        for(int i=0; i<pCount ; i++){
            keyListStr += pList[i] + "_" + cnList[i] + "_" + ctList[i] + ",";
        }
        
        keyListStr = UtilString.trimString(keyListStr, 1, false);
        
        return keyListStr;
        
    }
    
    // @@ 서빙셀 키만 추려내는 로직
    // 2022.11 수정 요청
    public static String collectServingCellKey(String keyListStr){
        if(keyListStr.equals("")){
            return "";
        }
        
        String result = "";
        
        ArrayList<String> parseList = UtilString.parseListCSV(keyListStr, ",");
        for(String str : parseList){
            if(str.equals("")){
                continue;
            }
            
            String[] part = UtilString.parseCSV(str, "_");
            
            if(part.length<3){
                continue;
            }
            
            if(part[2].equals("0")){
                result += str + ",";
            }
        }
        
        if(!result.equals("")){
            result = UtilString.trimString(result, 1, false);
        }
        
        return result;
        
    }
    
    // @@ 서빙셀 키수 
    public static int countServingCellKey(String keyListStr){
        if(keyListStr.equals("")){
            return 0;
        }
        
        int count = 0;
        
        ArrayList<String> parseList = UtilString.parseListCSV(keyListStr, ",");
        for(String str : parseList){
            if(str.equals("")){
                continue;
            }
            
            String[] part = UtilString.parseCSV(str, "_");
            
            if(part.length<3){
                continue;
            }
            
            if(part[2].equals("0")){
                count ++;
            }
        }
        
       
        
        return count;
        
    }
    
    // @@ LTE Key 분리 로직 
    public static String[] departKey(String keyListStr){
        
        if(keyListStr ==null || keyListStr.equals("")){
            return null;
        }
        
        String[] partList = new String[4];
        partList[0] = "";
        partList[1] = "";
        partList[2] = "";
        partList[3] = "";
        
        ArrayList<String> parseList = UtilString.parseListCSV(keyListStr, ",");
        for(String str : parseList){
            String[] part = UtilString.parseCSV(str, "_");
            
            if(part.length < 4){
                continue;
            }
            partList[0] += part[0] + ",";
            partList[1] += part[1] + ",";
            partList[2] += part[2] + ",";
            partList[3] += part[3] + ",";
        }
        
        partList[0] = UtilString.trimString(partList[0], 1, false);
        partList[1] = UtilString.trimString(partList[1], 1, false);
        partList[2] = UtilString.trimString(partList[2], 1, false);
        partList[3] = UtilString.trimString(partList[3], 1, false);
        
        return partList;
        
    }
    
    
    public static int getMNC(String corpName){
        
        int mnc = -1;
        if(corpName.toUpperCase().equals("KT")){  // KT
            mnc = 8;
        }else if(corpName.toUpperCase().equals("SKT")){  // SKT
            mnc = 5;
        }else if(corpName.toUpperCase().equals("LG")){ // LG
            mnc = 6;
        }
        return mnc;
    }
    
    public static int getCorpNumber(String corpName){
        
        int corpNum = -1;
        if(corpName.toUpperCase().equals("KT")){  // KT
            corpNum = 1;
        }else if(corpName.toUpperCase().equals("SKT")){  // SKT
            corpNum = 2;
        }else if(corpName.toUpperCase().equals("LG")){ // LG
            corpNum = 3;
        }
        return corpNum;
    }
    
    
    public static String getCorpName(int mnc){
        
        String corpName = "";
        if(mnc == 8 ){  // KT
            corpName = "kt";
        }else if(mnc == 5 || mnc == 12){  // SKT
            corpName = "skt";
        }else if(mnc == 6 || mnc == 7){ // LG
            corpName = "lg";
        }
        
        return corpName;
    }
    
    
    // 다른 LTE 리스트만 모아 담아서 JSON으로 보내 줌
    public static JSONArray getOtherLTEListAll(LTEKeyObject[] otherKeyList){
        
        ArrayList<ResultMap> allList = new ArrayList<ResultMap>();
        if(otherKeyList[0] != null){
            System.out.println("otherKeyList[0] = " + otherKeyList[0].telecomName + "  >>>  " +  otherKeyList[0].keyList );
        }
        if(otherKeyList[1] != null){
            System.out.println("otherKeyList[1] = " + otherKeyList[1].telecomName + "  >>>  " +  otherKeyList[1].keyList );
        }
        
        if(otherKeyList != null){
            for(int k=0; k<2 ; k++){
                if(otherKeyList[k]!= null && otherKeyList[k].matchList != null){
                     allList.addAll(otherKeyList[k].matchList);
                }
            }
        }
        
        return UtilJSON.convertArrayListToJSONArray(allList);
        
    }
    
}
