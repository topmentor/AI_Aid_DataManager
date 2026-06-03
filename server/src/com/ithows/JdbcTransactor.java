/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author dreamct
 */
public interface JdbcTransactor {    
    void doTransaction(Connection conn)  throws SQLException;    
}
