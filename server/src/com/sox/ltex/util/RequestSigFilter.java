/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sox.ltex.util;

import com.ithows.util.UtilString;
import java.util.ArrayList;

/**
 *
 * @author mailt
 */
public class RequestSigFilter {
 
    private static ArrayList<String> filterMacList = null; 
    
    
    public static void main(String[] args) {
        setMacList("ff:ff:ff,00:00:00");
//        String maclist = excludeMac("ff:ff:ff:ee:ee:ee,00:00:00:ff:ff:ff");
//        System.out.println("maclist = " + maclist);
        String[] maclist = excludeMac("ff:ff:ff:ee:ee:ee,00:00:ff:ff:ff:ff,00:00:00:aa:aa:aa,11:22:33:44:55:66", "-10,-20,-30,-40");
        System.out.println("maclist = " + maclist[0]);
        System.out.println("rssilist = " + maclist[1]);
    }
    
    public static int setMacList(String maccsv){
        
        if(maccsv == null ||  maccsv.isEmpty()){
            return 0;
        }
            
        maccsv = UtilString.removeNullCsv(maccsv, ",");
        filterMacList = UtilString.parseListCSV(maccsv, ",");
        
        return (filterMacList != null ? filterMacList.size() : 0) ;
    }
    
    public static String excludeMac(String wifimac){
        
        if(wifimac.isEmpty()){
            return "";
        }

        String result = "";
        StringBuffer buf = new StringBuffer();
        wifimac = UtilString.removeNullCsv(wifimac, ",");
        ArrayList<String> wifiList = UtilString.parseListCSV(wifimac, ",");
        
        for(String wmac : wifiList){
            if(containFilterMac(wmac) == false){
                buf.append(wmac);
                buf.append(",");
            }
        }
        
        if(buf.length() > 0){
            result = UtilString.trimString(buf.toString(), 1, false);
        }
        
        return result;
    }
    
    public static String[] excludeMac(String wifimac, String wifirssi){
        
        if(wifimac.isEmpty() || wifirssi.isEmpty()){
            return new String[2];
        }

        String[] result = new String[2];
        StringBuffer buf = new StringBuffer();
        StringBuffer buf2 = new StringBuffer();
        wifimac = UtilString.removeNullCsv(wifimac, ",");
        wifirssi = UtilString.removeNullCsv(wifirssi, ",");
        ArrayList<String> wifiList = UtilString.parseListCSV(wifimac, ",");
        ArrayList<String> rssiList = UtilString.parseListCSV(wifirssi, ",");
        
        for(int i=0; i< wifiList.size() ; i++){
            String wmac = wifiList.get(i);
            String rssi = rssiList.get(i);
            if(containFilterMac(wmac) == false){
                buf.append(wmac);
                buf.append(",");
                buf2.append(rssi);
                buf2.append(",");
            }
        }
        
        if(buf.length() > 0){
            result[0] = UtilString.trimString(buf.toString(), 1, false);
        }
        if(buf2.length() > 0){
            result[1] = UtilString.trimString(buf2.toString(), 1, false);
        }
        
        return result;
    }
    
    
    // 시작 부분이 같으면 필터 Mac에 속하는 것으로 간주
    private static boolean containFilterMac(String macItem){
        
        if(filterMacList == null || filterMacList.size() == 0){
            return false;
        }
        
        macItem = macItem.toLowerCase();
        for(String fmac : filterMacList){
            fmac = fmac.toLowerCase();
            if(macItem.startsWith(fmac)){
                return true;
            }
        }
        
        return false;
    }
       
}
