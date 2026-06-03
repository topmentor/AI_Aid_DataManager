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
import java.util.Comparator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Eid, Time, ,M System(7-LTE, 20-WLAN), 0, Count , parameter count, Cell type(0-neighboring, 1-serving), band, quality, channel, rssi, ssid, mac, security, link speed, ip 
 * CELLMEAS,14:36:09.333,,20,0,2,10,0,200001,,6,-61,"janus_bb_gw200_231A9A","00:e1:40:23:1a:9a",6,,,0,200001,,8,-80,"UR10_2_4GHz","88:36:6c:d6:2a:94",6,,
 */
public class WiFiData implements Comparable<WiFiData>{
    public String time = "";
    public int sigType = 0;
    public int cellType = 0;
    public int band = 0;
    public double quality = 0;
    public int channel = 0;
    public double rssi = -100;
    public String ssid = "";
    public String mac = "";
    public int security = 0;
    public int linkspeed = 0;
    public String ip = "";
    
    public void print(PrintStream out){
        out.println("   time : " + time );
        out.println("   cellType : " + cellType );
        out.println("   band : " + band );
        out.println(String.format("   quality : %.2f", quality));
        out.println("   channel : " + channel );
        out.println(String.format("   rssi : %.1f", rssi));
        out.println("   ssid : " + ssid );
        out.println("   mac : " + mac );
        out.println("   security : " + security );
        out.println("   linkspeed : " + linkspeed );
        out.println("   ip : " + ip );
    }
    
    
    public JSONObject getJSON(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("time", time);
            obj.put("cellType", cellType);
            obj.put("band", band);
            obj.put("quality", quality);
            obj.put("channel", channel);
            obj.put("rssi", rssi);
            obj.put("ssid", ssid);
            obj.put("mac", mac);
            obj.put("security", security);
            obj.put("linkspeed", linkspeed);
            obj.put("ip", ip);

        } catch (JSONException ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        
        return obj;
    }
    
    public static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
        return -1;
        }
    }
    
    
    /**
     * 앱에서 받은 데이터를 WiFiData 변환
     * @param jArr
     * @return 
     */
    public static ArrayList<WiFiData> convertFromJSONArray(JSONArray jArr){
        ArrayList<WiFiData> list = new ArrayList<WiFiData>();
        
        for(int i=0 ; i< jArr.length() ; i++){
            try {
                JSONArray arr = jArr.getJSONArray(i);
                for(int j=0; j < arr.length() ; j++){
                    WiFiData wifiObj = new WiFiData();  
                    JSONObject jObj = arr.getJSONObject(j);

                    wifiObj.band = jObj.getInt("band");
                    wifiObj.mac = UtilString.convertMacString(jObj.getString("mac"));
                    wifiObj.ssid = jObj.getString("ssid");
                    wifiObj.rssi = jObj.getDouble("rssi");
                    wifiObj.time = DateTimeUtils.convertTimestampToDate(jObj.getLong("time"));

                    list.add(wifiObj);
                    
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }

        return list;
    }            
    
    public int compareTo(WiFiData other) {
        // 먼저 rssi 크기로 비교
        int i = Double.valueOf(this.rssi).compareTo(Double.valueOf(other.rssi));
        return i;

//        if (i != 0) return i;
//
//        // mac 크기로 비교 
//        i = this.mac.compareTo(other.mac);
//        return i;

    }

}
