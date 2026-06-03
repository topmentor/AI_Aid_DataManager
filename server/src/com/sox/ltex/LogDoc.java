/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex;

import com.ithows.service.UploadConst;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mailt
 */
public class LogDoc {
     public static PrintStream logWriter = null;
//     public static String logPath ="D:\\00_project2022\\SSF2026\\build\\web\\resultLog\\"; // @@ 컨텍스트 경로 - 로컬 서버
//    public static String logPath = "C:\\Tomcat9\\webapps\\SSF2026\\resultLog\\";;   // @@ 컨텍스트 경로 - etri 서버
    public static String logPath ="C:\\Tomcat9_2\\webapps\\SSF2026\\resultLog\\";   // @@ 컨텍스트 경로 - 서울 서버
    public static String fileName = "";
     
    static {
        logPath = UploadConst.getResultLogFileDir() ;  
    }
     
     public static PrintStream getLogStream(){
         return logWriter;
     }
     
     public static PrintStream initLog(String filename){
         fileName = filename;
         
         try {
             logWriter =  new PrintStream(new FileOutputStream(new File(logPath + fileName), true));
         } catch (Exception ex) {
             System.out.println(ex.getLocalizedMessage());
             fileName = "";
             logWriter = null;
         }
         
         return logWriter;
     }
     
         
    public static void printlog(String str) {
        printlog(str, false);
    }
    
    public static void printlog(String str, boolean stdout) 
    {
        if(stdout) System.out.println(str);

        if (logWriter != null) {
            try {
                logWriter.println(str);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void close() 
    {

        if (logWriter != null) {
            try {
                logWriter.close();
                logWriter = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
