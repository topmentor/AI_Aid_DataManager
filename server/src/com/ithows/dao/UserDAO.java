/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.dao;

import com.ithows.HttpUtil;
import com.ithows.JdbcDao;
import com.ithows.ResultMap;
import com.sox.ltex.CommonDoc;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class UserDAO {
    
    public final static int USERTYPE_ADMIN = 1;
    public final static int USERTYPE_SUPER = 2;
    public final static int USERTYPE_GENERAL = 3;
    public final static int USERTYPE_GUEST = 10;
   
    
    public final static int USERSORT_LASTLOGIN_DESC = 1;
    public final static int USERSORT_LASTLOGIN_ASC = 2;
    public final static int USERSORT_REGDATE_DESC = 9;
    public final static int USERSORT_REGDATE_ASC = 10;
    public final static int USERSORT_ID_DESC = 3;
    public final static int USERSORT_ID_ASC = 4;
    public final static int USERSORT_NO_DESC = 5;
    public final static int USERSORT_NO_ASC = 6;
    public final static int USERSORT_LEVEL_DESC = 7;
    public final static int USERSORT_LEVEL_ASC = 8;
    
    
    public static void main(String[] args) {
        System.out.println("result = " + getAllUserMacString());
    }
    
    
    /**
     * 전체 회원
     * @param orderOption
     * @param pageIdx : -1이면 전체
     * @param count
     * @return 
     */
    public static List getAllUser(int orderOption, int pageIdx, int count) {
        String query = "select * from user ";

         if(pageIdx > -1 && count > -1){
            String limitStr = "limit " + (pageIdx - 1) * count + ", " + count + " ";
            query = addOrderBy(query, orderOption)  + "  " + limitStr + ";" ;
            
        }else{
            
            query = addOrderBy(query, orderOption) + " ;";
        }
        
        try {
            return JdbcDao.queryForMapList(query, new Object[]{}); 
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static HashMap getAllUserMacString() {
        String query = "select * from user ;";
        String resultStr = "";
        ArrayList<ResultMap> list = null;
        HashMap<String, String> userMap = new HashMap<String, String>();

        try {
            list = (ArrayList<ResultMap>)JdbcDao.queryForMapList(query, new Object[]{}); 
            for( int i=0 ; i<list.size() ; i++){
                ResultMap map = list.get(i);
                if(map.getString("userMac")!=null && !map.getString("userMac").equals("")){
                    if(i==list.size()-1){
                        resultStr += "'" + map.getString("userMac") + "'";
                    }else{
                        resultStr += "'" + map.getString("userMac") + "',";
                    }
                    
                    userMap.put(map.getString("userMac"), map.getString("userId"));
                }
            }
            
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        userMap.put("@@AllMac@@", resultStr);
        
        return userMap;
    }
    
    public static int countAllUser() {
        String query = "select count(*) from user ";
        
        try {
            return JdbcDao.queryForInt(query, new Object[]{}); 
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }    
    
    
        
    public static boolean checkAdmin(String userId){
        String sql = "select userSecurityLevel from user where userId=? ";
        int userSecurityLevel = 10;
          
        try {
            userSecurityLevel = JdbcDao.queryForInt(sql, new Object[]{userId}); 
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("userSecurityLevel = " + userSecurityLevel);
        if(userSecurityLevel == 1){
            return true;
        }
        return false;
    }
    
    
    // check할 회원
    public static List getCheckUsers() {
        String query = "select * from user where userCheck = 1 ;";

        try {
            return JdbcDao.queryForMapList(query, new Object[]{}); 
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 상태정보에 따라 회원 검색
     * @param statusValue
     * @param orderOption
     * @param pageIdx
     * @param count
     * @return 
     */
    public static List getUsersStatus(int statusValue, int orderOption, int pageIdx, int count) {
        String query = "select * from user where userStatus = ? ";

         if(pageIdx > -1 && count > -1){
            String limitStr = "limit " + (pageIdx - 1) * count + ", " + count + " ";
            query = addOrderBy(query, orderOption)  + "  " + limitStr + ";" ;
            
        }else{
            
            query = addOrderBy(query, orderOption) + " ;";
        }
        
        try {
            return JdbcDao.queryForMapList(query, new Object[]{statusValue}); 
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    // 회원의 로그인 처리를 위한 (일반유저는 로그인 안 됨)
    public static ResultMap getLoginUser(String userId, String userPw) {
//        String query = "select * from user where userId=? and userPswd=? and userSecurityLevel=1 ";
        String query = "select * from user where userId=? and userPswd=? and userSecurityLevel = 1 ";
        ResultMap result = null;
        try {
            result = JdbcDao.queryForMapObject(query, new Object[]{userId, userPw});
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    
    public static int getUserNo(String userKey) {
        
        String query = "select userNo from user where userKey=? ";
        
        try {
            return JdbcDao.queryForInt(query, new Object[]{userKey}); 
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }  
    
    // 회원의 접속 아이디로 회원정보 알아내기
    public static ResultMap getUser(String userId, String userClass) {
        String query = "select * from user where userId=? and userClass=? ;";
        ResultMap result = null;
        try {
            result = JdbcDao.queryForMapObject(query, new Object[]{userId, userClass});
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    // 회원의 접속 아이디로 회원정보 알아내기
    public static ResultMap getUser(String userId) {
        String query = "select * from user where userId=?";
        ResultMap result = null;
        try {
            result = JdbcDao.queryForMapObject(query, new Object[]{userId});
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public static ResultMap getUser(int userNo) {
        String query = "select * from user where userNo=?";
        ResultMap result = null;
        try {
            result = JdbcDao.queryForMapObject(query, new Object[]{userNo});
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
   
    // 새로운 유저이면 추가.  유저의 userNo가 리턴 됨
    // 비번은 일단 아이디와 동일하게
    public static int insertUser(String userId, String passwordStr, String macStr, HttpServletRequest request) throws SQLException {
         
//        @@ 마리아 DB에서는 에러 발생         
//        String[] outParmas = new String[]{new String()};  // @@ 기존 버그 해결. 무조건 String으로 받은 후 변환
//        Object[] outParmas = new Object[]{new Object()};  // @@ 기존 버그 해결. 무조건 String으로 받은 후 변환
//        int userLevel = 10;
//        JdbcDao2.updateStored("{call sp_insert_user(?,?,?,?)} ", new Object[]{userId, userId, userLevel}, outParmas);
//        return Integer.parseInt(outParmas[0].toString());

        int res = 0;


        String sql = "select count(*) FROM user WHERE userId=?";
        int result = JdbcDao.queryForInt(sql, new Object[]{userId});
        String sql2 = "select count(*) FROM user WHERE userMac=?";
        int result2 = JdbcDao.queryForInt(sql2, new Object[]{macStr});
        
        int userLevel = 10;
        
	if(result == 0 && result2 == 0){
            JdbcDao.update("INSERT INTO user (  " +
"					userName," +
"					userId," +
"					userPswd," +
"					userMac," +
"					userSecurityLevel," +
"					userPoint," +
"					userStatus," +
"					userDownCount," +
"					userUpCount," +
"                                       userLoginCount," +
"					userRegisterDate," +
"                                       userLastLogin)" +
"				  VALUES (" +
"					?, " +
"					?," +
"					?," +
"					?," +
"					?," +
"					0," +
"					0," +
"					0," +
"					0," +
"                                       1," +
"					now()," +
"                                       now()\n" +
"             ); ", new Object[]{userId, userId, passwordStr, macStr, userLevel});
            
            res = 0;
        }else if(result == 1 && result2 == 0){
            updateUserMac(userId, macStr);
            res = 1;
        }else if(result == 0 && result2 == 1){
            updateUserId(userId, macStr);
            res = 2;
        }else{  // 이미 있음
            res = 3;
        }

        return res;
    }
     
    // 기존에 있는 유저이면 업데이트
    public static int updateUserMac(String userId, String macStr) throws SQLException {

//        @@ 마리아 DB에서는 에러 발생 
//        String[] outParmas = new String[]{new String()};  // @@ 기존 버그 해결. 무조건 String으로 받은 후 변환
//        Object[] outParmas = new Object[]{new Object()};  // @@ 기존 버그 해결. 무조건 String으로 받은 후 변환
//        JdbcDao2.updateStored("{call sp_update_user(?,?)} ", new Object[]{userId}, outParmas);
//        return Integer.parseInt(outParmas[0].toString());
        
        JdbcDao.update("UPDATE user SET userMac = ? WHERE  userId = ? ; ", new Object[]{macStr, userId});
        return 1;
    }
     
    public static int updateUserId(String userId, String macStr) throws SQLException {

//        @@ 마리아 DB에서는 에러 발생 
//        String[] outParmas = new String[]{new String()};  // @@ 기존 버그 해결. 무조건 String으로 받은 후 변환
//        Object[] outParmas = new Object[]{new Object()};  // @@ 기존 버그 해결. 무조건 String으로 받은 후 변환
//        JdbcDao2.updateStored("{call sp_update_user(?,?)} ", new Object[]{userId}, outParmas);
//        return Integer.parseInt(outParmas[0].toString());
        
          JdbcDao.update("UPDATE user SET userId = ?, userPswd = ? WHERE userMac = ? ; ", new Object[]{userId, userId, macStr});
          return 1;

    }
     
   
    public static int deleteUser(String userId) {
       int res = 0;
       
        try{
            if(!userId.equals("")) {
                 JdbcDao.update("DELETE FROM user where userId = '" + userId + "'" );
                 res = 1;
             }
        }catch(SQLException e){
            
        }
        return res;
    }
   
    /**
     * 암호 변경
     * @param userId : userId가 있으면 useNo는 -1
     * @param userNo : userNo가 있으면 useId는 ""
     * @param value
     * @return 
     */
    
    public static int updatePassword(String userId,  int userNo, String newPassword) {
        int res = 0;
        
        try{
            
            if(!userId.equals("")) {
                 JdbcDao.update("UPDATE user SET userPswd = ? WHERE userId = ? ", new Object[]{newPassword, userId});
                 res = 1;
             }else if(userNo > 0){
                 System.out.println("newPassword -----------" + newPassword);
                 
                 JdbcDao.update("UPDATE user SET userPswd = ? WHERE userNo = ? ", new Object[]{newPassword, userNo});
                 res = 1;
             }
            
        }catch(SQLException  e){
            System.out.println("updataPassword Error");
            return 0;
        }
        return 1;
    }
 
    
    private static String addOrderBy(String query, int orderOption){
        String queryStr = "";
        
        if(orderOption == UserDAO.USERSORT_LASTLOGIN_DESC ){
            queryStr = query + " order by userLastLogin desc, userId asc " ;
            
        } else if(orderOption == UserDAO.USERSORT_LASTLOGIN_ASC ){
            queryStr = query + " order by userLastLogin asc, userId asc  " ;
            
        } else if(orderOption == UserDAO.USERSORT_REGDATE_DESC ){
            queryStr = query + " order by userRegisterDate desc, userId asc  " ;
            
        } else if(orderOption == UserDAO.USERSORT_REGDATE_ASC ){
            queryStr = query + " order by userRegisterDate asc, userId asc  " ;
            
        } else if(orderOption == UserDAO.USERSORT_NO_DESC ){
            queryStr = query + " order by userNo desc  " ;
            
        } else if(orderOption == UserDAO.USERSORT_NO_ASC ){
            queryStr = query + " order by userNo asc  " ;
            
        } else if(orderOption == UserDAO.USERSORT_LEVEL_DESC ){
            queryStr = query + " order by userSecurityLevel desc, userId asc  " ;
            
        } else if(orderOption == UserDAO.USERSORT_LEVEL_ASC ){
            queryStr = query + " order by userSecurityLevel asc, userId asc  " ;
            
        } else if(orderOption == UserDAO.USERSORT_ID_DESC ){
            queryStr = query + " order by userId desc  " ;
            
        } else if(orderOption == UserDAO.USERSORT_ID_ASC ){
            queryStr = query + " order by userId asc  " ;
            
        }else{
            queryStr = query;
        }
        
        return queryStr;
    }
}
