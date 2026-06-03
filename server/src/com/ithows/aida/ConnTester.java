package com.ithows.aida;

import com.ithows.aida.model.SourceRef;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.json.JSONObject;

/**
 * 데이터 소스 연결 테스트. (기존 IPC: catalog:testConnection)
 */
public final class ConnTester {

    private ConnTester() {}

    /** {@code {ok:boolean, error?:string}} */
    public static JSONObject test(SourceRef s) {
        JSONObject o = new JSONObject();
        try {
            switch (s.type == null ? "" : s.type) {
                case "csv":
                case "json":
                case "jsonl":
                    checkFile(s.configStr("filePath", ""));
                    break;
                case "shapefile":
                    checkFile(s.configStr("shpPath", ""));
                    break;
                case "mariadb":
                    testMariaDb(s);
                    break;
                default:
                    return o.put("ok", false).put("error", "지원하지 않는 소스 유형: " + s.type);
            }
            return o.put("ok", true);
        } catch (Exception e) {
            return o.put("ok", false).put("error", e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private static void checkFile(String path) throws Exception {
        if (path == null || path.isEmpty()) throw new Exception("파일 경로가 비어 있습니다");
        File f = new File(path);
        if (!f.exists()) throw new Exception("파일이 존재하지 않습니다: " + path);
        if (!f.canRead()) throw new Exception("파일을 읽을 수 없습니다: " + path);
    }

    private static void testMariaDb(SourceRef s) throws Exception {
        Class.forName("org.mariadb.jdbc.Driver");
        String host = s.configStr("host", "localhost");
        int port = s.config.optInt("port", 3306);
        String db = s.configStr("database", "");
        String user = s.configStr("user", "");
        String pw = s.configStr("password", "");
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + db
                   + "?connectTimeout=5000&socketTimeout=5000";
        try (Connection c = DriverManager.getConnection(url, user, pw);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            if (!rs.next()) throw new Exception("SELECT 1 응답 없음");
        }
    }
}
