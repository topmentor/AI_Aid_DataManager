/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows;

import java.util.Date;

/**
 *
 * @author dreamct
 */
public class BaseDebug {
    
   
    public static void info(Object... msgs) {
        System.out.println("Debug INFO " + new Date().toString() + "   ");
        for (Object obj : msgs) {
            System.out.print(obj.toString() + ",");
        }
        System.out.println();
    }
    public static void log(Exception e, Object... msgs) {
        System.out.print("LOG START " + new Date().toString());
        e.printStackTrace();
        System.out.print(" Method Name: ");
        System.out.println(e.getStackTrace()[0].getMethodName());
        for (Object obj : msgs) {
            System.out.print(obj.toString() + ", ");
        }
        System.out.println("LOG END ");
    }
}
