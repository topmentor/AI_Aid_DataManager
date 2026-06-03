package com.ithows.aida;

import java.io.File;

/**
 * AIDA 데이터 홈 및 표준 경로 해석.
 *
 * <p>홈 디렉터리 우선순위:</p>
 * <ol>
 *   <li>시스템 프로퍼티 {@code -Daida.home=<path>} (Electron 기동 시 지정)</li>
 *   <li>환경변수 {@code AIDA_HOME}</li>
 *   <li>기본값 {@code ${user.home}/.aida}</li>
 * </ol>
 */
public final class AidaContext {

    private static File home;

    private AidaContext() {}

    public static synchronized File home() {
        if (home == null) {
            String p = System.getProperty("aida.home");
            if (p == null || p.isEmpty()) p = System.getenv("AIDA_HOME");
            if (p == null || p.isEmpty()) {
                p = new File(System.getProperty("user.home"), ".aida").getAbsolutePath();
            }
            home = new File(p);
            home.mkdirs();
            dataDir().mkdirs();
            jobsDir().mkdirs();
        }
        return home;
    }

    public static File appDbFile() { return new File(home(), "app.db"); }
    public static File dataDir()   { return new File(home(), "data"); }
    public static File jobsDir()   { return new File(home(), "jobs"); }
    public static File jobDir(String jobId) { return new File(jobsDir(), "job_" + jobId); }
}
