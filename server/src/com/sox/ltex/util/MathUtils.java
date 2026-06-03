/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sox.ltex.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author mailt
 */
public class MathUtils {
    public static boolean isEqualRounded(double a, double b, int scale) {
        BigDecimal bdA = new BigDecimal(a).setScale(scale, RoundingMode.HALF_UP);
        BigDecimal bdB = new BigDecimal(b).setScale(scale, RoundingMode.HALF_UP);
        return bdA.equals(bdB);
    }
    
}
