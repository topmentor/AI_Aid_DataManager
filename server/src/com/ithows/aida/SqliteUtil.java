package com.ithows.aida;

import com.ithows.ResultMap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 전용 경량 JDBC 헬퍼.
 *
 * <p>프레임워크의 {@link com.ithows.JdbcDao}는 MariaDB 중앙 DataSource에 하드와이어되어 있어
 * 파일별(중앙 app.db / job별 data.db) SQLite 접근에는 부적합하다. 이 클래스는 경로별로
 * {@link Connection}을 직접 열어 사용한다.</p>
 */
public final class SqliteUtil {

    static {
        try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException ignore) {}
    }

    private SqliteUtil() {}

    public static Connection open(String path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path);
    }

    public static List<ResultMap> queryForMapList(Connection c, String sql, Object[] params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    public static ResultMap queryForMap(Connection c, String sql, Object[] params) throws SQLException {
        List<ResultMap> l = queryForMapList(c, sql, params);
        return l.isEmpty() ? null : l.get(0);
    }

    public static int update(Connection c, String sql, Object[] params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** 세미콜론으로 구분된 다중 SQL 문을 순차 실행한다(문자열/주석 내 세미콜론 무시). */
    public static void execScript(Connection c, String sqlText) throws SQLException {
        for (String stmt : splitStatements(sqlText)) {
            String s = stmt.trim();
            if (s.isEmpty()) continue;
            try (Statement st = c.createStatement()) {
                st.execute(s);
            }
        }
    }

    public static List<String> listTables(Connection c) throws SQLException {
        List<String> out = new ArrayList<>();
        for (ResultMap m : queryForMapList(c,
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null)) {
            out.add(String.valueOf(m.get("name")));
        }
        return out;
    }

    /** 식별자를 SQLite용으로 안전하게 큰따옴표 인용한다. */
    public static String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
        }
    }

    private static List<ResultMap> mapRows(ResultSet rs) throws SQLException {
        List<ResultMap> list = new ArrayList<>();
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        while (rs.next()) {
            ResultMap m = new ResultMap();
            for (int i = 1; i <= cols; i++) {
                m.put(md.getColumnLabel(i), rs.getObject(i));
            }
            list.add(m);
        }
        return list;
    }

    /**
     * SQL 텍스트를 세미콜론 기준으로 분리한다. 작은따옴표/큰따옴표 문자열,
     * {@code --} 라인 주석, {@code /* *}{@code /} 블록 주석 내부의 세미콜론은 무시한다.
     */
    static List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int n = sql.length();
        boolean inSingle = false, inDouble = false, inLine = false, inBlock = false;
        for (int i = 0; i < n; i++) {
            char ch = sql.charAt(i);
            char next = (i + 1 < n) ? sql.charAt(i + 1) : '\0';

            if (inLine) {
                cur.append(ch);
                if (ch == '\n') inLine = false;
                continue;
            }
            if (inBlock) {
                cur.append(ch);
                if (ch == '*' && next == '/') { cur.append(next); i++; inBlock = false; }
                continue;
            }
            if (inSingle) {
                cur.append(ch);
                if (ch == '\'') {
                    if (next == '\'') { cur.append(next); i++; }   // 이스케이프된 ''
                    else inSingle = false;
                }
                continue;
            }
            if (inDouble) {
                cur.append(ch);
                if (ch == '"') {
                    if (next == '"') { cur.append(next); i++; }
                    else inDouble = false;
                }
                continue;
            }

            // 일반 상태
            if (ch == '-' && next == '-') { inLine = true; cur.append(ch); continue; }
            if (ch == '/' && next == '*') { inBlock = true; cur.append(ch); continue; }
            if (ch == '\'') { inSingle = true; cur.append(ch); continue; }
            if (ch == '"')  { inDouble = true; cur.append(ch); continue; }
            if (ch == ';')  { out.add(cur.toString()); cur.setLength(0); continue; }
            cur.append(ch);
        }
        if (cur.toString().trim().length() > 0) out.add(cur.toString());
        return out;
    }
}
