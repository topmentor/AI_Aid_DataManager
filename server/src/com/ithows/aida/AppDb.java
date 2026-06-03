package com.ithows.aida;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 중앙 메타 데이터베이스(app.db) 접근 + 스키마 초기화.
 *
 * <p>테이블: {@code aida_settings}, {@code aida_catalog}, {@code aida_jobs}.
 * job별 data.db는 본 클래스가 아니라 {@link SqliteUtil}로 경로 직접 연결한다.</p>
 */
public final class AppDb {

    private AppDb() {}

    public static Connection conn() throws SQLException {
        return SqliteUtil.open(AidaContext.appDbFile().getAbsolutePath());
    }

    public static void init() throws SQLException {
        try (Connection c = conn()) {
            SqliteUtil.execScript(c,
                "CREATE TABLE IF NOT EXISTS aida_settings ("
              + "  key TEXT PRIMARY KEY,"
              + "  value TEXT"
              + ");"
              + "CREATE TABLE IF NOT EXISTS aida_catalog ("
              + "  id TEXT PRIMARY KEY,"
              + "  name TEXT NOT NULL,"
              + "  type TEXT NOT NULL,"
              + "  config TEXT,"
              + "  created_at INTEGER"
              + ");"
              + "CREATE TABLE IF NOT EXISTS aida_jobs ("
              + "  id TEXT PRIMARY KEY,"
              + "  user_request TEXT,"
              + "  status TEXT,"
              + "  error_msg TEXT,"
              + "  workspace_dir TEXT,"
              + "  created_at INTEGER,"
              + "  updated_at INTEGER"
              + ");");
        }
    }
}
