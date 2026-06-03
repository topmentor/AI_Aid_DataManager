/*
 *  Copyright 2020. S.O.X. All rights reserved
 */

package com.sox.ltex.model.shape;

import com.ithows.ResultMap;
import com.sox.ltex.model.primeConst;
import org.json.JSONObject;


/**
 * Class GIndex
 * 
 * @author Roi Kim <S.O.X Co. Ltd.>
 */
public class GIndex implements Comparable, Cloneable{
    
    // 상수는 primeConst(→ configplatform.xml) 에서 관리합니다.
    public static double orgMinX    = primeConst.orgMinX;
    public static double orgMinY    = primeConst.orgMinY;
    public static double orgMaxX    = primeConst.orgMaxX;
    public static double orgMaxY    = primeConst.orgMaxY;
    public static double OFFSET_5M_X = primeConst.OFFSET_5M_X;
    public static double OFFSET_5M_Y = primeConst.OFFSET_5M_Y;
    public final static int ORIGIN_LEVEL_UNIT = 5;       // @@ 현재 기준 레벨이 5 (임시적) 
    
    
    public boolean isUnitLevel = true;
    public long xId = 0;
    public long yId = 0;
    public int level = 1;   // @@ level은 그리드 셀의 크기를 좌우하며 25미터이면 level은 5가 된다 
    public MBR extent = new MBR();

    public int subCellCount = 0;
    public String subCellIds = "";
    

    public GIndex(long xId, long yId, int level) {
        this.xId = xId;
        this.yId = yId;
        this.level = level;
        if(this.level > ORIGIN_LEVEL_UNIT){
            isUnitLevel = false;
        }
        calculateExtent();
    }

    public GIndex(double lnt, double lat, int level) {
        this.xId = (long)((lnt - orgMinX) / (OFFSET_5M_X * level)) + 1 ;
        this.yId = (long)((lat - orgMinY) / (OFFSET_5M_Y * level)) + 1 ;
        this.level = level;
        if(this.level > ORIGIN_LEVEL_UNIT){
            isUnitLevel = false;
        }
        calculateExtent();
    }
    
    public GIndex(GPoint pt, int level) {
        this(pt.x, pt.y, level);
    }

    public GIndex() {
    }
    

    public GIndex(GIndex pt) {
        this.xId = pt.xId;
        this.yId = pt.yId;
        this.level = pt.level;
        if(this.level > ORIGIN_LEVEL_UNIT){
            isUnitLevel = false;
        }
        calculateExtent();
    }

    public GIndex copy() {

        return new GIndex(this);
    }

    public long[] getId(){
        long[] idNum = new long[2];
        idNum[0] = xId;
        idNum[1] = yId;
        return idNum;
    }
    
    public MBR getExtent(){
        return extent;
    }
    
    public void calculateExtent(){
        extent.minX = orgMinX + ((xId-1) * OFFSET_5M_X * level);
        extent.minY = orgMinY + ((yId-1) * OFFSET_5M_Y * level);
        extent.maxX = extent.minX + (OFFSET_5M_X * level);
        extent.maxY = extent.minY + (OFFSET_5M_Y * level);
    }
    
    public GPoint getCenterPoint(){
        GPoint centPt = new GPoint();
        centPt.x = (extent.maxX + extent.minX) / 2;
        centPt.y = (extent.maxY + extent.minY) / 2;
        return centPt;
    }
    
    public double getCenterX(){
        double x = (extent.maxX + extent.minX) / 2;
        return x;
    }
    
    public double getCenterY(){
        double x = (extent.maxY + extent.minY) / 2;
        return x;
    }
    
    public double getMinX(){
        return extent.minX;
    }
    
    public double getMinY(){
        return extent.minY;
    }
    
    public double getMaxX(){
        return extent.maxX;
    }
    
    public double getMaxY(){
        return extent.maxY;
    }
    
    

    public GPoint getBasePoint(){
        GPoint basePt = new GPoint();
        basePt.x = extent.minX;
        basePt.y = extent.minY;
        return basePt;
    }

    public boolean equal(GIndex pt) {
        return xId == pt.xId && yId == pt.yId && level == pt.level ;
    }

    public boolean equals(Object other) {
        if (!(other instanceof GIndex)) {
            return false;
        }
        GIndex otherIndex = (GIndex) other;
        return (     xId == otherIndex.xId
                &&   yId == otherIndex.yId 
                &&   level == otherIndex.level );
    }

    public int compareTo(Object o) {
        GIndex other = (GIndex) o;
        if (xId < other.xId) {
            return -1;
        }
        if (xId > other.xId) {
            return 1;
        }
        if (yId < other.yId) {
            return -1;
        }
        if (yId > other.yId) {
            return 1;
        }
        return 0;
    }

    
    public String getExtentJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("minX", extent.minX);
            obj.put("minY", extent.minY);
            obj.put("maxX", extent.maxX);
            obj.put("maxY", extent.maxY);
            GPoint center = getCenterPoint();
            obj.put("ox", center.x);
            obj.put("oy", center.y);
            
        } catch (Exception e) {
            return "";
        }
        return obj.toString();
    }
    
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append('(');
        result.append(xId);
        result.append(',');
        result.append(yId);
        result.append(',');
        result.append(level);
        result.append(',');
        result.append(extent);
        result.append(')');

        return result.toString();
    }
    
    public ResultMap toMap() {
        ResultMap map = new ResultMap();
        
        map.put("xId", xId);
        map.put("yId", yId);
        map.put("centerX", getCenterX());
        map.put("centerY", getCenterY());
        map.put("minX", getMinX());
        map.put("minY", getMinY());
        map.put("maxX", getMaxX());
        map.put("maxY", getMaxY());
        map.put("cellCount", subCellCount);
        map.put("noneCellCount", innerCellCount(this.level) - subCellCount);
        map.put("cellIds", subCellIds);
        

        return map;
    }

    // 좌표를 받으면 해당하는 그리드 셀의 아이디를 알려 줌
    public static long[] findIndex(double lnt, double lat, int level) {
        long xId = (long)((lnt - orgMinX) / (OFFSET_5M_X * level)) + 1 ;
        long yId = (long)((lat - orgMinY) / (OFFSET_5M_Y * level)) + 1 ;
                
        long[] result = new long[2];
        result[0] = xId;
        result[1] = yId;
        
        return result;
    }
    
    // @@ 2023
    // @@ 서브 셀에 대한 수를 리턴 (의존성이 있음)
    public int innerCellCount(){
        return (this.level / ORIGIN_LEVEL_UNIT) * (this.level / ORIGIN_LEVEL_UNIT) ;
    }
    
    public static int innerCellCount(int level){
        return (level / ORIGIN_LEVEL_UNIT) * (level / ORIGIN_LEVEL_UNIT) ;
    }
}
