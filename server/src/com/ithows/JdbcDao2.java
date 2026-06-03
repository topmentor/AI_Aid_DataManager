/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ithows;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;

/**
 *
 * @author mailt
 */
public class JdbcDao2 {
    
    
    static {
        try {
            JdbcDao2.loadProperties();
        } catch (Exception ex) {
            BaseDebug.log(ex, "드라이브 로딩 에러");
        }
    }

    private static DataSource datasource = null;
    private static String dbUser = "soxuser";
    private static String dbUserPW = "sox123";

    public static Properties loadProperties() {
        Properties p = new Properties();
        try {
            String fileName;
            String OS = System.getProperty("os.name").toLowerCase();

            if(OS.indexOf("win") >= 0){
//                fileName = JdbcDao2.class.getResource("/").toString().replaceAll("file:/", "") + "connpool_etri.xml";  // -- @@ 윈도우즈 적용
                fileName = JdbcDao2.class.getResource("/").toString().replaceAll("file:/", "") + "connpool.xml";  // -- @@ 윈도우즈 적용
            }else{
//                fileName = "/usr/share/tomcat/webapps/SSF2026/WEB-INF/classes/connpool.xml";  // @@ 리눅스 용 
                fileName = "/" +  JdbcDao2.class.getResource("/").toString().replaceAll("file:/", "") + "connpool.xml" ; //  @@ 리눅스 용 
            }


            File f = new File(fileName);
            if (f.exists()) {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));  //XML경로로 파일스트림을 연다
                p.loadFromXML(bis);  //XML파일의 프로퍼티를 로드함
                bis.close();            //inputstream 닫음
            } else {
                BaseDebug.info("if else 로딩 실패", f.toString());
            }
        } catch (IOException e) {
            BaseDebug.log(e, "로딩 실패");
        }
        return p;
    }

    public synchronized static DataSource getDataSource() {
        
                
        if (datasource == null) {
            PoolConfiguration p = new PoolProperties();
            String OS = System.getProperty("os.name").toLowerCase();
            Properties config = JdbcDao2.loadProperties();

            p.setDriverClassName("org.mariadb.jdbc.Driver");  // @@ mariaDB 
            p.setUrl(config.getProperty("db2Url"));
            
//            if(config.getProperty("database").equals("mysql")){
//                p.setDriverClassName("com.mysql.jdbc.Driver"); // -- @@ mysql
//                p.setUrl(config.getProperty("db2Url"));
//            }else{
//            }
            
            // @@ 보안을 위해 내부에 숨김 
            p.setUsername(dbUser);
            p.setPassword(dbUserPW);
            
            p.setMinIdle(Integer.parseInt(config.getProperty("minIdle")));
            p.setMaxIdle(Integer.parseInt(config.getProperty("maxIdle")));
            p.setInitialSize(Integer.parseInt(config.getProperty("initialSize")));
            p.setMaxActive(Integer.parseInt(config.getProperty("maxActive")));
            p.setRemoveAbandonedTimeout(Integer.parseInt(config.getProperty("removeAbandonedTimeout")));  // @@ org.apache.tomcat.jdbc.pool.ConnectionPool.abandon Connection has been abandoned 에러 대응
            p.setRemoveAbandoned(false);
            p.setJmxEnabled(true);
            p.setTestWhileIdle(false);
            p.setTestOnBorrow(true);
            p.setValidationQuery("SELECT 1");
            p.setTestOnReturn(false);
            p.setValidationInterval(30000);
            p.setTimeBetweenEvictionRunsMillis(30000);
            p.setMaxWait(10000);
            p.setMinEvictableIdleTimeMillis(30000);
            p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
            p.setLogAbandoned(true);
            
            datasource = new DataSource();
            datasource.setPoolProperties(p);
        }
        return datasource;
    }

    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = JdbcDao2.getDataSource().getConnection();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            BaseDebug.log(e,  "Connection 얻어내기 에러");
        }
        return conn;
    }

    private static void printPool() {
        if (datasource != null) {
            BaseDebug.info(" active:" + datasource.getPool().getActive());
            BaseDebug.info(" wait:" + datasource.getPool().getWaitCount());
            BaseDebug.info(" idle:" + datasource.getPool().getIdle());
            BaseDebug.info(" ");
        }
    }

    public static void endConnection(Connection conn) {
        try {
            conn.commit();
        } catch (SQLException e) {
            try {
                BaseDebug.log(e, "endConnection 에러 - commit - error");
                conn.rollback();
            } catch (SQLException e2) {
                BaseDebug.log(e2, "endConnection 에러 - rollback - error");
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException e3) {
                BaseDebug.log(e3,  "endConnection 에러 - close - error");
            }
        }
    }

    public static void transacUpdate(JdbcTransactor transactor) throws SQLException{
        Connection conn = JdbcDao2.getConnection();
        try{
            if(conn!=null)
                transactor.doTransaction(conn);
        }catch(SQLException e){
            throw e;
        }finally{
            if(conn!=null)
                JdbcDao2.endConnection(conn);
        }
    }    
    
    
    
    public static boolean execute(String sp_query) throws SQLException {
        return JdbcDao2.execute(null, sp_query);
    }

    /**
     * alter table DML 쿼리를 실행한다
     * @param sp_query 요청된 query
     * @return result 쿼리 성공 여부
     * @throws SQLException
     */
    public static boolean execute(Connection conn, String sp_query) throws SQLException {
        boolean result = false;
        if (sp_query == null) {
            throw new SQLException("SQL Query가 NULL입니다.");
        }
        /* --------2011.9.5 transaction 처리를 위해서 ------------------- */
        boolean isInnerConnection = false;//일단은 외부 Connection 이용
        if(conn==null){ //없으면 내부 Connection 이용
            isInnerConnection = true;
            conn = JdbcDao2.getConnection();
        }
        /* -------------------------------------------------------------- */

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sp_query);
            result = pstmt.execute();
        } catch (SQLException ex) {
            System.out.println("Alter Query ");
            throw ex;
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getLocalizedMessage());
                throw ex;
            } finally {
                if (conn != null) {
                    /* -------------------------------------------------------------- */
                    if(isInnerConnection==true){//내부에서 만든 Connection만 제거한다.
                        JdbcDao2.endConnection(conn);
                    }/* -------------------------------------------------------------- */
                }
            }
        }
        return result;
    }

    
    public static int update(String sp_query) throws SQLException {
        return JdbcDao2.update(null, sp_query, null);
    }    
    public static int update(String sp_query, Object[] sp_Input) throws SQLException {
        return JdbcDao2.update(null, sp_query, sp_Input);
    }
    /**
     * insert, update, delete 쿼리를 실행하고 변경된 레코드의 개수를 리턴한다.
     * @param sp_query 요청된 query
     * @param sp_Input IN 파리미터
     * @return result 변경된 레코드 개수
     * @throws SQLException
     */
    public static int update(Connection conn, String sp_query, Object[] sp_Input) throws SQLException {
        int result = -1;
        if (sp_query == null) {
            throw new SQLException("SQL Query가 NULL입니다.");
        }
        /* --------2011.9.5 transaction 처리를 위해서 ------------------- */
        boolean isInnerConnection = false;//일단은 외부 Connection 이용
        if(conn==null){ //없으면 내부 Connection 이용
            isInnerConnection = true; 
            conn = JdbcDao2.getConnection();
        }
        /* -------------------------------------------------------------- */
        
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sp_query);
            if (sp_Input != null) {
                for (int i = 0; i < sp_Input.length; i++) {
                    pstmt.setObject(i + 1, sp_Input[i]);
                }
            }
            result = pstmt.executeUpdate();
        } catch (SQLException ex) {
            BaseDebug.log(ex,  "SQL Query Update");
            throw ex;
        } finally {
            try {
                if (pstmt != null) {
                    //conn.commit();
                    pstmt.close();
                }
            } catch (SQLException ex) {
                BaseDebug.log(ex);
                throw ex;
            } finally {
                if (conn != null) {
                    /* -------------------------------------------------------------- */
                    if(isInnerConnection==true){//내부에서 만든 Connection만 제거한다.
                        JdbcDao2.endConnection(conn);
                    }/* -------------------------------------------------------------- */
                }
            }
        }
        return result;
    }

    

     /**
     * insert, update, delete 쿼리를 실행하고 변경된 레코드의 개수를 리턴한다.
     * commit 처리를 안하는 버전
     **/

    public static int updateNoCommit(Connection conn, String sp_query, Object[] sp_Input) throws SQLException {
        int result = -1;
        if (sp_query == null) {
            throw new SQLException("SQL Query가 NULL입니다.");
        }
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sp_query);
            if (sp_Input != null) {
                for (int i = 0; i < sp_Input.length; i++) {
                    pstmt.setObject(i + 1, sp_Input[i]);
                }
            }
            result = pstmt.executeUpdate();
        } catch (SQLException ex) {
            BaseDebug.log(ex);
            throw ex;
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                BaseDebug.log(ex);
                throw ex;
            }
        }
        return result;
    }

    
    

    /**
     * 주어진 쿼리와 파라미터를 이용하여 PreparedStatement 객체를 생성한 후 List 타입으로 결과값을 리턴한다.
     * @param sp_query 요청된 query
     * @param sp_Input IN 파리미터
     * @param mapper 사용자가 구현한 RowMapper
     * @return list list 객체
     * @throws SQLException
     */
    public static List queryForList(String sp_query, Object[] sp_Input, RowMapper mapper) throws SQLException {
        if (sp_query == null) {
            throw new SQLException("SQL Query가 NULL입니다.");
        }
        List list = new ArrayList();
        Connection conn = JdbcDao2.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sp_query);
            if (sp_Input != null) {
                for (int i = 0; i < sp_Input.length; i++) {
                    pstmt.setObject(i + 1, sp_Input[i]);
                }
            }
            boolean hasResults = pstmt.execute();
            rs = pstmt.getResultSet();
            while (rs.next()) {
                if (mapper != null) {
                    list.add(mapper.mapRow(rs));
                } else {
                    list.add(rs.getObject(1));
                }
            }
        } catch (SQLException e) {
            BaseDebug.log(e,  "catch:" + sp_query);
            throw e;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    //conn.commit();
                    pstmt.close();
                }
            } catch (SQLException ex) {
                BaseDebug.log(ex, "catch :" + sp_query);
                throw ex;
            } finally {
                if (conn != null) {
                    JdbcDao2.endConnection(conn);
                }
            }
        }
        return list;
    }

    public static ResultMap queryForMapObject(String sp_query, Object[] sp_Input) throws SQLException {
        List list = queryForMapList(sp_query, sp_Input);
        if (list.size() > 0) {
            return (ResultMap) list.get(0);
        } else {
            return null;
        }
    }

    public static List queryForMapList(String sp_query, Object[] sp_Input) throws SQLException {
        if (sp_query == null) {
            throw new SQLException("SQL Query가 NULL입니다.");
        }
        List list = new ArrayList();
        Connection conn = JdbcDao2.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sp_query);
            if (sp_Input != null) {
                for (int i = 0; i < sp_Input.length; i++) {
                    pstmt.setObject(i + 1, sp_Input[i]);
                }
            }
            boolean hasResults = pstmt.execute();
            rs = pstmt.getResultSet();
            /*----------------------------------------------------------------*/
            ResultSetMetaData rsMetaData = rs.getMetaData();
            while (rs.next()) {
                ResultMap map = new ResultMap();
                for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
                    map.put(rsMetaData.getColumnLabel(i), rs.getObject(i));
                }
                list.add(map);
            }
            /*----------------------------------------------------------------*/
        } catch (SQLException e) {
            BaseDebug.log(e, "catch:" + sp_query);
            throw e;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    //conn.commit();
                    pstmt.close();
                }
            } catch (SQLException ex) {
                BaseDebug.log(ex, "catch :" + sp_query);
                throw ex;
            } finally {
                if (conn != null) {
                    JdbcDao2.endConnection(conn);
                }
            }
        }
        return list;
    }

    /**
     * 주어진 쿼리를 이용하여 PreparedStatement 객체를 생성한 후 하나의 Object 타입 객체를 리턴하는 함수
     * @param sp_query 요청된 query
     * @return obj Object 객체
     * @throws SQLException
     */
    public static Object queryForObject(String sp_query, Object[] sp_Input, RowMapper mapper) throws SQLException {
        List list = queryForList(sp_query, sp_Input, mapper);
        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    /**
     * 주어진 쿼리와 파라미터를 이용하여 PreparedStatement 객체를 생성한 후 하나의 Object 타입 객체를 리턴하는 함수
     * @param sp_query 요청된 query
     * @return obj Object 객체
     * @throws SQLException
     */
    public static Object queryForObject(String sp_query, RowMapper mapper) throws SQLException {
        return queryForObject(sp_query, null, mapper);
    }

    /**
     * 주어진 쿼리를 이용하여 PreparedStatement 객체를 생성한 후 하나의 int형 결과값을 리턴하는 함수
     * @param sp_query - 요청된 query
     * @return resultInt - int형 결과값
     * @throws SQLException
     * @throws NumberFormatException
     */
    public static int queryForInt(String sp_query) throws SQLException, NumberFormatException {
        return queryForInt(sp_query, null);
    }

    /**
     * 주어진 쿼리와 파라미터값을 이용하여 PreparedStatement 객체를 생성한 후 하나의 int형 결과값을 리턴하는 함수
     * @param sp_query 요청된 query
     * @param sp_Input IN 파리미터
     * @return resultInt int형 결과값
     * @throws SQLException
     * @throws NumberFormatException
     */
    public static int queryForInt(String sp_query, Object[] sp_Input) throws SQLException, NumberFormatException {
        int resultInt = -1;
        Object obj = queryForObject(sp_query, sp_Input, null);
        if(obj!=null){
            try {
                resultInt = Integer.parseInt(obj.toString());
            } catch (NumberFormatException e) {
                BaseDebug.log(e);
            }
        }
        return resultInt;
    }

    /**
     * 주어진 쿼리와 파라미터값을 이용하여 PreparedStatement 객체를 생성한 후 하나의 String형 결과값을 리턴하는 함수
     * @param sp_query 요청된 query
     * @return obj.toString() String 객체
     * @throws SQLException
     */
    public static String queryForString(String sp_query) throws SQLException {
        return queryForString(sp_query, null);
    }

    /**
     * 주어진 쿼리와 파라미터값을 이용하여 PreparedStatement 객체를 생성한 후 하나의 String형 결과값을 리턴하는 함수
     * @param sp_query
     * @param sp_Input
     * @return obj.toString() String 객체
     * @throws SQLException
     */
    public static String queryForString(String sp_query, Object[] sp_Input) throws SQLException {
        Object obj = queryForObject(sp_query, sp_Input, null);
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }

    /**
     * 주어진 쿼리와 파라미터를 이용하여 CallableStatement 객체를 생성한 후 OUT  파라미터와 변경된 레코드 갯수를 리턴한다.
     * @param sp_query 요청된 query
     * @param sp_Input IN 파리미터
     * @param sp_Out OUT 파라미터
     * @return reuslt 변경된 레코드 갯수
     * @throws SQLException
     */
    public static int updateStored(Connection conn, String sp_query, Object[] sp_Input, Object[] sp_Out) throws SQLException {
        if (sp_query == null) {
            throw new SQLException("SQL Query가 NULL입니다.");
        }
        int result = -1;
        /* --------2011.9.5 transaction 처리를 위해서 ------------------- */
        boolean isInnerConnection = false;//일단은 외부 Connection 이용
        if(conn==null){ //없으면 내부 Connection 이용
            isInnerConnection = true; 
            conn = JdbcDao2.getConnection();
        }
        /* -------------------------------------------------------------- */
        CallableStatement cstmt = null;
        try {
            cstmt = conn.prepareCall(sp_query);
            if (sp_Input != null) {
                for (int i = 0; i < sp_Input.length; i++) {
                    cstmt.setObject(i + 1, sp_Input[i]);
                }
            }
            System.out.println(sp_query);
//            if (sp_Out != null) {
//                int pos = sp_Input.length; // IN 파라미더 이후 OUT 파라미터를 받기 위해 pos 이동
//                for (int i = 0; i < sp_Out.length; i++) {
//                    if (sp_Out[i] instanceof String) {
//                        System.out.println("String Procedue");
//                        cstmt.registerOutParameter(pos + i + 1, Types.VARCHAR);
//                    } else if (sp_Out[i] instanceof Integer) {
//                        System.out.println("Integer Procedue");
//                        cstmt.registerOutParameter(pos + i + 1, Types.INTEGER);
//                    } else if (sp_Out[i] instanceof Date) {
//                        System.out.println("Date Procedue");
//                        cstmt.registerOutParameter(pos + i + 1, Types.DATE);
//                    } else{
//                        System.out.println("Nothing Procedue");
//                    }
//                }
//            }
            result = cstmt.executeUpdate();

            if (sp_Out != null) {
                int pos = sp_Input.length; // IN 파라미터 이후 OUT 파라미터를 받기 위해 pos 이동
                for (int i = 0; i < sp_Out.length; i++) {
                    sp_Out[i] = cstmt.getObject(pos + i + 1);
                }
            }

        } catch (SQLException ex) {
            BaseDebug.log(ex);
            throw ex;
        } finally {
            try {
                if (cstmt != null) {
                    //conn.commit();
                    cstmt.close();
                }
            } catch (SQLException ex) {
                BaseDebug.log(ex);
                throw ex;
            } finally {
                if (conn != null) {
                    /* -------------------------------------------------------------- */
                    if(isInnerConnection==true)//내부에서 만든 Connection만 제거한다.
                        JdbcDao2.endConnection(conn);
                    /* -------------------------------------------------------------- */
                }
            }
        }
        return result;
    }

    /**
     * 주어진 쿼리와 파라미터를 이용하여 CallableStatement 객체를 생성한 후 변경된 레코드 갯수를 리턴한다.
     * @param sp_query 요청된 query
     * @param sp_Input IN 파리미터
     * @return result 변경된 레코드 갯수
     * @throws SQLException
     */
    public static int updateStored(String sp_query, Object[] sp_Input) throws SQLException {
        return updateStored(null, sp_query, sp_Input, null);
    }
    
    public static int updateStored(String sp_query, Object[] sp_Input, Object[] sp_Out) throws SQLException {
        return updateStored(null, sp_query, sp_Input, sp_Out);
    }

    /**
     * 주어진 쿼리와 파라미터를 이용하여 CallableStatement 객체를 생성한 후 List 타입 객체를 리턴한다.
     * @param sp_query 요청된 query
     * @param sp_Input IN 파리미터
     * @param mapper 사용자가 구현한 RowMapper
     * @return list List 객체
     * @throws SQLException
     */
    public static List queryForListStored(String sp_query, Object[] sp_Input, RowMapper mapper) throws SQLException {
        return JdbcDao2.queryForListStoredOutParam(sp_query, sp_Input, null, mapper);
    }

    /**
     * 주어진 쿼리와 파라미터를 이용하여 CallableStatement 객체를 생성한 후 List 타입 객체를 리턴한다.
     * @param sp_query 요청된 query
     * @param sp_Input IN 파리미터
     * @param mapper 사용자가 구현한 RowMapper
     * @return list List 객체
     * @throws SQLException
     */
    public static List queryForListStoredOutParam(String sp_query, Object[] sp_Input, Object[] sp_Out, RowMapper mapper) throws SQLException {
        if (sp_query == null) {
            throw new SQLException("SQL Query가 NULL입니다.");
        }
        List list = new ArrayList();
        Connection conn = JdbcDao2.getConnection();
        CallableStatement cstmt = null;
        ResultSet rs = null;
        try {
            cstmt = conn.prepareCall(sp_query);
            if (sp_Input != null) {
                for (int i = 0; i < sp_Input.length; i++) {
                    cstmt.setObject(i + 1, sp_Input[i]);
                }
            }
            if (sp_Out != null) {
                int pos = sp_Input.length; // IN 파라미더 이후 OUT 파라미터를 받기 위해 pos 이동
                for (int i = 0; i < sp_Out.length; i++) {
                    if (sp_Out[i] instanceof String) {
                        cstmt.registerOutParameter(pos + i + 1, Types.VARCHAR);
                    } else if (sp_Out[i] instanceof Integer) {
                        cstmt.registerOutParameter(pos + i + 1, Types.INTEGER);
                    } else if (sp_Out[i] instanceof Date) {
                        cstmt.registerOutParameter(pos + i + 1, Types.DATE);
                    } 
                }
            }

            boolean hasResults = cstmt.execute();
            rs = cstmt.getResultSet();
            while (rs.next()) {
                list.add(mapper.mapRow(rs));
            }
            if (sp_Out != null) {
                int pos = sp_Input.length; // IN 파라미터 이후 OUT 파라미터를 받기 위해 pos 이동
                for (int i = 0; i < sp_Out.length; i++) {
                    sp_Out[i] = cstmt.getObject(pos + i + 1);
                }
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (cstmt != null) {
                    //conn.commit();
                    cstmt.close();
                }
            } catch (SQLException ex) {
                throw ex;
            } finally {
                if (conn != null) {
                    JdbcDao2.endConnection(conn);
                }
            }
        }
        return list;
    }

    /**
     * 주어진 쿼리와 파라미터를 이용하여 CallableStatement 객체를 생성한 후 Object 타입 객체를 리턴한다.
     * @param sp_query 요청된 query
     * @param sp_Input IN 파리미터
     * @param mapper 사용자가 구현한 RowMapper
     * @return list List 객체
     * @throws SQLException
     */
    public static Object queryForObjectStored(String sp_query, Object[] sp_Input, RowMapper mapper) throws SQLException {
        List list = JdbcDao2.queryForListStored(sp_query, sp_Input, mapper);
        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    /**
     * 주어진 쿼리를 CallableStatement 객체를 생성한 후 Object 타입 객체를 리턴한다.
     * @param sp_query 요청된 query
     * @param mapper 사용자가 구현한 RowMapper
     * @return list List 객체
     * @throws SQLException
     */
    public static Object queryForObjectStored(String sp_query, RowMapper mapper) throws SQLException {
        return queryForObjectStored(sp_query, null, mapper);
    }

    public static ResultMap queryForMapObjectStored(String sp_query, Object[] sp_Input, Object[] sp_Out) throws SQLException{
        List list = queryForMapListStored(sp_query, sp_Input, sp_Out);
        if(list.size()>0){
            return (ResultMap)list.get(0);
        }else{
            return null;
        }
    }
    public static List queryForMapListStored(String sp_query, Object[] sp_Input, Object[] sp_Out) throws SQLException{
        if (sp_query == null) {
            throw new SQLException("SQL Query가 NULL입니다.");
        }
        List list = new ArrayList();
        Connection conn = JdbcDao2.getConnection();
        CallableStatement cstmt = null;
        ResultSet rs = null;
        try {
            cstmt = conn.prepareCall(sp_query);
            if (sp_Input != null) {
                for (int i = 0; i < sp_Input.length; i++) {
                    cstmt.setObject(i + 1, sp_Input[i]);
                }
            }
            if (sp_Out != null) {
                int pos = sp_Input.length; // IN 파라미더 이후 OUT 파라미터를 받기 위해 pos 이동
                for (int i = 0; i < sp_Out.length; i++) {
                    if (sp_Out[i] instanceof String) {
                        cstmt.registerOutParameter(pos + i + 1, Types.VARCHAR);
                    } else if (sp_Out[i] instanceof Integer) {
                        cstmt.registerOutParameter(pos + i + 1, Types.INTEGER);
                    } else if (sp_Out[i] instanceof Date) {
                        cstmt.registerOutParameter(pos + i + 1, Types.DATE);
                    } 
                }
            }

            boolean hasResults = cstmt.execute();
            rs = cstmt.getResultSet();
            /*----------------------------------------------------------------*/
            ResultSetMetaData rsMetaData = rs.getMetaData();
            while (rs.next()) {
                ResultMap map = new ResultMap();
                for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
                    map.put(rsMetaData.getColumnLabel(i), rs.getObject(i));
                }
                list.add(map);
            }
            /*----------------------------------------------------------------*/
            if (sp_Out != null) {
                int pos = sp_Input.length; // IN 파라미터 이후 OUT 파라미터를 받기 위해 pos 이동
                for (int i = 0; i < sp_Out.length; i++) {
                    sp_Out[i] = cstmt.getObject(pos + i + 1);
                }
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (cstmt != null) {
                    //conn.commit();
                    cstmt.close();
                }
            } catch (SQLException ex) {
                throw ex;
            } finally {
                if (conn != null) {
                    JdbcDao2.endConnection(conn);
                }
            }
        }
        return list;
    }
}
