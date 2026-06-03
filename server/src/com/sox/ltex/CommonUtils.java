/*
 *  Copyright 2020. S.O.X. All rights reserved
 */

package com.sox.ltex;

/**
 * Class CommonUtils
 * 
 * @author Roi Kim <S.O.X Co. Ltd.>
 */
public class CommonUtils {
    public static void sleep(double sec){

        try {
            Thread.sleep((long) (sec * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
