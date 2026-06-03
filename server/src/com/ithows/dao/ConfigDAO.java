/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ithows.dao;

import com.ithows.JdbcDao;
import com.ithows.ResultMap;
import com.ithows.util.UtilString;
import java.sql.SQLException;
import java.util.List;
import org.json.JSONObject;


/**
 *
 * @author mailt
 */
public class ConfigDAO {
    
    
    public static ResultMap selectConfig(String idStr){

        String query = "select *  " +
                "from config " +
                "where id = ? ;";

        ResultMap result = null;
        try {
            result = JdbcDao.queryForMapObject(query, new Object[]{idStr});
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    
    public static int convertInt(ResultMap map, int defaultValue){

        int result = defaultValue;
        
        if(map == null || map.size() == 0){
            return result;
        }
        
        try{
            int type = map.getInt("type");
            
            if(type == 1){
                result = Integer.parseInt(map.getString("value")) ;
            }
        }catch(Exception e){
            
        }
        
        return result;
    }
    
    public static double convertDouble(ResultMap map, double defaultValue){

        double result = defaultValue;
        
        try{
            int type = map.getInt("type");
            
            if(type == 2){
                result = Double.parseDouble(map.getString("value")) ;
            }
        }catch(Exception e){
            
        }
        
        return result;
    }
    
    public static JSONObject convertJson(ResultMap map){

        JSONObject result = null;
        
        try{
            int type = map.getInt("type");
            
            if(type == 3){
                result = new JSONObject(map.getString("value")) ;
            }
        }catch(Exception e){
            
        }
        
        return result;
    }

    
    
    public static List selectAllConfig(){

        String query = "select *  " +
                "from config ;" ;

        List list = null;
        try {
            list = JdbcDao.queryForMapList(query, new Object[]{});
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }   
    
    
    final static int CONFIG_VALUETYPE_NONE = 0;
    final static int CONFIG_VALUETYPE_INT = 1;
    final static int CONFIG_VALUETYPE_DOUBLE = 2;
    final static int CONFIG_VALUETYPE_STRING = 3;
    final static int CONFIG_VALUETYPE_JSON = 4;
    final static int CONFIG_VALUETYPE_CSV = 5;
    
    public static int insertConfig(String idStr, String valueStr, int type, String commentStr){
        
       return updateConfig(idStr, valueStr, type, commentStr); 
    }

    
    public static int updateConfig (String idStr, String valueStr, int type,  String commentStr) {
        String query = "select count(*) " +
                    "FROM config "+
                    "WHERE id=? ;" ;
        int no = 0;

         try {
             int count = JdbcDao.queryForInt(query, new Object[]{idStr});  

             if(count>0){
                 JdbcDao.update("UPDATE config SET value=?, type = ?, comment = ?, lastUpdate=now() WHERE id = ?", new Object[]{valueStr, type, commentStr, idStr});        
             }else{
                 JdbcDao.update("INSERT INTO config (id, value, type, comment, lastUpdate, registerTime ) " +
                         "  VALUES (?,?,?,?,now(),now()); ", new Object[]{idStr, valueStr, type, commentStr});
             }

             no = 1;
         }catch(Exception ex){
             ex.getLocalizedMessage();
         }

        return no ; 
    }

    public static int updateConfig (String idStr, String valueStr, String commentStr) {
        String query = "select count(*) " +
                    "FROM config "+
                    "WHERE id=? ;" ;
        int no = 0;

         try {
             int count = JdbcDao.queryForInt(query, new Object[]{idStr});  

             if(count>0){
                 JdbcDao.update("UPDATE config SET value=?, comment = ?, lastUpdate=now() WHERE id = ?", new Object[]{valueStr, commentStr, idStr});        
             }else{
                 JdbcDao.update("INSERT INTO config (id, value, type, comment, lastUpdate, registerTime ) " +
                         "  VALUES (?,?,3,?,now(),now()); ", new Object[]{idStr, valueStr, commentStr});
             }

             no = 1;
         }catch(Exception ex){
             ex.getLocalizedMessage();
         }

        return no ; 
    }
    
    public static int updateConfig (String idStr, String valueStr) {
        String query = "select count(*) " +
                    "FROM config "+
                    "WHERE id=? ;" ;
        int no = 0;

         try {
             int count = JdbcDao.queryForInt(query, new Object[]{idStr});  

             if(count>0){
                 JdbcDao.update("UPDATE config SET value=?, lastUpdate=now() WHERE id = ?", new Object[]{valueStr, idStr});        
             }else{
                 JdbcDao.update("INSERT INTO config (id, value, type, lastUpdate, registerTime ) " +
                         "  VALUES (?,?,3,now(),now()); ", new Object[]{idStr, valueStr});
             }

             no = 1;
         }catch(Exception ex){
             ex.getLocalizedMessage();
         }

        return no ; 
    }

  
    
    public static String convertCSV(ResultMap map){
        return convertString(map);
    }
    
    public static String convertString(ResultMap map){

        String result = "";
        
        try{
            int type = map.getInt("type");
            
            if(type == CONFIG_VALUETYPE_STRING || type == CONFIG_VALUETYPE_CSV){
                
                String value = map.getString("value");
                if(value != null && !value.isEmpty()){
                    result = UtilString.removeNullCsv(value, ",") ;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        
        return result;
    }
      
}
