/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sox.ltex.model;

/**
 *
 * @author mailt
 */
public class ShapeType {
	public final static int UNDEFINED = -1;
	public final static int NullShape	= 0;
	public final static int Point		= 1;
	public final static int PolyLine	= 3;
	public final static int Polygon		= 5;
	public final static int MultiPoint	= 8;
	public final static int PointZ		= 11;
	public final static int PolyLineZ	= 13;
	public final static int PolygonZ	= 15;
	public final static int MultiPointZ	= 18;
	public final static int PointM		= 21;
	public final static int PolyLineM	= 23;
	public final static int PolygonM	= 25;
	public final static int MultiPointM	= 28;
	public final static int MultiPatch	= 31;

	public final static int HeatmapPoint	= 32;  // @@ Heatmap

	public final static int Ring	= 33;
	public final static int MultiPolygon	= 34;

	public static boolean hasMBR(int type) {
		return type != Point && type != PointM && type != PointZ;
	}
	
	public static boolean isPointType(int type) {
		return (type == Point) || (type == PointZ) || (type == PointM) || (type == HeatmapPoint);  // @@ Heatmap
	}
	
	public static boolean isMultiPointType(int type) {
		return (type == MultiPoint) || (type == MultiPointZ) || (type == MultiPointM) || (type == MultiPolygon);
	}
	
	public static boolean isPolylineType(int type) {
		return (type == PolyLine) || (type == PolyLineZ) || (type == PolyLineM);
	}
	
	public static boolean isPolygonType(int type) {
		return (type == Polygon) || (type == PolygonZ) || (type == PolygonM) || (type == Ring) || (type == MultiPolygon) ;
	}
}