package com.ithows.aidclaude;

import com.ithows.aidclaude.model.SourceRef;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * MariaDB 소스 접속 헬퍼(저장된 자격증명 사용).
 */
public final class MariaDbUtil {

    private MariaDbUtil() {}

    public static Connection connect(SourceRef s) throws Exception {
        Class.forName("org.mariadb.jdbc.Driver");
        String host = s.configStr("host", "localhost");
        int port = s.config.optInt("port", 3306);
        String db = s.configStr("database", "");
        String user = s.configStr("user", "");
        String pw = s.configStr("password", "");
        String url = "jdbc:mariadb://" + host + ":" + port + "/" + db
                   + "?connectTimeout=10000&socketTimeout=30000";
        return DriverManager.getConnection(url, user, pw);
    }

    public static String database(SourceRef s) { return s.configStr("database", ""); }
}
