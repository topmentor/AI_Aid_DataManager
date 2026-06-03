package com.ithows;


public class CommonUtils {


    public static void Sleep(double sec) {
        try {
            Thread.sleep((int)(sec * 1000));
        } catch (Exception e) {
            
        }
    }
}
