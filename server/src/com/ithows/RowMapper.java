/**
 * 데이터베이스의 ResultSet을 처리하기 위한 추상 클래스 
 */

package com.ithows;


public abstract class RowMapper {

    public Object mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        throw new java.sql.SQLException();
    }
}
