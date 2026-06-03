/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows;

/**
 *
 * @author dreamct
 */
public class DirGenerator {

    private static int TOTAL = 62;
    private static char[] mapper = new char[]{48, 49, 50, 51, 52, 
                                     53, 54, 55, 56, 57, 
                                     65, 66, 67, 68, 69,
                                     70, 71, 72, 73, 74,
                                     75, 76, 77, 78, 79,
                                     80, 81, 82, 83, 84, 
                                     85, 86, 87, 88, 89,
                                     90, 97, 98, 99, 100,
                                     101, 102, 103, 104, 105, 
                                     106, 107, 108, 109, 110,
                                     111, 112, 113, 114, 115, 
                                     116, 117, 118, 119, 120,
                                     121, 122};

    private static char[] specialKey = new char[]{'S', 'M', 'T', 'H', 'O', 'W', 'S'};
    private static char[] specialKey2 = new char[]{'d', 'l', 't', 'k', 'd', 'g', 'k'};

    private static String getKey(char[] keys, int ref) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            int pos = (keys[i] * ref) % TOTAL;
            sb.append(mapper[pos]);
        }
        return sb.toString();
    }
    
    public static String getAutoLoginKey(int ref){
        return getKey(specialKey2, ref);
    }
    
    public static String getAutoLoginKey(String keys){
        return getKey(keys.toCharArray(), 1189);
    }
    
    public static void main(String[] args) {
        //System.out.println(DirGenerator.getAutoLoginKey("abcdefg"));
        //System.out.println(DirGenerator.getNomuDir(3));
        //System.out.println(DirGenerator.getExcelFile(3, (int)(System.currentTimeMillis()%1000)));
    }
}