package com.ithows;

import java.io.File;
import java.security.ProtectionDomain;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Embedded Tomcat으로 SSF2026를 실행하는 메인 클래스.
 *
 * <p>시스템 프로퍼티:</p>
 * <ul>
 *   <li>{@code -Dserver.port=8080} : HTTP 포트 (기본값: 8080)</li>
 *   <li>{@code -Dserver.contextPath=/SSF2026} : 컨텍스트 경로 (기본값: /SSF2026)</li>
 *   <li>{@code -Dwebapp.base=web} : 웹앱 기본 디렉토리 (개발 모드용)</li>
 * </ul>
 */
public class EmbeddedApplication {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_CONTEXT_PATH = "/AidClaude";

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("server.port", String.valueOf(DEFAULT_PORT)));
        String contextPath = System.getProperty("server.contextPath", DEFAULT_CONTEXT_PATH);
        String webappBase = System.getProperty("webapp.base");

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        // 개발 모드 판별: -Dwebapp.base=web 이고 web/WEB-INF/web.xml이 존재
        boolean devMode = false;
        if ("web".equals(webappBase)) {
            File webXml = new File("web/WEB-INF/web.xml");
            if (webXml.exists()) {
                devMode = true;
            }
        }

        // 웹앱 베이스 디렉토리 결정
        String webappDir;
        if (webappBase != null && new File(webappBase).exists()) {
            webappDir = new File(webappBase).getAbsolutePath();
        } else {
            webappDir = new File("web").getAbsolutePath();
        }

        System.out.println("========================================");
        System.out.println(" SSF2026 Embedded Tomcat");
        System.out.println(" Port    : " + port);
        System.out.println(" Context : " + contextPath);
        System.out.println(" Webapp  : " + webappDir);
        System.out.println(" Mode    : " + (devMode ? "Development" : "Production"));
        System.out.println("========================================");

        Context ctx = tomcat.addWebapp(contextPath, webappDir);

        // 개발 모드에서만 추가 리소스 등록
        // (프로덕션 모드에서는 추출된 WAR에 이미 classes와 lib가 포함되어 있음)
        if (devMode) {
            File classesDir = resolveClassesDir();
            if (classesDir != null && classesDir.exists()) {
                WebResourceRoot resources = new StandardRoot(ctx);
                resources.addPreResources(new DirResourceSet(
                        resources, "/WEB-INF/classes",
                        classesDir.getAbsolutePath(), "/"));
                ctx.setResources(resources);
                System.out.println(" Classes : " + classesDir.getAbsolutePath());
            }

            File libDir = new File("lib");
            if (libDir.exists() && libDir.isDirectory()) {
                WebResourceRoot resources = ctx.getResources();
                if (resources == null) {
                    resources = new StandardRoot(ctx);
                    ctx.setResources(resources);
                }
                resources.addPreResources(new DirResourceSet(
                        resources, "/WEB-INF/lib",
                        libDir.getAbsolutePath(), "/"));
                System.out.println(" Lib     : " + libDir.getAbsolutePath());
            }
        }

        System.out.println("========================================");

        tomcat.start();
        System.out.println("Server started: http://localhost:" + port + contextPath);
        tomcat.getServer().await();
    }

    /**
     * 컴파일된 클래스 디렉토리를 찾는다 (개발 모드 전용).
     */
    private static File resolveClassesDir() {
        File mavenClasses = new File("target/classes");
        if (mavenClasses.exists()) return mavenClasses;

        File antClasses = new File("build/web/WEB-INF/classes");
        if (antClasses.exists()) return antClasses;

        return null;
    }
}
