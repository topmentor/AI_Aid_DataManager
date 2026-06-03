/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sox.ltex.model;

import com.ithows.AppConfig;

/**
 * 그리드 시스템 공통 상수.
 * configplatform.xml 에서 초기값을 로드하며, 없을 경우 하드코딩 기본값을 사용합니다.
 *
 * configplatform.xml 키:
 *   grid_org_min_x  (기본: 124.54117)
 *   grid_org_min_y  (기본: 32.928463)
 *   grid_org_max_x  (기본: 130.57113)
 *   grid_org_max_y  (기본: 42.344405)
 *   grid_offset_5m_x (기본: 0.0000555)
 *   grid_offset_5m_y (기본: 0.0000460)
 */
public class primeConst {

    public static double orgMinX    = 124.54117;
    public static double orgMinY    = 32.928463;
    public static double orgMaxX    = 130.57113;
    public static double orgMaxY    = 42.344405;
    public static double OFFSET_5M_X = 0.0000555;
    public static double OFFSET_5M_Y = 0.0000460;
    
    public static String nationCode = "kr";
    
    

//    static {
//        try {
//            if (AppConfig.has("grid_org_min_x"))  orgMinX    = Double.parseDouble(AppConfig.getConf("grid_org_min_x"));
//            if (AppConfig.has("grid_org_min_y"))  orgMinY    = Double.parseDouble(AppConfig.getConf("grid_org_min_y"));
//            if (AppConfig.has("grid_org_max_x"))  orgMaxX    = Double.parseDouble(AppConfig.getConf("grid_org_max_x"));
//            if (AppConfig.has("grid_org_max_y"))  orgMaxY    = Double.parseDouble(AppConfig.getConf("grid_org_max_y"));
//            if (AppConfig.has("grid_offset_5m_x")) OFFSET_5M_X = Double.parseDouble(AppConfig.getConf("grid_offset_5m_x"));
//            if (AppConfig.has("grid_offset_5m_y")) OFFSET_5M_Y = Double.parseDouble(AppConfig.getConf("grid_offset_5m_y"));
//        } catch (Exception e) {
//            System.out.println("[primeConst] configplatform.xml 로드 실패, 기본값 사용: " + e.getMessage());
//        }
//    }
//    
    
    static {
        int mcc = AppConfig.getConfInt("mcc");
        if (mcc < 0) {
            mcc = 440; // configplatform.xml에 mcc 항목이 없거나 파싱 실패 시 한국 기본값
        }

        if (mcc == 440 || mcc == 441) {
            // 일본 extent
            orgMinX = 122.93;
            orgMinY = 20.23;
            orgMaxX = 154.00;
            orgMaxY = 45.52;
            nationCode = "jp";
        } else {
            // 대한민국 extent (MNC 450, 기본값)
            orgMinX = 124.54117;
            orgMinY = 32.928463;
            orgMaxX = 130.57113;
            orgMaxY = 42.344405;
            nationCode = "kr";
        }
        
        System.out.println("mcc = " + mcc );
        System.out.println("orgMinX = " + orgMinX );
        System.out.println("orgMinY = " + orgMinY );
        System.out.println("orgMaxX = " + orgMaxX );
        System.out.println("orgMaxY = " + orgMaxY );
    }
    
}
