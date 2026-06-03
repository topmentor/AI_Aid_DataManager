/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ithows.ResultMap;

import java.io.FileWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class UtilJSON
 *
 * @author Roi Kim <S.O.X Co. Ltd.>
 */
public class UtilJSON {

    //    // DB에 꺼낼 때 변환
//    extentStr = extentStr.replaceAll("\\\\\"", "\"");
//
//    // DB에 넣을 때 변환
//    jObj.toString().replaceAll("\"", "\\\\\"");
    public static String JSonBeautify(String jsonStr) {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(jsonStr);

        String prettyJsonString = gson.toJson(je);

        return prettyJsonString;
    }

    public static String JSonBeautify(JSONObject jsonObj) {

        if (jsonObj == null) {
            return "";
        }

        String jsonStr = jsonObj.toString();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(jsonStr);

        String prettyJsonString = gson.toJson(je);

        return prettyJsonString;
    }

    public static String objectToJsonstring(Object obj) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(obj);
        return jsonString;
    }



    public static int getJsonElementInt(JSONObject jObj, String key, int defaultValue){
        int result = defaultValue;

        if(jObj != null && jObj.has(key)){
            try {
                Object cidObj = jObj.opt(key);
                if (cidObj instanceof Number) {
                    result = ((Number) cidObj).intValue();
                } else if (cidObj instanceof String) {
                    try {
                        result = Integer.parseInt((String) cidObj);
                    } catch (NumberFormatException nfe) {
                        System.err.println("getJsonElementInt 문자열 변환 오류: " + nfe.getMessage());
                    }
                }
            }catch(Exception e){

            }
        }
        return result;
    }


    public static long getJsonElementLong(JSONObject jObj, String key, long defaultValue){
        long result = defaultValue;

        if(jObj != null && jObj.has(key)){
            try {
                Object cidObj = jObj.opt(key);
                if (cidObj instanceof Number) {
                    result = ((Number) cidObj).longValue();
                } else if (cidObj instanceof String) {
                    try {
                        result = Long.parseLong((String) cidObj);
                    } catch (NumberFormatException nfe) {
                        System.err.println("getJsonElementLong 문자열 변환 오류: " + nfe.getMessage());
                    }
                }
            }catch(Exception e){

            }
        }
        return result;
    }


    public static double getJsonElementDouble(JSONObject jObj, String key, double defaultValue){
        double result = defaultValue;

        if(jObj != null && jObj.has(key)){
            try {
                Object cidObj = jObj.opt(key);
                if (cidObj instanceof Number) {
                    result = ((Number) cidObj).doubleValue();
                } else if (cidObj instanceof String) {
                    try {
                        result = Double.parseDouble((String) cidObj);
                    } catch (NumberFormatException nfe) {
                        System.err.println("getJsonElementdouble 문자열 변환 오류: " + nfe.getMessage());
                    }
                }
            }catch(Exception e){

            }
        }
        return result;
    }

    public static String getJsonElementString(JSONObject jObj, String key, String defaultValue){
        String result = defaultValue;

        if(jObj != null && jObj.has(key)){
            try {
                Object cidObj = jObj.opt(key);
                if (cidObj instanceof String) {
                    result = (String) cidObj;
                } else if (cidObj instanceof Number) {
                    result = "" + cidObj;
                }
            }catch(Exception e){

            }
        }
        return result;
    }




    // JSONObject를 ResultMap으로 변환
    public static ResultMap jsonToMap(JSONObject jObj) {
        ResultMap map = new ResultMap();

        Iterator itr = jObj.keys();
        while (itr.hasNext()) {
            try {
                String key = itr.next().toString();

                if (jObj.get(key) instanceof JSONObject) {
                    ResultMap subMap = jsonToMap((JSONObject) jObj.get(key));
                    map.put(key, subMap);
                } else if (jObj.get(key) instanceof JSONArray) {
                    ArrayList<ResultMap> arr = convertJSONArrayToArrayList((JSONArray) jObj.get(key));
                    map.put(key, arr);
                } else {
                    map.put(key, jObj.get(key));
                }

            } catch (JSONException ex) {
            }
        }

        return map;
    }

    // 객체를 JSONObject로 변환
    public static JSONObject objectToJsonObject(Object obj) {
        JSONObject jObj = null;

        try {
            jObj = new JSONObject(objectToJsonstring(obj));
        } catch (JSONException ex) {
            Logger.getLogger(UtilJSON.class.getName()).log(Level.SEVERE, null, ex);
        }

        return jObj;
    }

    public static JSONObject mapToJSon(Map<String, Object> payload) {
        JSONObject jObj = new JSONObject();

        try {
            for (String keyVal : payload.keySet()) {
                jObj.put(keyVal, payload.get(keyVal));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jObj;
    }

    public static JSONObject resultMapToJSon(ResultMap payload) {
        JSONObject jObj = new JSONObject();

        try {
            for (Object key : payload.keySet()) {
                jObj.put((String) key, payload.get(key));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if(jObj.length() > 0){
            jObj = keySort(jObj);
        }
        return jObj;
    }

    public static boolean writeJsonTextToFile(String jsonStr, String jsonFilName) {
        try (FileWriter writer = new FileWriter(jsonFilName)) {
            jsonStr = UtilJSON.JSonBeautify(jsonStr) ;
            writer.append(jsonStr);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    public static boolean writeJsonToFile(JSONObject jsonObj, String jsonFilName) {
        if (jsonObj == null || jsonObj.length() == 0) {
            return false;
        }

        String jsonStr = jsonObj.toString();
        try (FileWriter writer = new FileWriter(jsonFilName)) {
            jsonStr = UtilJSON.JSonBeautify(jsonStr);
            writer.append(jsonStr);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean writeJsonToFile(JSONArray jsonArr, String jsonFilName) {
        if (jsonArr == null || jsonArr.length() == 0) {
            return false;
        }

        String jsonStr = jsonArr.toString();

        try (FileWriter writer = new FileWriter(jsonFilName)) {
            jsonStr = UtilJSON.JSonBeautify(jsonStr);
            writer.append(jsonStr);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // JSon 구조의 최대 너비를 체크하는 함수
    public static int getWidth(Object Obj) {
        int max = 0;
        int width = 0;
        int depth = 0;
        JSONArray jArr = null;
        if (Obj instanceof JSONObject) {
            jArr = new JSONArray();
            jArr.put((JSONObject) Obj);
        } else if (Obj instanceof JSONArray) {
            jArr = (JSONArray) Obj;
        }

        depth = getDepth(jArr);

        for (int i = 1; i <= depth; i++) {
            width = getWidthDepth(jArr, i);
            if (max <= width) {
                max = width;
            }
        }

        return max;
    }

    private static int getWidthArray(JSONArray jArr) {
        int max = jArr.length();

        try {
            int width = 0;
            for (int i = 0; i < jArr.length(); i++) {
                JSONObject jObj = jArr.getJSONObject(i);
                Iterator itr = jObj.keys();
                while (itr.hasNext()) {
                    String key = itr.next().toString();
                    if (jObj.get(key) instanceof JSONArray) {
                        width += getWidthArray(jObj.getJSONArray(key));
                    }
                }
                if (max <= width) {
                    max = width;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return max;
    }

    // JSon 구조의 깊이를 체크하는 함수
    public static int getDepth(Object Obj) {
        int depth = 1;
        if (Obj instanceof JSONObject) {
            return getDepthObject((JSONObject) Obj, depth);
        } else if (Obj instanceof JSONArray) {
            return getDepthArray((JSONArray) Obj, depth);
        }
        return -1;
    }

    private static int getDepthObject(JSONObject jObj, int depth) {
        Iterator i = jObj.keys();
        int max = depth;
        try {
            while (i.hasNext()) {
                String key = i.next().toString();
                int dep = depth + 1;
                if (jObj.get(key) instanceof JSONObject) {
                    dep = getDepthObject(jObj.getJSONObject(key), dep);
                    if (max <= dep) {
                        max = dep;
                    }
                } else if (jObj.get(key) instanceof JSONArray) {
                    dep = getDepthArray(jObj.getJSONArray(key), dep);
                    if (max <= dep) {
                        max = dep;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return max;
    }

    // JSon배열 구조의 깊이를 체크하는 함수
    private static int getDepthArray(JSONArray jArr, int depth) {
        int max = depth;

        try {
            for (int i = 0; i < jArr.length(); i++) {
                JSONObject jObj = jArr.getJSONObject(i);
                Iterator itr = jObj.keys();
                while (itr.hasNext()) {
                    String key = itr.next().toString();
                    int dep = depth + 1;
                    if (jObj.get(key) instanceof JSONObject) {
                        dep = getDepthObject(jObj.getJSONObject(key), dep);
                        if (max <= dep) {
                            max = dep;
                        }
                    } else if (jObj.get(key) instanceof JSONArray) {
                        dep = getDepthArray(jObj.getJSONArray(key), dep);
                        if (max <= dep) {
                            max = dep;
                        }
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return max;
    }

    // 특정 depth의 총 요소의 수를 얻기
    // context 값은 2로 고정
    public static int getWidthDepth(JSONArray jArr, int depth) {

        if (depth == 1) {
            return jArr.length();
        }

        if (depth > getDepth(jArr)) {
            return 0;
        }

        ArrayList<JSONObject> nodeList = new ArrayList<JSONObject>();
        getNodesDepth(nodeList, jArr, depth, 1);
        int value = nodeList.size();

        return value;
    }

    private static void getNodesDepth(ArrayList<JSONObject> nodeList, JSONArray jArr, int depth, int context) {

        JSONArray jChildren = null;

        try {
            int count = jArr.length();
            for (int i = 0; i < count; i++) {
                JSONObject jObj = jArr.getJSONObject(i);

                if (depth > context) {
                    try {
                        jChildren = jObj.getJSONArray("children");
                    } catch (JSONException ex) {
                        jChildren = null;
                    }
                    // 요소 확인
                    if (jChildren != null && jChildren.length() > 0) {
                        getNodesDepth(nodeList, jChildren, depth, (context + 1));
                    }
                } else if (depth == context) {
                    nodeList.add(jObj);
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return;
    }

    // 단순 HashMap
    public static JSONObject convertHashMapToJSon(HashMap map) {
        JSONObject jObj = new JSONObject();

        try {
            for (Object key : map.keySet()) {
                jObj.put((String) key, map.get(key));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return jObj;
    }

    // 이중 HashMap
    public static JSONObject convertHashMapListToJSon(HashMap<String, HashMap> map) {
        JSONObject jObj = new JSONObject();

        try {
            for (String innerKey : map.keySet()) {
                HashMap innerMap = map.get(innerKey);
                JSONObject innerJObj = new JSONObject();

                for (Object key : innerMap.keySet()) {
                    innerJObj.put((String) key, innerMap.get(key));
                }
                jObj.put(innerKey, innerJObj);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return jObj;
    }

    public static JSONArray convertArrayListToJSONArray(ArrayList<ResultMap> list) {

        JSONArray jArr = new JSONArray();

        if (list.size() < 1) {
            return jArr;
        }

        try {

            for (ResultMap element : list) {
                JSONObject jObj = new JSONObject();
                for (Object key : element.keySet()) {
                    jObj.put((String) key, element.get(key));
                }
                jArr.put(jObj);
            }
        } catch (JSONException ex) {
            System.out.println(ex.getLocalizedMessage());
        }

        return jArr;
    }

    // JSONArray를 ArrayList로 변환
    public static ArrayList convertJSONArrayToArrayList(JSONArray jArr) {

        ArrayList<Object> arraylist = new ArrayList<Object>();

        if (jArr.length() < 1) {
            return arraylist;
        }

        try {
            int count = jArr.length();

            if (!(jArr.get(0) instanceof JSONObject)) {

            }

            for (int i = 0; i < count; i++) {

                if (jArr.get(i) instanceof JSONObject) {
                    JSONObject jObj = jArr.getJSONObject(i);
                    ResultMap map = jsonToMap(jObj);
                    arraylist.add(map);
                } else {
                    arraylist.add(jArr.get(i));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return arraylist;
    }

    public static void printArrayList(ArrayList<ResultMap> list, boolean arrange) {

        if (list.size() < 1) {
            return;
        }

        JSONArray jArr = convertArrayListToJSONArray(list);

        if (arrange) {
            String str = JSonBeautify(jArr.toString());
            System.out.println(str);
        } else {
            System.out.println(jArr.toString());
        }

        return;
    }

    public static JSONArray readJsonFileToJSONArray(String fileName) {

        JSONArray arr = UtilFile.readTextToJSonArray(fileName);

        return arr;

    }


    public static String convertJSONArrayToCsv(JSONArray jArr) {

        String result = "";

        if (jArr.length() < 1) {
            return result;
        }

        try {
            int count = jArr.length();

            if (!(jArr.get(0) instanceof JSONObject)) {

            }

            for (int i = 0; i < count; i++) {

                if (jArr.get(i) instanceof JSONObject) {
                    JSONObject jObj = jArr.getJSONObject(i);
                    result += jObj.toString() + ",";
                } else {
                    result += jArr.get(i).toString() + ",";
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(result.length() > 0){
            result = UtilString.trimString(result, 1, false);
        }

        return result;
    }


    public static JSONObject keySort(JSONObject src) {
        JSONObject result = new JSONObject();

        // 키를 리스트로 모아서 정렬 (스트림 안 쓰는 호환성 높인 방식)
        List<String> keys = new ArrayList<>();
        Iterator<String> it = src.keys(); // 구버전 라이브러리 대응
        while (it.hasNext()) {
            keys.add(it.next());
        }
        Collections.sort(keys); // 오름차순 정렬

        try{
            for (String key : keys) {
                Object val = src.get(key);
                if (val instanceof JSONObject) {
                    val = keySort((JSONObject) val);
                } else if (val instanceof JSONArray) {
                    val = sortArray((JSONArray) val);
                }
                result.put(key, val);
            }
        }catch(Exception e){

        }

        return result;
    }

    private static JSONArray sortArray(JSONArray arr) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            Object elem;
            try {
                elem = arr.get(i);
                if (elem instanceof JSONObject) {
                    elem = keySort((JSONObject) elem);
                } else if (elem instanceof JSONArray) {
                    elem = sortArray((JSONArray) elem);
                }
                result.put(elem);
            } catch (JSONException ex) {
                Logger.getLogger(UtilJSON.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }





    public static void main(String[] args) {
//        String jsonStr = "{\"aa\" : [  {\"1\" : {\"2\" : {\"3\" : 3}}}, {\"4\" : {\"1\" : {\"2\" : {\"3\" : 4}}}}  ] }";
//        String jsonStr = "[  {\"1\" : {\"2\" : [{\"3\" : 3},{\"4\" : 3},{\"5\" : 3} ] }}, {\"4\" : {\"1\" : {\"2\" : {\"3\" : 4}}}}  ]";
        String jsonStr = "[{\n"
                + "    \"name\": \"flare\",\n"
                + "    \"children\": [\n"
                + "        {\n"
                + "            \"name\": \"flex\",\n"
                + "            \"children\": [\n"
                + "                {\"name\": \"FlareVis\", \"value\": 4116}\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"scale\",\n"
                + "            \"children\": [\n"
                + "                {\"name\": \"IScaleMap\", \"value\": 2105,  \"children\": [ {\"name\": \"Map1\", \"value\": 1316} , {\"name\": \"Map2\", \"value\": 1312}] },\n"
                + "                {\"name\": \"Scale\", \"value\": 4268, \"children\": [ {\"name\": \"Map1\", \"value\": 1316} , {\"name\": \"Map2\", \"value\": 1312}]},\n"
                + "                {\"name\": \"OrdinalScale\", \"value\": 3770},\n"
                + "                {\"name\": \"LogScale\", \"value\": 3151},\n"
                + "                {\"name\": \"QuantitativeScale\", \"value\": 4839},\n"
                + "                {\"name\": \"QuantileScale\", \"value\": 2435},\n"
                + "                {\"name\": \"RootScale\", \"value\": 1756},\n"
                + "                {\"name\": \"LinearScale\", \"value\": 1316},\n"
                + "                {\"name\": \"ScaleType\", \"value\": 1821},\n"
                + "                {\"name\": \"TimeScale\", \"value\": 5833}\n"
                + "           ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"name\": \"display\",\n"
                + "            \"children\": [\n"
                + "                {\"name\": \"DirtySprite\", \"value\": 8833}\n"
                + "           ]\n"
                + "        }\n"
                + "    ]\n"
                + "},"
                + "{\"name\": \"IScaleMap\", \"value\": 2105,  \"children\": [ {\"name\": \"Map1\", \"value\": 1316} , {\"name\": \"Map2\", \"value\": 1312}] } \n"
                + "];";
//        int depth = 0;
//        try {
//            JSONObject jobj = new JSONObject(jsonStr);
//            depth = getDepth(jobj);
//        } catch (JSONException e) {
//            try {
//                JSONArray jobj = new JSONArray(jsonStr);
//                depth = getDepth(jobj);
//            } catch (JSONException jsonException) {
//                jsonException.printStackTrace();
//            }
//        }
//        System.out.println("depth = " + depth);

        int width = 0;
        try {
            //JSONObject jobj = new JSONObject(jsonStr);
            JSONArray arr = new JSONArray(jsonStr);
            //arr.put(jobj);
            int depth = getDepth(arr);
            System.out.println("depth= " + depth);
            width = getWidth(arr);
            System.out.println("max width= " + width);
            width = getWidthDepth(arr, 1);
            System.out.println("width 1 = " + width);
            width = getWidthDepth(arr, 2);
            System.out.println("width 2 = " + width);
            width = getWidthDepth(arr, 3);
            System.out.println("width 3 = " + width);
            width = getWidthDepth(arr, 4);
            System.out.println("width 4 = " + width);

//            HashMap<String, String> map = new HashMap<String, String>();
//            map.put("heel", "sdfdsfsd");
//            System.out.println("tojson = " + objectToJsonstring(map));
            JSONArray arr2 = new JSONArray("[1,2,3]");
            ArrayList<ResultMap> list = convertJSONArrayToArrayList(arr);
            for (Object obj : list) {
                if (obj instanceof ResultMap) {
                    System.out.println("tojson - " + objectToJsonstring((ResultMap) obj));
                } else {
                    System.out.println("tojson - " + obj);
                }
            }



            JSONObject ooo = new JSONObject(
                    "        {\n"
                            + "            \"name\": \"scale\",\n"
                            + "            \"children\": [\n"
                            + "                {\"name\": \"IScaleMap\", \"value\": 2105,  \"children\": [ {\"name\": \"Map1\", \"value\": 1316} , {\"name\": \"Map2\", \"value\": 1312}] },\n"
                            + "                {\"name\": \"Scale\", \"value\": 4268, \"children\": [ {\"name\": \"Map1\", \"value\": 1316} , {\"name\": \"Map2\", \"value\": 1312}]},\n"
                            + "                {\"name\": \"OrdinalScale\", \"value\": 3770},\n"
                            + "                {\"name\": \"LogScale\", \"value\": 3151},\n"
                            + "                {\"name\": \"QuantitativeScale\", \"value\": 4839},\n"
                            + "                {\"name\": \"QuantileScale\", \"value\": 2435},\n"
                            + "                {\"name\": \"RootScale\", \"value\": 1756},\n"
                            + "                {\"name\": \"LinearScale\", \"value\": 1316},\n"
                            + "                {\"name\": \"ScaleType\", \"value\": 1821},\n"
                            + "                {\"name\": \"TimeScale\", \"value\": 5833}\n"
                            + "           ]\n"
                            + "        }"
            );

            System.out.println(JSonBeautify(keySort(ooo)).toString());


            JSONObject oo = new JSONObject(
                    "        {\n"
                            + "            \"val1\": \"22\",\n"
                            + "            \"val2\": \"22.3\",\n"
                            + "            \"val3\": 22222,\n"
                            + "        }"
            );

            System.out.println("getJsonElementDouble =" + getJsonElementDouble(oo, "val2", 0.0));
            System.out.println("getJsonElementInt = " + getJsonElementInt(oo, "val1", 0));
            System.out.println("getJsonElementString = " + getJsonElementString(oo, "val3", "---"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
