/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.ithows.dao;

import com.ithows.AppConfig;
import com.ithows.JdbcDao;
import com.ithows.ResultMap;
import com.ithows.util.DateTimeUtils;
import com.ithows.util.KeyGenerator;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

/**
 *
 * @author mailt
 */
public class UserKeyDAO {
      
    public static void main(String[] args) {
        // System.out.println("result = " + makeUserKey("",""));
    }
    
    
    // 새로운 키 생성    
    public static String makeUserKey(String userId, String userClass){
        String userKey = "";
        ResultMap user = UserDAO.getUser(userId, userClass);

        if(user == null || user.isEmpty()){
            return userKey;
        }
        
        userKey = KeyGenerator.createNormalKey(8);
        
        updateKey(userId, userKey, userClass);
        
        return userKey;
    }
    
    public static boolean checkAPIKey(String apiKey){
        String key = AppConfig.getConf("common_api_key");
        
        if(apiKey.equals(key)){
            return true;
        }
        
        return false;
    }
    
    
    public static boolean checkKey(String userKey){
        
        // @@ to-do 추후 기간 유효성 로직 추가
//        System.out.println("userKey = " + userKey);
        
        String sql = "select count(*) from user where userKey=? ";
        int cnt = 0;
          
        try {
            cnt = JdbcDao.queryForInt(sql, new Object[]{userKey}); 
        } catch (SQLException e) {
            e.printStackTrace();
        }
//        System.out.println("cnt = " + cnt);
        
        if(cnt > 0){
            return true;
        }
        
        return false;
    }
    
    public static String getUserKey(String userId, String userClass){
        String sql = "select userKey from user where userId=? and userClass=? ; ";
        String userKey = "";
          
        try {
            userKey = JdbcDao.queryForString(sql, new Object[]{userId, userClass}); 
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("userKey = " + userKey);
        
        return userKey;
    }
    
    
    public static boolean deleteKey(String userId, String userClass) {
       boolean res = false;
       
        try{
                JdbcDao.update("UPDATE user SET userKey =''  where userId = ? and userClass=? ;" , new Object[]{userId, userClass});
                updateKeyLog(userId, userClass, "delete key");
                 res = true;
        }catch(SQLException e){
            
        }
        return res;
    }
   
    
    public static boolean updateKey(String userId, String newKey, String userClass) {
       boolean res = false;
       
        try{
            if(!userId.equals("")) {
                 JdbcDao.update("UPDATE user SET userKey =?  where userId = ? and userClass=? ;"  , new Object[]{newKey, userId, userClass});
                 updateKeyLog(userId, userClass, "get new key");
                 res = true;
             }
        }catch(SQLException e){
            
        }
        return res;
    }
   
   
    
    
    // 1유저 로그 업데이트 
    private static void updateKeyLog(String userId, String userClass, String newLog){
        
        if(newLog.equals("")) {
            return;
        }
        
        try{
                String query = "select userKeyLog from user where userId=? and userClass=? ";
                String result = JdbcDao.queryForString(query, new Object[]{userId, userClass});
                
                
                String logTime = DateTimeUtils.getTimeDateNow();  
                JSONObject jObj = null;
                
                if(!result.equals("")){
                    jObj = new JSONObject(result);
                    jObj.put(logTime, newLog);
                }else{
                    jObj = new JSONObject();
                    jObj.put(logTime, newLog);
                }
                
                 
                JdbcDao.update("UPDATE user SET userKeyLog =?  where userId = ? and userClass=? ;"  , new Object[]{ jObj.toString(), userId, userClass});
                
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
