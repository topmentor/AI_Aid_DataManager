package com.ithows.dao;

import com.ithows.ResultMap;
import com.ithows.aida.AidaContext;
import com.ithows.aida.AppDb;
import com.ithows.aida.SqliteUtil;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * 앱 설정 DAO — 중앙 app.db {@code aida_settings(key, value)} 키/값 저장.
 * 기존 AppSettings(claudeBin/pythonBin/workspaceRoot) 기본값과 병합해 반환한다.
 */
public class SettingsDAO {

    private SettingsDAO() {}

    /** 저장값이 없을 때 사용하는 기본값. */
    public static Map<String, String> defaults() {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("claudeBin", "claude");
        d.put("pythonBin", "python");
        d.put("workspaceRoot", AidaContext.home().getAbsolutePath());
        return d;
    }

    /** 기본값 + 저장값 병합. */
    public static Map<String, String> getAll() {
        Map<String, String> out = defaults();
        try (Connection c = AppDb.conn()) {
            List<ResultMap> rows = SqliteUtil.queryForMapList(
                    c, "SELECT key, value FROM aida_settings", null);
            for (ResultMap r : rows) {
                out.put(String.valueOf(r.get("key")), r.get("value") == null ? null : String.valueOf(r.get("value")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public static int put(String key, String value) {
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.update(c,
                    "INSERT INTO aida_settings(key, value) VALUES(?, ?) "
                  + "ON CONFLICT(key) DO UPDATE SET value=excluded.value",
                    new Object[]{key, value});
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** JSON 부분 설정을 모두 upsert. 변경된 키 수 반환. */
    public static int putAll(JSONObject patch) {
        if (patch == null) return 0;
        int n = 0;
        for (String key : patch.keySet()) {
            Object v = patch.isNull(key) ? null : patch.get(key);
            n += put(key, v == null ? null : String.valueOf(v));
        }
        return n;
    }
}
