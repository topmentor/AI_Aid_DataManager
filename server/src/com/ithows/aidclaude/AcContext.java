package com.ithows.aidclaude;

import java.io.File;

/**
 * AidClaude 데이터 홈 및 표준 경로 해석.
 *
 * <p>홈 디렉터리 우선순위:</p>
 * <ol>
 *   <li>시스템 프로퍼티 {@code -Daidclaude.home=<path>} (Electron 기동 시 지정)</li>
 *   <li>환경변수 {@code AIDCLAUDE_HOME}</li>
 *   <li>기본값 {@code ${user.home}/.aidclaude}</li>
 * </ol>
 */
public final class AcContext {

    private static File home;

    private AcContext() {}

    public static synchronized File home() {
        if (home == null) {
            String p = System.getProperty("aidclaude.home");
            if (p == null || p.isEmpty()) p = System.getenv("AIDCLAUDE_HOME");
            if (p == null || p.isEmpty()) {
                p = new File(System.getProperty("user.home"), ".aidclaude").getAbsolutePath();
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
