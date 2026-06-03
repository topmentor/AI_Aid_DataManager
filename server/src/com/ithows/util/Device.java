/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

/**
 *
 * @author dreamct
 */
public class Device {
    
    
    public static final int PC = 11;
    public static final int MOBILE = 12;
    public static final int TABLET = 13;
    
    public static final int manualManager_WEB_PC = 0;
    public static final int manualManager_WEB_MOBILE = 1;
    public static final int manualManager_WEB_TABLET = 7;
    
    public static final int workerGPS_WEB_MOBILE = 2;
    public static final int workerGPS_WEB_PC = 8;
    public static final int workerGPS_WEB_TABLET = 9;
    
    public static final int autoManager_App_NATIVEPC = 3;
    
    public static final int autoManager_App_PC = 4;
    public static final int autoManager_App_MOBILE = 5;
    public static final int autoManager_App_TABLET = 6;

    public static int getGPSDeviceType(String userAgent) {
        String uAgent = userAgent.toLowerCase();
        int result = Device.workerGPS_WEB_PC;;        
        int type = Device.getDeviceType(userAgent);
        if(type==Device.PC){
            result = Device.workerGPS_WEB_PC;
        }else if(type==Device.MOBILE){
            result = Device.workerGPS_WEB_MOBILE;
        }else if(type==Device.TABLET){
            result = Device.workerGPS_WEB_TABLET;
        }
        return result;
    }
    
    public static int getWebDeviceType(String userAgent) {
        String uAgent = userAgent.toLowerCase();
        int result = Device.manualManager_WEB_PC;;        
        int type = Device.getDeviceType(userAgent);
        if(type==Device.PC){
            result = Device.manualManager_WEB_PC;
        }else if(type==Device.MOBILE){
            result = Device.manualManager_WEB_MOBILE;
        }else if(type==Device.TABLET){
            result = Device.manualManager_WEB_TABLET;
        }
        return result;
        
        
    }

    public static int getAppDeviceType(String userAgent) {
        String uAgent = userAgent.toLowerCase();
        int result = Device.autoManager_App_PC;;
        
        int type = Device.getDeviceType(userAgent);
        if(type==Device.PC){
            result = Device.autoManager_App_PC;
        }else if(type==Device.MOBILE){
            result = Device.autoManager_App_MOBILE;
        }else if(type==Device.TABLET){
            result = Device.autoManager_App_TABLET;
        }
        return result;
    }
    
    public static int getDeviceType(String userAgent) {
        int type = 0;
        String uAgent = userAgent.toLowerCase();
        if (uAgent.indexOf("android") != -1) {
            if (uAgent.indexOf("mobile") != -1) {
                type = Device.MOBILE; //모바일 4
            } else {
                type = Device.TABLET; // 테블릿 5
            }
        } else if (uAgent.indexOf("ipad") != -1) {
            type = Device.TABLET; // 테블릿 5
        } else if (uAgent.indexOf("ipod") != -1 || uAgent.indexOf("iphone") != -1) {
            type = Device.MOBILE; //모바일 4
        } else if (uAgent.indexOf("phone") != -1) {
            type = Device.MOBILE; //모바일 4
        } else {
            type = Device.PC; //PC 3
        }
        return type;
    }
}