/*
 *  Copyright 2020. S.O.X. All rights reserved
 */

package com.sox.ltex.model;

import com.ithows.JdbcDao;
import com.ithows.JdbcDao2;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class NMFCellID
 * 
 * @author Roi Kim <S.O.X Co. Ltd.>
 */
public class StationData implements Cloneable{

    int mcc = 0;
    int mnc = 0;
    int band = 0;
    int pci = 0;
    int channel = 0;
    int ECIInt = 0;
    String ECIStr = "";
    String eNB_ID = "";
    int eNBInt = 0;
    String Sector_ID = "";
    int SectorInt = 0;
    
    double latitude;
    double longtitude;
    double utm_x;
    double utm_y;
    
    public StationData(){
        
    }
    public StationData(int num, int new_mcc, int new_mnc, int new_band, int new_pci, int new_channel){
        parsing(num, new_mcc, new_mnc, new_band, new_pci, new_channel);
    }
    
    public void setData(int num, int new_mcc, int new_mnc, int new_band, int new_pci, int new_channel){
        parsing(num, new_mcc, new_mnc, new_band, new_pci, new_channel);
    }
    
    public void setGPS(GPSData gps){
        latitude = gps.latitude;
        longtitude = gps.longtitude;
        utm_x = gps.utm_x;
        utm_y = gps.utm_y;
    }
    
    public void parsing(int num, int new_mcc, int new_mnc, int new_band, int new_pci, int new_channel){
        mnc = new_mnc;
        mcc = new_mcc;
        band = new_band;
        pci = new_pci;
        channel = new_channel;
        ECIInt = num;
        ECIStr = Integer.toHexString(ECIInt);
        eNB_ID = ECIStr.substring(0, ECIStr.length()-2);
        eNBInt = Integer.parseInt(eNB_ID, 16);
        Sector_ID = ECIStr.substring(ECIStr.length()-2, ECIStr.length());
        SectorInt = Integer.parseInt(Sector_ID, 16);

         return ;
    }
    
    
    public JSONObject getJSon(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("mnc", mnc);
            obj.put("mcc", mcc);
            obj.put("band", band);
            obj.put("pci", pci);
            obj.put("channel", channel);
            obj.put("latitude", latitude);
            obj.put("longtitude", longtitude);
            obj.put("utm_x", utm_x);
            obj.put("utm_y", utm_y);
            obj.put("ECI", ECIInt);
            obj.put("ECIHex", ECIStr);
            obj.put("eNBHex", eNB_ID);
            obj.put("eNB", eNBInt);
            obj.put("SectorHex", Sector_ID);
            obj.put("Sector", SectorInt);
        } catch (JSONException ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        return obj;
    }
    
    public void print(PrintStream out){
        
        out.println("mnc = " + mnc);
        out.println("mcc = " + mcc);
        out.println("band = " + band);
        out.println("pci = " + pci);
        out.println("channel = " + channel);
        out.println("latitude = " + latitude);
        out.println("longtitude = " + longtitude);
        out.println("utm_x = " + utm_x);
        out.println("utm_y = " + utm_y);
        out.println("ECI = " + ECIInt);
        out.println("ECIHex = " + ECIStr);
        out.println("eNB_Hex = " + eNB_ID);
        out.println("Sector_Hex = " + Sector_ID);
        out.println("eNB = " + eNBInt);
        out.println("Sector = " + SectorInt);
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException{
    	return super.clone();
    }
    
    
    public static void setGPSAll(HashMap<String, StationData> list, GPSData gps){
        for (String key : list.keySet()) {
            StationData obj = list.get(key);
            obj.setGPS(gps);
        }
    }
    
    public static boolean insertDB(HashMap<String, StationData> list){
        
       String SQL = "insert into " + 
                     "station (latitude, longtitude, utm_x, utm_y, " + 
                     "mcc, mnc, band, pci, channel, " +
                     "eci, eNB, sector, " +
                     "registerTime ) values ";
    
        int i=0;
        int sCount = 0;


        try{

            for (String key : list.keySet()) {
                StationData obj = list.get(key);

                String query = "SELECT count(*) FROM station WHERE band=? and pci=? and channel=? ;";
                int l_count = JdbcDao2.queryForInt(query, new Object[]{obj.band, obj.pci, obj.channel});
        
                // @@ Station DB insert 정책 : 좌표값이 있어야 추가 
                if(obj !=null && obj.latitude > 0 && obj.longtitude > 0 && l_count==0){
                    SQL += "(" + obj.latitude + ", " + obj.longtitude + ", " + obj.utm_x + ", " + obj.utm_y + ", "  
                            + obj.mcc + ", " + obj.mnc + ", " + obj.band + ", " + obj.pci + ", " + obj.channel + ", " 
                            + obj.ECIInt + ", " + obj.eNBInt + ", " + obj.SectorInt + ", "  
                            + "now() ), ";

                    sCount++;
                }

            }

            SQL = SQL.substring(0, SQL.length()-2);
            SQL += ";";
            
//            System.out.println("query: " + SQL);
            
            if(sCount > 0){   
                JdbcDao2.update(SQL);
            }

        }catch(Exception ex){
            ex.printStackTrace();
            System.out.println(ex.getLocalizedMessage());
            return false;
        }
        
        return true;
    }
    
}
