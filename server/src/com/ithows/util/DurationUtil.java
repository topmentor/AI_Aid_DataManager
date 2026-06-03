/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ithows.util;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author mailt
 */
public class DurationUtil {
    
    private String id = "";
    public String startTime = "";
    public String endTime = "";

    private long sTime = 0;
    private long eTime = 0;
    private long durationTime = 0;

    private boolean consoleOn = true;

    private String durationStr = "";

    private StringBuffer bufLog = new StringBuffer();

    private ArrayList<HashMap<String, Object>> checkTimeList = new ArrayList<HashMap<String, Object>>();
    
    
    private String logPath = "./";
    

    public DurationUtil(){
        startTime = DateTimeUtils.getTimeDateNow2();
        sTime = System.currentTimeMillis();
        id = KeyGenerator.createNormalKey(6);

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("time" , sTime);
        map.put("duration" , 0L);
        map.put("comment" , "Start - " + id);
        map.put("subtext" , id);
        checkTimeList.add(map);

        writelnLog("//////////////////////////////////////////////////////// ");
        writelnLog("// " + id );
        writelnLog("\nstart : " + startTime + " [ " + this.id + " ] \n\n");
    }

    public DurationUtil(String processName, String lPath){
        startTime = DateTimeUtils.getTimeDateNow();
        sTime = System.currentTimeMillis();
        id = processName;
     
        if(!lPath.isEmpty()){
            logPath = lPath;
        }

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("time" , sTime);
        map.put("duration" , 0L);
        map.put("comment" , "Start - " + processName);
        map.put("subtext" , processName);
        checkTimeList.add(map);


        writelnLog("//////////////////////////////////////////////////////// ");
        writelnLog("// " + id );
        writelnLog("\nstart : " + startTime + " [ " + this.id + " ] \n\n");

    }

    public long getDurationTime() {
        return durationTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String stop(){
        return stop("End " , "");
    }

    public String stop(String tag, String subText){

        if(tag == null || tag.equals("")){
            tag = "last process";
        }
        check(tag, subText);

        endTime = DateTimeUtils.getTimeDateNow();
        eTime = System.currentTimeMillis();

        durationTime = eTime - sTime;
        durationStr = DateTimeUtils.getTimeString(durationTime);

        writelnLog("\n\n[ " + this.id + " ]  entire end ------------------------------" );
        writelnLog("time :  " + startTime + " ~ " + endTime );
        writelnLog("total duration :  " + durationStr );
        writelnLog("//////////////////////////////////////////////////////// \n\n");

        if(!consoleOn){
            System.out.println("duration = " + durationStr );
        }

        setDurationPercent();

        return durationStr;
    }

    // 구간 duration check
    public String check(String tag, String subText){
        
        String checkTime = DateTimeUtils.getTimeDateNow();
        Long nowTime = System.currentTimeMillis();
        Long preTime = Long.parseLong((checkTimeList.get(checkTimeList.size()-1)).get("time").toString());


        Long duTime = nowTime - preTime;
        String duStr = DateTimeUtils.getTimeString(duTime);

        durationTime = nowTime - sTime;

        writelnLog("check - " + checkTimeList.size() + " [ " + tag + " ] --------");
        writelnLog("info : " + subText );
        writelnLog("time : " + checkTime );
        writelnLog("duration : " + duStr );
        writelnLog("----------------------------------");


        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("time" , nowTime);
        map.put("duration" , duTime);
        map.put("comment" , tag);
        map.put("subtext" , subText);
        checkTimeList.add(map);

        return duStr;
    }


    private void setDurationPercent(){

        for(int i=0; i<checkTimeList.size(); i++){
            HashMap<String, Object> map = checkTimeList.get(i);
            long time = Long.parseLong(map.get("duration").toString());
            double percent = (double)time / (double)durationTime * 100;
            map.put("percent", percent);
        }

        writelnLog("////////////////////////////////////////////////////////");
        writelnLog("total duration :  " + durationStr );

        for(int i=0; i<checkTimeList.size(); i++){
            HashMap<String, Object> map = checkTimeList.get(i);
            writelnLog("" + i + " [ " + map.get("comment").toString() + " ] " + DateTimeUtils.getTimeString(Long.parseLong(map.get("duration").toString())) + "  -->  " + String.format("%.2f", map.get("percent")) + "%");
        }
        writelnLog("////////////////////////////////////////////////////////");
    }

    public void setConsoleOn(boolean onoff){
        consoleOn = onoff;
    }

    public void writeLog(String logStr){
        bufLog.append(logStr);

        if(consoleOn)
            System.out.print(logStr);
    }
    public void writelnLog(String logStr){
        bufLog.append(logStr + "\n");

        if(consoleOn)
            System.out.println(logStr);
    }

    public boolean saveLogFile(String userLogs){

        // @@ 로그 파일명 수정
        String logFile =  logPath + "log_" + this.id + "_" + DateTimeUtils.getTimeDateNow2() + ".log"  ;
        
        if(!userLogs.isEmpty()){
            logFile =  logPath + "log_" + this.id + "_" + userLogs + "_" + DateTimeUtils.getTimeDateNow2() + ".log"  ;
            
        }else{
            bufLog.append("\n" + userLogs + "\n");
        }


        return writeJsonTextToFile(bufLog.toString(), logFile);
    }

    private static boolean writeJsonTextToFile(String saveStr, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.append(saveStr);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


}
