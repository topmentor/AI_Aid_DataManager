/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sox.ltex.util;

import com.ithows.ResultMap;
import com.sox.ltex.model.shape.GPoint;
import com.sox.ltex.model.shape.MBR;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 *
 * @author mailt
 */
public class GeoCluster {
    public MBR mbr = new MBR(-1,-1,-1,-1);
    public GPoint centerPoint = null;
    public ArrayList<Long> ids = new ArrayList<>();
    public ArrayList<GPoint> clusterPoints = new ArrayList<>();
    public ArrayList<GPoint> convexHull = null;
    
    public ResultMap info = new ResultMap();
    
    
    public GeoCluster (MBR mbr, GPoint pt){
        this.mbr = mbr;
        this.centerPoint = pt;
    }
    
    public GeoCluster (MBR mbr, double x, double y){
        this(mbr, new GPoint(x,y));
    }

    public void insertPoint(long id, double x, double y){

//        System.out.println("x = " + x + "  y= " + y);
        if(x >= mbr.minX && x <= mbr.maxX && y >= mbr.minY && y <= mbr.maxY){
            ids.add(id);
            // System.out.println("insert id  = " + id);
        }

    }

    public void insertPoint(long id, GPoint pt){
        this.insertPoint(id, pt.x, pt.y);
    }

    public void setConvexHull(){
        this.convexHull = computeConvexHull(clusterPoints);
    }

    public ArrayList<GPoint> getConvexHull(){
        return convexHull;
    }
    

    
    
    public JSONObject toJson(){
        JSONObject res = new JSONObject();
        try {
            res.put("minX", mbr.minX);
            res.put("minY", mbr.minY);
            res.put("maxX", mbr.maxX);
            res.put("maxY", mbr.maxY);
            res.put("centerX", centerPoint.x);
            res.put("centerY", centerPoint.y);
            
            JSONArray jArr = new JSONArray();
            for(Long id : ids){
                jArr.put(id);
            }
            res.put("ids", jArr);
            res.put("info", info.toJson());

            JSONArray hullArray = new JSONArray();
            for(GPoint pt : convexHull){
                JSONArray coord = new JSONArray();
                coord.put(pt.x);
                coord.put(pt.y);
                hullArray.put(coord);
            }
            res.put("convexHull", hullArray);
            
        } catch (JSONException ex) {
        }
        return res;
    }

    public JSONObject toGeoJson(){
        JSONObject feature = new JSONObject();
        try{
            feature.put("type", "Feature");

            JSONObject geometry = new JSONObject();
            geometry.put("type", "Polygon");

            JSONArray ring = new JSONArray();
            ArrayList<GPoint> hull = (convexHull != null && !convexHull.isEmpty()) ? convexHull : createMbrRing();
            if(hull != null){
                for(GPoint pt : hull){
                    JSONArray coord = new JSONArray();
                    coord.put(pt.x);
                    coord.put(pt.y);
                    ring.put(coord);
                }
                // GeoJSON polygon ring은 폐곡선을 요구
                if(hull.size() > 0){
                    GPoint first = hull.get(0);
                    GPoint last = hull.get(hull.size()-1);
                    if(first.x != last.x || first.y != last.y){
                        JSONArray coord = new JSONArray();
                        coord.put(first.x);
                        coord.put(first.y);
                        ring.put(coord);
                    }
                }
            }
            JSONArray coords = new JSONArray();
            coords.put(ring);
            geometry.put("coordinates", coords);
            feature.put("geometry", geometry);

            JSONObject props = new JSONObject();
            props.put("minX", mbr != null ? mbr.minX : JSONObject.NULL);
            props.put("minY", mbr != null ? mbr.minY : JSONObject.NULL);
            props.put("maxX", mbr != null ? mbr.maxX : JSONObject.NULL);
            props.put("maxY", mbr != null ? mbr.maxY : JSONObject.NULL);
            props.put("centerX", centerPoint != null ? centerPoint.x : JSONObject.NULL);
            props.put("centerY", centerPoint != null ? centerPoint.y : JSONObject.NULL);

            JSONArray idArr = new JSONArray();
            for(Long id : ids){
                idArr.put(id);
            }
            props.put("ids", idArr);
            props.put("info", info.toJson());

            feature.put("properties", props);
        }catch(JSONException ex){
        }
        return feature;
    }

    private ArrayList<GPoint> createMbrRing(){
        if(mbr == null){
            return null;
        }
        ArrayList<GPoint> ring = new ArrayList<>();
        ring.add(new GPoint(mbr.minX, mbr.minY));
        ring.add(new GPoint(mbr.maxX, mbr.minY));
        ring.add(new GPoint(mbr.maxX, mbr.maxY));
        ring.add(new GPoint(mbr.minX, mbr.maxY));
        ring.add(new GPoint(mbr.minX, mbr.minY)); // close ring
        return ring;
    }
    
    public String toString(){
        StringBuffer buf = new StringBuffer();
        
        buf.append("minX=").append(mbr.minX).append("\n");
        buf.append("minY=").append(mbr.minY).append("\n");
        buf.append("maxX=").append(mbr.maxX).append("\n");
        buf.append("maxY=").append(mbr.maxY).append("\n");
        buf.append("centerX=").append(centerPoint.x).append("\n");
        buf.append("centerY=").append(centerPoint.y).append("\n");
        buf.append("ids=");
        buf.append(ids.stream()
          .map(String::valueOf)
          .collect(Collectors.joining(","))).append("\n");
        buf.append("info=").append(info.toString()).append("\n");
            
            
        return buf.toString();
    }



        
    private static ArrayList<GPoint> computeConvexHull(ArrayList<GPoint> clusterPoints) {
        ArrayList<GPoint> hullPoints = new ArrayList<>();
        if (clusterPoints == null || clusterPoints.isEmpty()) {
            return hullPoints;
        }

        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coords = new Coordinate[clusterPoints.size()];
        for (int i = 0; i < clusterPoints.size(); i++) {
            GPoint pt = clusterPoints.get(i);
            coords[i] = new Coordinate(pt.x, pt.y);
        }

        Geometry hullGeometry = geometryFactory.createMultiPointFromCoords(coords).convexHull();
        for (Coordinate coord : hullGeometry.getCoordinates()) {
            hullPoints.add(new GPoint(coord.x, coord.y));
        }

        return hullPoints;
    }
    
}
