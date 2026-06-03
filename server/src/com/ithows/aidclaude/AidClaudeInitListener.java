package com.ithows.aidclaude;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 웹앱 기동 시 AidClaude 데이터 홈 생성 + 중앙 app.db 스키마 초기화.
 */
public class AidClaudeInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            AcContext.home();
            AppDb.init();
            System.out.println("[AidClaude] home=" + AcContext.home().getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[AidClaude] init 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}
