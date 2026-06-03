package com.ithows.aida;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 웹앱 기동 시 AIDA 데이터 홈 생성 + 중앙 app.db 스키마 초기화.
 */
public class AidaInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            AidaContext.home();
            AppDb.init();
            System.out.println("[AIDA] home=" + AidaContext.home().getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[AIDA] init 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}
