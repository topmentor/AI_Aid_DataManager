/*
 *  Copyright 2020. S.O.X. All rights reserved
 */

package com.sox.ltex.model.shape;

import com.sox.ltex.util.CoordTransformUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class GPSCoord
 * 
 * @author Roi Kim <S.O.X Co. Ltd.>
 */
public class GPSCoord {
    public String time = "";
    public double latitude = 0;
    public double longtitude = 0;
    public int height = 0;
    public int distance = 0;
    public double proj_x = 0;   // UTM계산이 디폴트
    public double proj_y = 0;   // UTM계산이 디폴트
    
    public GPSCoord(){
        
    }
    
    public GPSCoord(GPoint pt){
        longtitude = pt.x;
        latitude = pt.y;
        getUTMCoord();
        
    }
    
    public GPSCoord(double x, double y){
        longtitude = x;
        latitude = y;
        getUTMCoord();
        
    }
    
    public GPoint getUTMCoord(){
        GPoint pt = new GPoint();
        if(latitude == 0 || longtitude == 0)
            return pt;
        
        pt = CoordTransformUtil.WGS84toUTM(new GPoint(longtitude, latitude));
        
        proj_x = pt.x;
        proj_y = pt.y;
        return pt;
    }
    
    public GPoint getDaumCoord(){
        GPoint pt = new GPoint();
        if(latitude == 0 || longtitude == 0)
            return pt;
        
        pt = CoordTransformUtil.WGS84toDaum(new GPoint(longtitude, latitude));
        
        proj_x = pt.x;
        proj_y = pt.y;
        return pt;
    }
    

    public GPoint getNaverCoord(){
        GPoint pt = new GPoint();
        if(latitude == 0 || longtitude == 0)
            return pt;
        
        pt = CoordTransformUtil.WGS84toNaver(new GPoint(longtitude, latitude));
        
        proj_x = pt.x;
        proj_y = pt.y;
        return pt;
    }
    
    public GPoint getGoogleOSMCoord(){
        GPoint pt = new GPoint();
        if(latitude == 0 || longtitude == 0)
            return pt;
        
        pt = CoordTransformUtil.WGS84toGoogle(new GPoint(longtitude, latitude));
        
        proj_x = pt.x;
        proj_y = pt.y;
        return pt;
    }
    
    
    public void print(){
        System.out.println("time : " + time );
        System.out.println(String.format("longtitude : %.7f", longtitude));
        System.out.println(String.format("latitude : %.7f", latitude));
        System.out.println(String.format("utm_x : %.7f", proj_x));
        System.out.println(String.format("utm_y : %.7f", proj_y));
        System.out.println("height : " + height );
        System.out.println("distance : " + distance );
    }
    
    public JSONObject getJSON(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("time", time);
            obj.put("longtitude", longtitude);
            obj.put("latitude", latitude);
            obj.put("utm_x", proj_x);
            obj.put("utm_y", proj_y);
            obj.put("height", height);
            obj.put("distance", distance);

        } catch (JSONException ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        
        return obj;
    }
    
    public String toString() {
	return this.toString(false);
    }
    
    public String toString(boolean reverse) {
            if(reverse)
		return "[" + longtitude + " , " + latitude + "]";
            else
		return "[" + latitude + " , " + longtitude + "]";
                
   }
}
