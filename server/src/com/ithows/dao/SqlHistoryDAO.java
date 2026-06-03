package com.ithows.dao;

import com.ithows.ResultMap;
import com.ithows.aida.AppDb;
import com.ithows.aida.SqliteUtil;

import java.sql.Connection;
import java.util.List;

/**
 * SQL 실행 히스토리 DAO — 중앙 app.db {@code aida_sql_history} (앱 전역, 최신 N개 유지).
 */
public class SqlHistoryDAO {

    private static final int MAX = 100;

    private SqlHistoryDAO() {}

    /** 실행한 SQL을 기록(직전과 동일하면 건너뜀). 성공 시 1. */
    public static int add(String sql) {
        if (sql == null || sql.trim().isEmpty()) return 0;
        String q = sql.trim();
        try (Connection c = AppDb.conn()) {
            ResultMap latest = SqliteUtil.queryForMap(c,
                    "SELECT sql FROM aida_sql_history ORDER BY id DESC LIMIT 1", null);
            if (latest != null && q.equals(String.valueOf(latest.get("sql")))) return 0; // 중복 연속 입력 무시
            int n = SqliteUtil.update(c,
                    "INSERT INTO aida_sql_history(sql, created_at) VALUES(?, ?)",
                    new Object[]{q, System.currentTimeMillis() / 1000L});
            // 최신 MAX개만 유지
            SqliteUtil.update(c,
                    "DELETE FROM aida_sql_history WHERE id NOT IN "
                  + "(SELECT id FROM aida_sql_history ORDER BY id DESC LIMIT ?)",
                    new Object[]{MAX});
            return n;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** 최신순 목록. */
    public static List<ResultMap> list(int limit) {
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.queryForMapList(c,
                    "SELECT id, sql, created_at FROM aida_sql_history ORDER BY id DESC LIMIT ?",
                    new Object[]{limit <= 0 ? MAX : limit});
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int clear() {
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.update(c, "DELETE FROM aida_sql_history", null);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
