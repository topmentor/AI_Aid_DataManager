/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/**
 *
 * @author dreamct
 */
public class ResultMap extends HashMap implements Comparable<Object> {

    public static String compareField = "";

    public ResultMap() {

    }

    public ResultMap(Map map) {
        this.putAll(map);
    }

    public String getString(String key) {
        Object obj = this.get(key);
        if (obj != null) {
            return obj.toString();
        } else {
            return null;
        }
    }

    // 바이너리 필드를 받는 경우 
    public byte[] getBytes(String key) {
        Object obj = this.get(key);
        if (obj != null) {
            return (byte[])obj;
        } else {
            return null;
        }
    }

    public int getInt(String key) {
        Object obj = this.get(key);
        int tmp = -1;
        if (obj != null) {
            try {
                tmp = Integer.parseInt(obj.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }

    public int getInt(String key, int defaultVal) {
        Object obj = this.get(key);
        int tmp = defaultVal;
        if (obj != null) {
            try {
                tmp = Integer.parseInt(obj.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }
    
 
    public long getLong(String key) {
        Object obj = this.get(key);
        long tmp = -1;
        if (obj != null) {
            try {
                tmp = Long.parseLong(obj.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }   
    
    public long getLong(String key,long defaultVal) {
        Object obj = this.get(key);
        long tmp = defaultVal;
        if (obj != null) {
            try {
                tmp = Long.parseLong(obj.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }   

    public double getDouble(String key) {
        Object obj = this.get(key);
        double tmp = -1.0;
        if (obj != null) {
            try {
                tmp = Double.parseDouble(obj.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }

    public double getDouble(String key, int defaultVal) {
        Object obj = this.get(key);
        double tmp = defaultVal;
        if (obj != null) {
            try {
                tmp = Double.parseDouble(obj.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }

    public void printElements() {
        for (Object key : this.keySet()) {
            System.out.println("[ " + key + " ] : " + this.get(key));
        }
    }

    public void printElements(PrintStream out) {

        if (out == null) {
            return;
        }

        for (Object key : this.keySet()) {
            out.println("[ " + key + " ] : " + this.get(key));
        }
    }

    public void printInsertSQL(String tableName) {
        String sqlStr = "INSERT INTO " + tableName + " ";
        String fieldName = "";
        String fieldValue = "";
        int i = 1;
        for (Object key : this.keySet()) {
            if (i == this.size()) {
                fieldName = fieldName + key + " ";
                fieldValue = fieldValue + this.get(key) + " ";
            } else {
                fieldName = fieldName + key + ", ";
                fieldValue = fieldValue + this.get(key) + ", ";
            }
            i++;
        }
        sqlStr = sqlStr + "(" + fieldName + ") " + "VALUES (" + fieldValue + ");";

        System.out.println("SQL : " + sqlStr);
    }

    public JSONObject toJson() {
        JSONObject jObj = null;
        try {
            jObj = new JSONObject(this);
        } catch (Exception ex) {

        }
        return jObj;
    }

    @Override
    public int compareTo(Object obj) {
        String orgString = this.get(compareField).toString();
        String objString = ((ResultMap) obj).get(compareField).toString();
        return orgString.compareTo(objString);

    }

}
