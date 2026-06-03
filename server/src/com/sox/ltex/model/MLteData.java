/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.model;

import java.io.PrintStream;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * MIMOMEAS,time, ,M System(7-LTE, 20-WLAN),0, Count, param count,band,channel,pci,port,celltype(0-serving),rssi,rsrq,rsrp,timing 
 * MIMOMEAS,15:27:16.217,,7,0,8,9,70003,1550,318,100,0,-49.0,-6.0,-76.0,,70003,1550,318,101,0,,-6.0,,,70003,1550,318,102,0,,,,,70003,1550,318,103,0,-43.0,-11.0,-73.0,,70003,1550,318,110,0,-49.0,-6.0,-76.0,,70003,1550,318,111,0,,-6.0,,,70003,1550,318,112,0,,,,,70003,1550,318,113,0,-43.0,-11.0,-67.0,
 */
public class MLteData {
    public String time = "";
    public int sigType = 2;
    public int band = 0;
    public int channel = 0;
    public int pci = 0;
    public int port = 0;
    public int cellType = 0;
    public double rssi = 0;
    public double rsrq = 0;
    public double rsrp = 0;
    public int timing = 0;

    
    public void print(PrintStream out){
        out.println("time : " + time );
        out.println("band : " + band );
        out.println("channel : " + channel );
        out.println("pci : " + pci );
        out.println("port : " + port );
        out.println("cellType : " + cellType );
        out.println(String.format("rssi : %.7f", rssi));
        out.println(String.format("rsrq : %.7f", rsrq));
        out.println(String.format("rsrp : %.7f", rsrp));
        out.println("timing : " + timing );
    }
    
     public JSONObject getJSON(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("time", time);
            obj.put("cellType", cellType);
            obj.put("band", band);
            obj.put("channel", channel);
            obj.put("pci", pci);
            obj.put("port", port);
            obj.put("rssi", rssi);
            obj.put("rsrq", rsrq);
            obj.put("rsrp", rsrp);
            obj.put("timing", timing);

        } catch (JSONException ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        
        return obj;
    }
}
