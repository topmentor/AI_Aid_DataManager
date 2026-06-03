/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.model.shape;

import java.awt.geom.Point2D;

public class GPoint implements Comparable, Cloneable {
    public double x;
    public double y;

    public GPoint(GPoint pt) {
        this.x = pt.x;
        this.y = pt.y;
    }

    public GPoint copy() {

        return new GPoint(this);
    }

    public GPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public GPoint() {
    }

    public void set(GPoint pt) {
        x = pt.x;
        y = pt.y;
    }

    public boolean equal(GPoint pt) {

        return x == pt.x && y == pt.y;
    }

    public boolean equals(Object other) {
        if (!(other instanceof GPoint)) {
            return false;
        }
        GPoint otherCoordinate = (GPoint) other;
        return (     x == otherCoordinate.x
                &&   y == otherCoordinate.y );
    }

    public int compareTo(Object o) {
        GPoint other = (GPoint) o;
        if (x < other.x) {
            return -1;
        }
        if (x > other.x) {
            return 1;
        }
        if (y < other.y) {
            return -1;
        }
        if (y > other.y) {
            return 1;
        }
        return 0;
    }

    public double getDistanceFrom(GPoint pt) {
        return Math.sqrt((x - pt.x) * (x - pt.x) + (y - pt.y) * (y - pt.y));
    }

    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append('(');
        result.append(x);
        result.append(',');
        result.append(y);
        result.append(')');

        return result.toString();
    }

    public Point2D convertPoint2D(){
        return new Point2D.Double(x,y);
    }
    
    public String toSpatialQurey(){
        String query = "ST_PointFromText(CONCAT( 'POINT(', " + this.x + ", ' ', " + this.y + ", ')' ))";
        return query;
    }

}