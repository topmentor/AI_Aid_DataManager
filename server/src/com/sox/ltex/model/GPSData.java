/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.model;

import com.sox.ltex.util.CoordTransformUtil;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.StringTokenizer;
import com.sox.ltex.model.shape.GPoint;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Eid, time, ,longtitude, latitude, height, distance, quality, satelites, velocity,,,,status,be_alt
 * GPS,15:27:15.943,,127.367912,36.379753,91,0,1,6,0,,,,
 */
public class GPSData implements Cloneable {
    public String time = "";
    public double latitude = 0;    // 위도 - y
    public double longtitude = 0;  // 경도 - x
    public int height = 0;
    public int distance = 0;
    public int quality = 0;
    public int satelites = 0;
    public int velocity = 0;
    public int status = 0;
    public double utm_x = 0;
    public double utm_y = 0;
    
    public boolean isNull(){
        if(latitude == 0 && longtitude == 0)
            return true;
        
        return false;
    }
    
    public GPoint getData(){
        return new GPoint(longtitude, latitude);
    }
    
    public GPoint getUTMData(){
        return new GPoint(utm_x, utm_y);
    }
    
    public void setData(double lng, double lat){
        latitude = lat;
        longtitude = lng;
        
        GPoint pt = CoordTransformUtil.WGS84toUTM(new GPoint(longtitude, latitude));
        
        utm_x = pt.x;
        utm_y = pt.y;
    }
    
    public void setUTMCoord(){
        if(latitude == 0 || longtitude == 0)
            return;
        
        GPoint pt = CoordTransformUtil.WGS84toUTM(new GPoint(longtitude, latitude));
        
        utm_x = pt.x;
        utm_y = pt.y;
    }
    
    // Eid, time, ,longtitude, latitude, height, distance, quality, satelites, velocity,,,,status,be_alt
    public void parse(String line) {
//        System.out.println("line : " + line);
        String[] elements = line.split(",");
        time = elements[1];
        
        if(!elements[3].equals("")){
           longtitude = Double.parseDouble(elements[3]);
        }
        if(!elements[4].equals("")){
            latitude = Double.parseDouble(elements[4]);
        }
        
        if(!elements[5].equals("")){
            height = Integer.parseInt(elements[5]);
        }
        
        if(!elements[6].equals("")){
            distance = Integer.parseInt(elements[6]);
        }
        
        if(!elements[7].equals("")){
            
            quality = Integer.parseInt(elements[7]);
        }
        
        if(!elements[8].equals("")){
            satelites = Integer.parseInt(elements[8]);
            
        }
        
        if(!elements[9].equals("")){
            velocity = Integer.parseInt(elements[9]);
            
        }
        
        if(!elements[13].equals("")){
            status = Integer.parseInt(elements[13]);
            
        }
        
        setUTMCoord();
        
    }
    
    public void print(PrintStream out, int cnt){
        out.println("GPS [" + cnt + "] ----------------- "  + time);
        out.println(String.format("   longtitude : %.7f", longtitude));
        out.println(String.format("   latitude : %.7f", latitude));
        out.println(String.format("   utm_x : %.7f", utm_x));
        out.println(String.format("   utm_y : %.7f", utm_y));
        out.println("   height : " + height );
        out.println("   distance : " + distance );
        out.println("   quality : " + quality );
        out.println("   satelites : " + satelites );
        out.println("   velocity : " + velocity );
        out.println("   status : " + status );
    }
    
    public JSONObject getJSON(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("time", time);
            obj.put("longtitude", longtitude);
            obj.put("latitude", latitude);
            obj.put("utm_x", utm_x);
            obj.put("utm_y", utm_y);
            obj.put("height", height);
            obj.put("distance", distance);
            obj.put("quality", quality);
            obj.put("satelites", satelites);
            obj.put("velocity", velocity);
            obj.put("status", status);

        } catch (JSONException ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        
        return obj;
    }
    
    public static GPSData getAveragePoint(ArrayList<GPSData> gpsList){
        double lat = 0;
        double lng = 0;
        
        GPSData target = new  GPSData();
        
        for(GPSData obj : gpsList){
            lat += obj.latitude;
            lng += obj.longtitude;
        }
        lat = Math.round(lat/gpsList.size()*10) / 10.0;
        lng = Math.round(lng/gpsList.size()*10) / 10.0;
        
        target.setData(lng,lat);
        return target;
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException{
    	return (GPSData)super.clone();
    }
}
