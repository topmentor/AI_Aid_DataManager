/*
 *  Copyright 2020. S.O.X. All rights reserved
 */

package com.ithows.util;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sox.ltex.ProcessCall;

/**
 * Class DevUtils
 * 소스 코드 라인 카운트
 * @author Roi Kim <S.O.X Co. Ltd.>
 */
public class DevUtils {
    
    public static void main(String[] args) {
        try {
            checkLineCount();
        } catch (Exception ex) {
            Logger.getLogger(DevUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    /**
     * 라인수 저장 
     *  find /V /C C:\00_Project_Biz\ETRI2020\SSF2026\src\com\ithows\*.java ""
     * find . -name '*.java' | xargs wc -l
     * @throws Exception 
     */
    public static void checkLineCount() throws Exception{
        String projectPath = "C:\\01_project\\SSF2026\\";
        String[] jspPath = new String[5];
        jspPath[0] = "web\\WEB-INF\\jsp\\";
        jspPath[1] = "web\\WEB-INF\\jsp\\api";
        jspPath[2] = "web\\WEB-INF\\jsp\\data\\";
        jspPath[3] = "web\\WEB-INF\\jsp\\service\\";
        jspPath[4] = "web\\WEB-INF\\jsp\\admin\\";
        
        String[] jsPath = new String[3];
        jsPath[0] = "web\\js\\";
        jsPath[1] = "web\\js\\player\\sox\\";
        jsPath[2] = "web\\js\\player\\";
        
        String[] javaPath = new String[8];
        javaPath[0] = "src\\com\\ithows\\dao\\";
        javaPath[1] = "src\\com\\ithows\\controller\\";
        javaPath[2] = "src\\com\\ithows\\service\\";
        javaPath[2] = "src\\com\\ithows\\util\\";
        javaPath[3] = "src\\org\\etri\\locationdb\\";
        javaPath[4] = "src\\org\\etri\\locationdb\\util\\";
        javaPath[5] = "src\\org\\etri\\locationdb\\util\\shape\\";
        javaPath[5] = "src\\org\\etri\\locationdb\\ltematch\\";
        
        ArrayList<String> outputArray = null;
        String outfileName = "C:\\Users\\mailt\\OneDrive\\바탕 화면\\linecount_log.txt";
        PrintStream out = new PrintStream(new File(outfileName));

        int jspCount = 0;
        int jsCount = 0;
        int javaCount = 0;
        
        String[] command = {"cmd.exe", "/c", "dir ."};
        
        for(int i=0; i < jspPath.length; i++){
            command[2] = "find /V /C " + projectPath + jspPath[i] + "*.jsp \"\" ";
            outputArray = ProcessCall.normalCallCommand(command);
            
            for(String str : outputArray){
                if(!str.equals("")){
                    str = str.replaceAll("---------- ", "");
                    out.println(str);
                    String[] element = str.split(":");
                    jspCount += Integer.parseInt(element[2].trim());
                }
            }
        }
        
        for(int i=0; i < jsPath.length; i++){
            command[2] = "find /V /C " + projectPath + jsPath[i] + "*.js \"\" ";
            outputArray = ProcessCall.normalCallCommand(command);
            
            for(String str : outputArray){
               if(!str.equals("")){
                    str = str.replaceAll("---------- ", "");
                    out.println(str);
                    String[] element = str.split(":");
                    jsCount += Integer.parseInt(element[2].trim());
                }
            }
        }
        
        
        for(int i=0; i < javaPath.length; i++){
            command[2] = "find /V /C " + projectPath + javaPath[i] + "*.java \"\" ";
            outputArray = ProcessCall.normalCallCommand(command);
            
            for(String str : outputArray){
                if(!str.equals("")){
                    str = str.replaceAll("---------- ", "");
                    out.println(str);
                    String[] element = str.split(":");
                    javaCount += Integer.parseInt(element[2].trim());
                }
            }
        }
        
        out.println("\r\n\r\n \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ \r\n" );
        out.println("Total Lines : " + (javaCount + jspCount + jsCount) );
        out.println("Java Lines : " + javaCount);
        out.println("JSP Lines : " + jspCount);
        out.println("JS Lines : " + jsCount);
        
        out.close();
        
        
    }
    
}
