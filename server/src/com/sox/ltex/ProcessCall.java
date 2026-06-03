/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
import net.sf.json.JSONArray;

public class ProcessCall {

    public static Process APProcess = null;
    public static JSONArray apList = null;

    // 출력값 있는 커맨드 실행
    // String[] command = {"/bin/sh", "-c", "/home/pi/indoor_positioning/sender/register_ap"};
    //  String[] command = {"/bin/sh", "-c", "sudo ntpdate -u 2.kr.pool.ntp.org"};
    public static ArrayList<String> normalCallCommand(String[] command) {

        ArrayList<String> outputStrings = new ArrayList<>();
        try {
            // 프로세스빌더 실행
            Process process = new ProcessBuilder(command).start();
            // 스캐너클래스를 사용해 InputStream을 스캔함
            Scanner s = new Scanner(process.getInputStream());
            while (s.hasNextLine() == true) {
                // 표준출력으로 출력
                outputStrings.add(s.nextLine());
//                System.out.println(s.nextLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return outputStrings;

    }


    // 출력값 없는 커맨드 실행
    // command = "/bin/sh " +  "-c " + "/home/pi/indoor_positioning/sender/indoorPos_sender true";
    // command = "/bin/sh " +  "-c " + "/home/pi/indoor_positioning/sender/indoorPos_sender " + deviceMac + " true ";
    public static void simpleCallCommand(String command) {

        try {
            Runtime rt = Runtime.getRuntime();
            rt.exec(command);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 신호수집 애플리케이션 강제 종료
    // String[] command = {"/bin/sh", "-c", "sudo ps -ax"};
    public static void managedCallCommand(String[] command) {

        String readLine = "";

        try {
            if (APProcess != null) {
                APProcess.destroy();
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {

                }
            }
            // 프로세스빌더 실행
            APProcess = new ProcessBuilder(command).start();
            // 스캐너클래스를 사용해 InputStream을 스캔함
            Scanner s = new Scanner(APProcess.getInputStream());

            while (s.hasNextLine() == true) {

                readLine = s.nextLine();
                // 표준출력으로 출력
                System.out.println(readLine);

//                if (readLine.contains("indoorPos_sender")) {
//                    StringTokenizer token = new StringTokenizer(readLine);
//                    String psNum = token.nextToken(" ");
//
//                    Runtime rt = Runtime.getRuntime();
//                    rt.exec("kill -9 " + psNum);
//                    System.out.println(">>>>>>>>>>> kill AP " + psNum );
//
//                    break;
//                }

            }
            APProcess.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String[] command = {"cmd.exe", "/c", "dir ."};
        ProcessCall.managedCallCommand(command);
    }

}
