/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

import com.ithows.service.ProcessCall;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 *
 * @author ksyuser
 */
public class DateTimeUtils {

    
    /**
     * Hours per day.
     */
    public static final int HOURS_PER_DAY = 24;
    /**
     * Minutes per hour.
     */
    public static final int MINUTES_PER_HOUR = 60;
    /**
     * Minutes per day.
     */
    public static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;
    /**
     * Seconds per minute.
     */
    public static final int SECONDS_PER_MINUTE = 60;
    /**
     * Seconds per 10minute.
     */
    public static final int SECONDS_PER_10MINUTES = 600;
    /**
     * Seconds per hour.
     */
    public static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    /**
     * Seconds per day.
     */
    public static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;


    /**
     *      날짜 관련 주요 함수
     *         Calendar cal = Calendar.getInstance();
     *         int year = cal.get(Calendar.YEAR);
     *         int mon = cal.get(Calendar.MONTH);
     *         int day = cal.get(Calendar.DAY_OF_MONTH);
     *         int hour = cal.get(Calendar.HOUR_OF_DAY);
     *         int min = cal.get(Calendar.MINUTE);
     *         int sec = cal.get(Calendar.SECOND);
     *
     */

    
    /**
     * 시간 재기 기능
     *  Date start_time = new Date(System.currentTimeMillis());
     *  System.out.println("total time = " + DateTimeUtils.getTimeDifferenceNow(start_time));  
     */
    
    
    /**
     * dateformat
     * yyyy/MM/dd/HH:mm:ss
     * YYYY-MM-dd HH-mm-ss
     * yyyy-MM-dd HH:mm:ss
     * yyyyMMdd_HHmmss
     */

    public static final String FORMAT_DATETIME_SLASHDATETIME = "yyyy/MM/dd/HH:mm:ss";
    public static final String FORMAT_DATETIME_SLASHDATEONLY = "yyyy/MM/dd";
    public static final String FORMAT_DATETIME_COLONTIMEONLY = "HH:mm:ss";
    public static final String FORMAT_DATETIME_DASHEDATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT_DATETIME_DASHEDATETIME2 = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String FORMAT_DATETIME_DASHEDATEONLY = "yyyy-MM-dd";
    public static final String FORMAT_DATETIME_DASHETIMEONLY = "HH-mm-ss";
    public static final String FORMAT_DATETIME_NOSPACEDATETIME = "yyyyMMdd_HHmmss";
    public static final String FORMAT_DATETIME_NOSPACEDATEONLY = "yyyyMMdd";
    public static final String FORMAT_DATETIME_NOSPACETIMEONLY = "HHmmss";



    /**
     * 시간객체와 시간문자열간 변환 함수
     * @param date
     * @return
     */


    public static String getStringFromDateObj(Date date, String inputFormat){
        DateFormat df = new SimpleDateFormat(inputFormat);
        return df.format(date);
    }

    public static String getStringFromDateTime(Date date){
        return getStringFromDateObj(date, FORMAT_DATETIME_SLASHDATETIME) ; // "yyyy/MM/dd/HH:mm:ss"
    }

    public static String getStringFromDate(Date date){
        return getStringFromDateObj(date, FORMAT_DATETIME_SLASHDATEONLY );  // "yyyy/MM/dd"
    }

    public static String getStringFromDate2(Date date){
        return getStringFromDateObj(date, FORMAT_DATETIME_DASHEDATEONLY);  // "yyyy-MM-dd"
    }

    public static String getStringFromTime(Date date){
        return getStringFromDateObj(date, FORMAT_DATETIME_COLONTIMEONLY); // "HH:mm:ss"
    }

    public static Date getDateFromString(String timeStr, String inputFormat){
        Date date ;
        try {
            DateFormat df = new SimpleDateFormat(inputFormat);
            date = df.parse(timeStr);


        } catch (Exception e) {
            System.out.println("Exception :" + e);
            return null;
        }
        return date;
    }

    // 이 함수를 이용하면 시간, 분, 초를 정각화 해서 처리가 가능
    public static Date getFormatDate(Date timeObj, String inputFormat){
        Date date ;
        String timeStr = "";

        try {
            DateFormat df2 = new SimpleDateFormat(inputFormat);
            timeStr = df2.format(timeObj);
            date = df2.parse(timeStr);

        } catch (Exception e) {
            System.out.println("Exception :" + e);
            return null;
        }
        return date;
    }

    
    public static String convertTimestampToDate(long timestamp) {
        if(timestamp < 10000000000L){
            timestamp *= 1000L;
        }
        Date date = new java.util.Date(timestamp); 
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+9")); 
        String formattedDate = sdf.format(date);
        return formattedDate;
    }
    
    public static String convertTimestampToDateNumber(long timestamp) {
        if(timestamp < 10000000000L){
            timestamp *= 1000L;
        }
        Date date = new java.util.Date(timestamp); 
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss"); 
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+9")); 
        String formattedDate = sdf.format(date);
        return formattedDate;
    }
    
    public static String convertUnixTimeToDate(long timestamp) {
        if(timestamp < 10000000000L){
            timestamp *= 1000L;
        }
        Date date = new java.util.Date(timestamp); 
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+9")); 
        String formattedDate = sdf.format(date);
        return formattedDate;
    }
    
    public static long convertDateToUnixTime(Date dTime) {
        long epoch = dTime.getTime();
        return epoch;
    }
    
    public static long convertDateToUnixTime2(Date dTime) {
        // Date dt = sdf.parse(timestamp);
        long epoch = dTime.getTime();
        return (long)(epoch/1000);
    }
    
    
    
      

    public static long convertTimeStringToUnixTime(String timeStr, String inputFormat){
       Date date ;
       long epoch = -1;
        try {
            DateFormat df = new SimpleDateFormat(inputFormat);
            date = df.parse(timeStr);
            epoch = date.getTime();

        } catch (Exception e) {
            System.out.println("Exception :" + e);
        }
        return epoch ;
    }

   
    
   // Time String의 포맷만 바꿈    
    public static String convertFormat(String timeStr, String inputFormat, String outFormat){
       Date date ;
        try {
            DateFormat df = new SimpleDateFormat(inputFormat);
            date = df.parse(timeStr);

        } catch (Exception e) {
            System.out.println("Exception :" + e);
            return "";
        }
        return getStringFromDateObj(date, outFormat) ;
    }




    /**
     * 특정 시간에서 초단위로 더하거나 뺌
     * @param date : 기준 시간
     * @param sec : 더하는 시간(초). 음수값이면 빼는 효과
     * @return
     */
    public static Date getAfterSecondTime(Date date, int sec) {

        Date later = null;
        Calendar cal = Calendar.getInstance();

        cal.setTime(date);
        cal.add(Calendar.SECOND, sec);
        later = cal.getTime();

        return later;
    }

    public static Date getAfterSecondTime(String str_date, String inputFormat, int sec) {

        Date later = null;
        Date date = null;

        try {
            DateFormat df = new SimpleDateFormat(inputFormat);
            date = df.parse(str_date);
            later = getAfterSecondTime(date, sec);

        } catch (Exception e) {
            System.out.println("Exception :" + e);
            return null;
        }
        return later;
    }

    /**
     * 특정 시간 정보만 받아 옴
     * @param dateStr
     * @param inputFormat
     * @param valueOption  : Calendar.SECOND  캘린더 상수 사용
     * @return
     */
    public static int getOnlyTimeValue(String dateStr, String inputFormat, int valueOption){

        DateFormat df = new SimpleDateFormat(inputFormat);
        Date date;
        try {
            date = df.parse(dateStr);

        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }

        return getOnlyTimeValue(date, valueOption);
    }

    public static int getOnlyTimeValue(Date date, int valueOption){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return getOnlyTimeValue(calendar, valueOption);
    }

    public static int getOnlyTimeValue(Calendar cal, int valueOption){
        return cal.get(valueOption);
    }




    /**
     * 현재 시간 얻기
     * @return
     *
     * 현재 시간 구하기 방법 들
     * Calendar calendar = Calendar.getInstance(); // gets current instance of the calendar
     * Date date = new Date(); // this object contains the current date value
     * System.currentTimeMillis();   Date date = new Date(System.currentTimeMillis());
     * LocalDate date = LocalDate.now(); // gets the current date
     * LocalTime time = LocalTime.now(); // gets the current time
     * LocalDateTime dateTime = LocalDateTime.now(); // gets the current date and time    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
     */

    public static String getDateTimeFull() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH-mm-ss");

        return sdf.format(cal.getTime());
    }

    public static String getDateTime() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("(MM-dd HH:mm:ss)");

        return sdf.format(cal.getTime());
    }


    public static String getOnlyTimeNow(){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        String timeValue = sdf.format(date);

        return timeValue;
    }


    public static String getTimeDateNow(String formatStr){

        if(formatStr.equals("")){
            formatStr = FORMAT_DATETIME_DASHEDATETIME ;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(formatStr);
        Date date = new Date();
        String timeValue = sdf.format(date);

        return timeValue;
    }

    public static String getTimeDateNow(){   // yyyy-MM-dd HH:mm:ss

        return getTimeDateNow(FORMAT_DATETIME_DASHEDATETIME);
    }

    public static String getRegiterTime(){   // yyyy-MM-dd HH:mm:ss.SSS

        return getTimeDateNow(FORMAT_DATETIME_DASHEDATETIME2);
    }


    // yyyyMMdd_HHmmss
    public static String getTimeDateNow2(){  // yyyyMMdd_HHmmss
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date();
        String timeValue = sdf.format(date);

        return timeValue;
    }

    public static long getTimestampNow(){
        Date date = new Date();
        return date.getTime();
    }

    
    

    /**
     * 현재의 Calendar에서 마지막날 구하기
     * @param cal
     * @return
     */
    public static String getMaximumDate(Calendar cal){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        Calendar max = Calendar.getInstance(Locale.KOREA);
        max.setTime(cal.getTime());
        max.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        return dateFormat.format(max.getTime());
    }

    public static String updateDeviceDateTime(String newTime){
        String OS = System.getProperty("os.name").toLowerCase();
        String[] commandLine = new String[3];
        String lineStr;

        if(OS.indexOf("win") >= 0){
            return "";
        }

        commandLine[0] = "/bin/sh";
        commandLine[1] = "-c";
        commandLine[2] = "sudo date --set '" + newTime + "'";
//        commandLine[2] = "sudo date +\"%Y%m%d %T\" -s \"" + newTime + "\"";

        ArrayList<String> output = ProcessCall.normalCallCommand(commandLine);
        String outputStr = "";

        for (int i = 1; i < output.size(); i++) {
            lineStr = output.get(i).trim();
            if(lineStr.equals("") ){
                continue;
            }
            outputStr += lineStr + "\n";
        }

        return outputStr;
    }


    public final static int INTERVAL_SECOND = 0;
    public final static int INTERVAL_MINUTE = 1;
    public final static int INTERVAL_HOUR = 2;

    /**
     * 특정 시간부터 자정까지의 시간
     * @param dateStr : ""이면 현재 부터
     * @param option
     * @return
     */
    public static int getIntervalToMidnight(String dateStr, int option){

        LocalTime now = null;

        if(dateStr.equals("")){
            now = LocalTime.now(ZoneId.systemDefault()) ;// LocalTime = 14:42:43.062

        }else{
            DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss");
            now = LocalTime.parse(dateStr, dt);
        }

        if(option == INTERVAL_MINUTE){
            return MINUTES_PER_DAY - (now.toSecondOfDay() / SECONDS_PER_MINUTE );
        }else if(option == INTERVAL_HOUR){
            return HOURS_PER_DAY - (now.toSecondOfDay() / SECONDS_PER_HOUR );// Int = 52963
        }

        return SECONDS_PER_DAY - now.toSecondOfDay();// Int = 52963
    }

    // 정각 까지의 시간 (분, 초)
    public static int getTimeToZero(Date date, int valueOption){

        Calendar oCalendar = Calendar.getInstance(Locale.KOREA);
        oCalendar.setTime(date);
        int minutes = oCalendar.get(Calendar.MINUTE);
        int seconds = oCalendar.get(Calendar.SECOND);

        if(valueOption == Calendar.SECOND){
            return 3600 - (minutes * 60 + seconds);
        }else if(valueOption == Calendar.MINUTE){
            return 60 - minutes ;
        }
        return 0;
    }

    public static String getTimeDifferenceNow(Date startTime){
        Date nowTime = new Date(System.currentTimeMillis());
        return getTimeDiff(nowTime, startTime);
    }
    
    public static long getTimeDifferenceNow(long t1){
        return t1 - System.currentTimeMillis();
    }

    public static float getTimeDifference(String later, String before, int option) {  // option이 0이면 초단위, 1이면 분단위, 2는 시단위

        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        Date d1Date = null;
        Date d2Date = null;

        try {
            d1Date = dt.parse(before);
            d2Date = dt.parse(later);

        }catch (Exception e){
            e.printStackTrace();
            return 0.0f;
        }
        return getTimeDiff(d2Date, d1Date, option);
    }

    public static float getTimeDiff(Date later, Date before, int option) {  // option은 Calendar.HOUR 상수로 지정
        float result = 0;
        Calendar cal = Calendar.getInstance();
        cal.setTime(before);

        long t1 = cal.getTimeInMillis();
        cal.setTime(later);

        float diff = cal.getTimeInMillis() - t1;

        final int ONE_DAY = 1000 * 60 * 60 * 24;
        final int ONE_HOUR = ONE_DAY / 24;
        final int ONE_MINUTE = ONE_HOUR / 60;
        final int ONE_SECOND = 1000;


        if (option == Calendar.HOUR ) {
            result = diff / ONE_HOUR;
        } else if(option == Calendar.MINUTE ) {
            result = diff / ONE_MINUTE;
        }  else if(option == Calendar.SECOND ) {
            result = diff / ONE_SECOND;
        }

        return result;
    }

    
    public static String getTimeDiff(Date later, Date before) {

        Calendar cal = Calendar.getInstance();
        cal.setTime(before);

        long t1 = cal.getTimeInMillis();
        cal.setTime(later);

        long diff = cal.getTimeInMillis() - t1;

        String result = getTimeString(diff);

        return result;
    }

    /**
     * 밀리세컨드를 시간으로 변환
     * @param milliseconds
     * @return
     */
    public static String getTimeString(long milliseconds) {
        long days = milliseconds / (long)86400000;
        long remainder = milliseconds % (long)86400000;
        long hours = remainder / (long)3600000;
        remainder %= (long)3600000;
        long minutes = remainder / (long)'\uea60';
        remainder %= (long)'\uea60';
        long seconds = remainder / (long)1000;
        long millisec = milliseconds % 1000;
        
        return String.valueOf(String.valueOf(
                (new StringBuffer(String.valueOf(String.valueOf(days)))).append("d ").append(hours).append("h ").append(minutes).append("m ").append(seconds).append(".").append(millisec).append("s")));
    }

    
        /**
     * 밀리세컨드를 시간으로 변환
     * @param milliseconds
     * @return
     */
    public static String getTimeShortString(long milliseconds) {
        long days = milliseconds / (long)86400000;
        long remainder = milliseconds % (long)86400000;
        long hours = remainder / (long)3600000;
        remainder %= (long)3600000;
        long minutes = remainder / (long)'\uea60';
        remainder %= (long)'\uea60';
        long seconds = remainder / (long)1000;
        long millisec = milliseconds % 1000;
        
        String str = (days == 0 ? "" : String.valueOf(days) + "d ") + (hours == 0 ? "" : String.valueOf(hours) + "h ") + 
                (minutes == 0 ? "" : String.valueOf(minutes) + "m ") + (seconds == 0 ? "" : String.valueOf(seconds) + "s ");
        
        return str ;
    }

    public static void main(String[] args) {
        String startTime = DateTimeUtils.getStringFromDate( DateTimeUtils.getAfterSecondTime(new Date(), DateTimeUtils.SECONDS_PER_DAY * -1) ) + " 00:00:00";
        String endTime = DateTimeUtils.getStringFromDate(new Date()) + " 00:00:00";

        System.out.println("1 : " + startTime + " ~ " + endTime );
        System.out.println("2 : " + getStringFromDate(getAfterSecondTime("2020-01-01 23:59:59",  "yyyy-MM-dd kk:mm:ss", SECONDS_PER_DAY )) );
//        System.out.println("3 : " + getIntervalToMidnight("", INTERVAL_MINUTE) );
//        System.out.println("4 : " + getIntervalToMidnight("", INTERVAL_SECOND) );
        System.out.println("5 : " + getTimeDateNow("(yyyy-MM-dd HH:mm:ss)"));
        System.out.println("5 : " + getTimeToZero(new Date(), Calendar.SECOND) );
        System.out.println("6 : " + getStringFromDateTime(getAfterSecondTime(new Date(), DateTimeUtils.SECONDS_PER_HOUR * -1)) );
        System.out.println("7 : " + DateTimeUtils.getOnlyTimeValue(new Date(), Calendar.MINUTE) / 10 );
        System.out.println("8 : " + DateTimeUtils.getTimeString(100022) );
        System.out.println("9 : " + DateTimeUtils.convertTimestampToDate(1623031480634L) );
        System.out.println("9 : " + DateTimeUtils.convertTimestampToDate(1659410589731L) );
        
        long nowT = getTimestampNow();
        System.out.println("10 : " + nowT);
        System.out.println("10 : " + DateTimeUtils.convertTimestampToDate(nowT));
        System.out.println("11 : " + DateTimeUtils.convertFormat("2021/06/30/23:00:00", "yyyy/MM/dd/HH:mm:ss", "yyyy-MM-dd HH:mm:ss")); // 2021/06/30/23:00:00);
        System.out.println("12 : " + DateTimeUtils.convertTimestampToDate(1635397555));
        System.out.println("12 : " + DateTimeUtils.convertTimestampToDate(1658106119));


    }



}