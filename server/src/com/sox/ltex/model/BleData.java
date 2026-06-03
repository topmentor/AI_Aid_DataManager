/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.model;

import com.ithows.util.DateTimeUtils;
import com.ithows.util.UtilString;
import java.io.PrintStream;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



/**
 * BLE 데이터 형식
 */
public class BleData {
    public String time = "";
    public int major = 0;
    public int minor = 0;
    public int rssi = -100;
    public String uuid = "";
    public String mac = "";
    
    public void print(PrintStream out){
        out.println("   time : " + time );
        out.println("   rssi : " + rssi);
        out.println("   uuid : " + uuid );
        out.println("   mac : " + mac );
        out.println("   major : " + major );
        out.println("   minor : " + minor );
    }
    
    
    public JSONObject getJSON(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("time", time);
            obj.put("rssi", rssi);
            obj.put("uuid", uuid);
            obj.put("mac", mac);
            obj.put("major", major);
            obj.put("minor", minor);

        } catch (JSONException ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        
        return obj;
    }
    
    
    /**
     * 앱에서 받은 데이터를 BleData 변환
     * @param jArr
     * @return 
     */
    public static ArrayList<BleData> convertFromJSONArray(JSONArray jArr){
        ArrayList<BleData> list = new ArrayList<BleData>();
        
        for(int i=0 ; i< jArr.length() ; i++){
            try {
                JSONArray arr = jArr.getJSONArray(i);
                for(int j=0; j < arr.length() ; j++){
                    BleData bleObj = new BleData();  
                    JSONObject jObj = arr.getJSONObject(j);

                    bleObj.mac = UtilString.convertMacString(jObj.getString("mac")).toUpperCase();
                    bleObj.uuid = jObj.getString("uuid");
                    bleObj.rssi = jObj.getInt("rssi");
                    bleObj.time = DateTimeUtils.convertTimestampToDate(jObj.getLong("time"));
                    bleObj.major = jObj.getString("major").equals("") ? 0 : Integer.parseInt(jObj.getString("major"));
                    bleObj.minor = jObj.getString("minor").equals("") ? 0 : Integer.parseInt(jObj.getString("minor"));

                    list.add(bleObj);
                    
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }

        return list;
    }
    
    
    
    
}
