/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sox.ltex.util;


/**
 * Class JTSBasicOperator
 * 
 * @author Roi Kim <S.O.X Co. Ltd.>
 */
import com.sox.ltex.model.ShapeType;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import com.sox.ltex.model.shape.GPoint;
import com.sox.ltex.model.shape.MBR;

public class JTSBasicOperator {

    private static PrecisionModel precisionModel = new PrecisionModel(1000);
    private static GeometryFactory geometryFactory = new GeometryFactory(precisionModel, 0);


    public static void main(String[] args){



    }




    ////////////////////////////////////////////////////////////////////
    // CCW (시계방향 - 오른쪽 방향) 확인
    public static boolean isCCW(GPoint[] points){
        Coordinate[] pts = convertToCoordinates(points);
        return Orientation.isCCW(pts);
    }
    public static boolean isCCW(ArrayList<GPoint> points){
        Coordinate[] pts = convertToCoordinates(points);
        return Orientation.isCCW(pts);
    }


    //////////////////////////////////////////////////////////////////////////
    // Geo Data Conversion (to JTSObject)

    public static  Geometry convertToGeometry(ArrayList<GPoint> points, int geoType){
        Coordinate[] pts = JTSBasicOperator.convertToCoordinates(points);

        Geometry actualGeometry = null;

        if(geoType == ShapeType.Point  || (geoType == ShapeType.PointZ) || (geoType == ShapeType.PointM) || (geoType == ShapeType.HeatmapPoint) )  {
            actualGeometry = geometryFactory.createPoint(pts[0]);
        }else if((geoType == ShapeType.MultiPoint) || (geoType == ShapeType.MultiPointZ) || (geoType == ShapeType.MultiPointM) ){
            actualGeometry = geometryFactory.createMultiPoint(pts);
        }else if((geoType == ShapeType.PolyLine) || (geoType == ShapeType.PolyLineZ) || (geoType == ShapeType.PolyLineM) ){
            actualGeometry = geometryFactory.createLineString(pts);
        }else if((geoType == ShapeType.Polygon) || (geoType == ShapeType.PolygonZ) || (geoType == ShapeType.PolygonM) || (geoType == ShapeType.Ring) ){
            actualGeometry = geometryFactory.createPolygon(pts);
        }

        return actualGeometry;
    }

    //////////////////////////////////////////////////////////////////////////
    // Geo Data Conversion (to ArrayList<GPoint>)

    public static  ArrayList<GPoint> convertToArrayList(Geometry geomObject, int geoType){
        ArrayList<GPoint> pointList = null;

        if(geoType == ShapeType.Point  || (geoType == ShapeType.PointZ) || (geoType == ShapeType.PointM) || (geoType == ShapeType.HeatmapPoint) )  {
            pointList = new ArrayList<GPoint>();
            pointList.add(convertToGPoint(geomObject.getCoordinates()[0]));
        }else if((geoType == ShapeType.MultiPoint) || (geoType == ShapeType.MultiPointZ) || (geoType == ShapeType.MultiPointM) ){
            pointList =  convertToPoly(geomObject.getCoordinates());
        }else if((geoType == ShapeType.PolyLine) || (geoType == ShapeType.PolyLineZ) || (geoType == ShapeType.PolyLineM) ){
            pointList =  convertToPoly(geomObject.getCoordinates());
        }else if((geoType == ShapeType.Polygon) || (geoType == ShapeType.PolygonZ) || (geoType == ShapeType.PolygonM) || (geoType == ShapeType.Ring) ){
            pointList =  convertToPoly(geomObject.getCoordinates());
        }

        return pointList;
    }



    public static ArrayList<GPoint> convertToArrayList(GPoint[] points){
        ArrayList<GPoint> pts = new ArrayList<GPoint>();

        for(int i=0; i<points.length ; i++){
            pts.add(points[i]);
        }
        return pts;
    }

    public  static GPoint convertToGPoint(Coordinate pt){
        return new GPoint(pt.x, pt.y);
    }

    public  static GPoint[] convertToGPoints(Coordinate[] points){
        GPoint[] pts = new GPoint[points.length];

        for(int i=0; i<points.length ; i++){
            pts[i] = convertToGPoint(points[i]);
        }

        return pts;
    }

    public  static ArrayList<GPoint> convertToPoly(Coordinate[] points){
        ArrayList<GPoint> pts = new ArrayList<GPoint>();

        for(int i=0; i<points.length ; i++){
            pts.add(convertToGPoint(points[i]));
        }
        return pts;
    }

    public  static Coordinate[] convertToCoordinates(GPoint[] points){
        Coordinate[] pts = null;

        convertToCoordinates(  new ArrayList<GPoint>(Arrays.asList(points))  );

        return pts;
    }

    public  static Coordinate[] convertToCoordinates(ArrayList<GPoint> points){
        Coordinate[] pts = new Coordinate[points.size()];

        for(int i=0; i<points.size() ; i++){
            pts[i] = convertToCoordinate(points.get(i));
        }
        return pts;
    }

    public  static Coordinate convertToCoordinate(GPoint pt){
        return new Coordinate(pt.x, pt.y);
    }


    public  static Envelope convertToEnvelope(MBR mbr){
        Envelope extent = new Envelope();
        extent.init(mbr.minX, mbr.maxX, mbr.minY, mbr.maxY);

        return extent;
    }

    public  static Geometry  envelopeToGeometry(Envelope envelope){
        GeometryFactory geometryFactory = new GeometryFactory();

        // Envelope의 네 꼭짓점을 이용하여 Polygon을 생성합니다.
        Coordinate lowerLeft = new Coordinate(envelope.getMinX(), envelope.getMinY());
        Coordinate upperLeft = new Coordinate(envelope.getMinX(), envelope.getMaxY());
        Coordinate upperRight = new Coordinate(envelope.getMaxX(), envelope.getMaxY());
        Coordinate lowerRight = new Coordinate(envelope.getMaxX(), envelope.getMinY());

        // 폴리곤을 형성하기 위한 좌표 배열을 생성합니다. 폴리곤은 폐쇄된 형태이므로 시작점으로 다시 돌아와야 합니다.
        Coordinate[] coordinates = {lowerLeft, upperLeft, upperRight, lowerRight, lowerLeft};

        // 좌표 배열로부터 Polygon 객체를 생성합니다.
        Geometry polygon = geometryFactory.createPolygon(coordinates);

        return polygon;
    }

    public  static Geometry mbrToGeometry(MBR mbr){
        GeometryFactory geometryFactory = new GeometryFactory();

        // Envelope의 네 꼭짓점을 이용하여 Polygon을 생성합니다.
        Coordinate lowerLeft = new Coordinate(mbr.minX , mbr.minY);
        Coordinate upperLeft = new Coordinate(mbr.minX , mbr.maxY);
        Coordinate upperRight = new Coordinate(mbr.maxX, mbr.maxY);
        Coordinate lowerRight = new Coordinate(mbr.maxX, mbr.minY);

        Coordinate[] coordinates = {lowerLeft, upperLeft, upperRight, lowerRight, lowerLeft};

        Geometry polygon = geometryFactory.createPolygon(coordinates);

        return polygon;
    }

    public  static MBR convertToMBR(Envelope extent){
        MBR mbr = new MBR();
        mbr.minX = extent.getMinX();
        mbr.minY = extent.getMinY();
        mbr.maxX = extent.getMaxX();
        mbr.maxY = extent.getMaxY();

        return mbr;
    }


}