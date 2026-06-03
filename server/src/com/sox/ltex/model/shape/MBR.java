/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.model.shape;


import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

public class MBR {
	public double minX;
	
	public double minY;
	
	public double maxX;
	
	public double maxY;
        
        
        public static void main(String[] args) {
	}
        
        
	public MBR() {
		reset();
	}
	
	public MBR(double x1, double y1, double x2, double y2) {
            
                double minX;
                double minY;
                double maxX;
                double maxY;
                
                if(x1 < x2){
                    minX = x1;
                    maxX = x2;
                }else{
                    maxX = x1;
                    minX = x2;
                }
                
                if(y1 < y2){
                    minY = y1;
                    maxY = y2;
                }else{
                    maxY = y1;
                    minY = y2;
                }
            
		set(minX, minY, maxX, maxY);
	}
	
	public MBR(MBR mbr) {
            set(mbr.minX, mbr.minY, mbr.maxX, mbr.maxY);
	}


	public MBR(ArrayList<GPoint> points) {
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;

            for(int i=0; i < points.size() ; i++){
                    if(minX > points.get(i).x ){
                            minX = points.get(i).x;
                    }
                    if(minY > points.get(i).y ){
                            minY = points.get(i).y;
                    }
                    if(maxX < points.get(i).x ){
                            maxX = points.get(i).x;
                    }
                    if(maxY < points.get(i).y ){
                            maxY = points.get(i).y;
                    }
            }

            set(minX, minY, maxX, maxY);
	}

	public double getWidth() {
		return maxX - minX;
	}
	
	public double getHeight() {
		return maxY - minY;
	}
	
	public void set(MBR mbr){
		this.minX = mbr.minX;
		this.minY = mbr.minY;
		this.maxX = mbr.maxX;
		this.maxY = mbr.maxY;
        }
        
	public void set(double minX, double minY, double maxX, double maxY)
	{
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
	}
	
	public boolean isIn(GPoint coord) {
		boolean bOK = coord.x >= minX && coord.x <= maxX && coord.y >= minY && coord.y <= maxY;
		return bOK;
	}
	
	public boolean isIn(GPoint coord, double tol) {
		boolean bOK = coord.x > (minX-tol) && coord.x < (maxX+tol) && coord.y > (minY-tol) && coord.y < (maxY+tol);
		return bOK;
	}
	
	public MBR exapnd(double value) {
		MBR newMbr = new MBR(minX-value, minY-value, maxX+value, maxY+value);
		return newMbr;
	}

	public boolean isIntersect(MBR other) {
		if (this.minX > other.maxX) {
			return false;
		}
		if (this.minY > other.maxY) {
			return false;
		}
		if (this.maxX < other.minX) {
			return false;
		}
		if (this.maxY < other.minY) {
			return false;
		}
		return true;
	}


	public void move(double x, double y) {
		double deltaX = getCenterX() - x;
		double deltaY = getCenterY() - y;
		
		minX -= deltaX;
		minY -= deltaY;
		maxX -= deltaX;
		maxY -= deltaY;
	}
	
	public void append(MBR mbr) {
            if(mbr.minX < minX) {
                    minX = mbr.minX;
            }

            if(mbr.minY < minY) {
                    minY = mbr.minY;
            }

            if(mbr.maxX > maxX) {
                    maxX = mbr.maxX;
            }

            if(mbr.maxY > maxY) {
                    maxY = mbr.maxY;		
            }
	}
	
	public void append(GPoint pt) {
		if(pt.x < minX) minX = pt.x;
		if(pt.y < minY) minY = pt.y;
		if(pt.x > maxX) maxX = pt.x;
		if(pt.y > maxY) maxY = pt.y;		
	}
	
	public double getCenterX() {
		return (maxX + minX) / 2.0;
	}
	
	public double getCenterY() {
		return (maxY + minY) / 2.0;
	}	

        
        public GPoint getCenterPoint(){
            GPoint centPt = new GPoint();
            centPt.x = getCenterX();
            centPt.y = getCenterY();
            return centPt;
        }
        
	public double[] getDoubleArray(){
		double[] bounds = new double[4];
		bounds[0] = minX;
		bounds[1] = minY;
		bounds[2] = maxX;
		bounds[3] = maxY;

		return bounds;
	}

	public void valid() {
		if(minX > maxX) {
			double tmp = minX;
			minX = maxX;
			maxX = tmp;
		}
		
		if(minY > maxY) {
			double tmp = minY;
			minY = maxY;
			maxY = tmp;
		}		
	}
	
	public void reset() {
		minX = Double.MAX_VALUE;
		minY = Double.MAX_VALUE;
		maxX = -Double.MAX_VALUE;
		maxY = -Double.MAX_VALUE;
	}

	public boolean isValid() {
		return minX < maxX && minY < maxY;
	}
	
	public String toString() {
		return "[[" + minX + " , " + minY + "],[" + maxX + ", " + maxY + "]]";
	}
        
	public String toJsonString() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("minX", minX);
                obj.put("minY", minY);
                obj.put("maxX", maxX);
                obj.put("maxY", maxY);

            } catch (JSONException ex) {
                System.out.println(ex.getLocalizedMessage());
            }

            return obj.toString();
	}
        
	public String toString(boolean reverse) {
            if(reverse)
		return "[[" + minY + " , " + minX + "],[" + maxY + ", " + maxX + "]]";
            else
		return "[[" + minX + " , " + minY + "],[" + maxX + ", " + maxY + "]]";
                
	}
        
	public String toSpationQueryString(boolean isPoint) {
            return convertSpatialQuery(this, isPoint);
	}
        
        public ArrayList<GPoint> toGPoints() {
            ArrayList<GPoint> pts = new ArrayList<GPoint>();
            pts.add(new GPoint(minX, minY));
            pts.add(new GPoint(maxX, minY));
            pts.add(new GPoint(maxX, maxY));
            pts.add(new GPoint(minX, maxY));
            pts.add(new GPoint(minX, minY));
	    return pts;
	}

        
        public static String convertSpatialQuery(MBR extent, boolean isPoint){
            String query = "";
            
            
            if(extent == null){
                return query;
            }
            
            if(isPoint){
              query = "ST_Contains( ST_PolyFromText('POLYGON(("
                    + extent.minX + " " + extent.minY + ","
                    + extent.minX + " " + extent.maxY + ","
                    + extent.maxX + " " + extent.maxY + ","
                    + extent.maxX + " " + extent.minY + ","
                    + extent.minX + " " + extent.minY + "))'), pt) ";    
            }else{
                query = "ST_Intersects(extent, ST_PolyFromText('POLYGON(("
                    + extent.minX + " " + extent.minY + ","
                    + extent.minX + " " + extent.maxY + ","
                    + extent.maxX + " " + extent.maxY + ","
                    + extent.maxX + " " + extent.minY + ","
                    + extent.minX + " " + extent.minY + "))')) ";
            
            }
            
            return query;
        }
        
        
        public double[] calculateSize() {
		double[] dist = new double[2];
		dist[0] = com.sox.ltex.util.CoordTransformUtil.getDistanceBetween(new GPoint(minX, minY), new GPoint(maxX, minY), true);
		dist[1] = com.sox.ltex.util.CoordTransformUtil.getDistanceBetween(new GPoint(minX, minY), new GPoint(minX, maxY), true);
		return dist;
	}

	public void printSize(){
		double[] dist = calculateSize();
		System.out.println("width = " + dist[0] + " , height = " + dist[1]);
	}
}
