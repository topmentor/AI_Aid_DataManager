/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ithows.base;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author mailt
 */
public class SOXServletContextListener implements ServletContextListener, Runnable  {
    
    boolean flag = false;
    Thread monitor = null;
    ServletContext sc = null;

    // web application 시작 시 한번 생성되는 ServletContext 생성 직후 호출
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        this.sc = sce.getServletContext();

        System.out.println(">>>>>> OmniFLService  context initialized  ");

        this.monitor = new Thread(this);
        this.monitor.setDaemon(true);
        this.monitor.start();
    }

    @Override
    public void run() {
        try {
            // delay 실행
            Thread.sleep(3000);
        } catch (Exception e) {
        }

        while (!flag) {

            ///////////////////////////////////////////////////
            //  @@ 반복 실행할 코드 넣는 부분


            // 주기 조절
            try {
                Thread.sleep(20000);
            } catch (Exception e) {
            }
            
            System.out.println(">>>>>>  continue process  ");
        }
    }

    
    // web application이 종료 직전에 실행되는 메서드
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        this.flag = true;
        System.out.println(">>>>>> OmniFLService context destroyed  ");
    }
}
