package com.ithows.aida.model;

import com.ithows.ResultMap;
import org.json.JSONObject;

/**
 * 카탈로그 한 행의 파싱된 표현. {@code config}는 JSON 객체로 해석된다.
 *
 * <p>type별 config 키:
 * csv {filePath, delimiter?}, json {filePath, rootPath?}, jsonl {filePath},
 * shapefile {shpPath, encoding?}, mariadb {host, port, database, user, password}.</p>
 */
public class SourceRef {

    public String id;
    public String name;
    public String type;
    public JSONObject config;
    public long createdAt;

    public static SourceRef from(ResultMap r) {
        SourceRef s = new SourceRef();
        s.id = str(r.get("id"));
        s.name = str(r.get("name"));
        s.type = str(r.get("type"));
        String cfg = r.get("config") == null ? "{}" : String.valueOf(r.get("config"));
        try { s.config = new JSONObject(cfg); } catch (Exception e) { s.config = new JSONObject(); }
        Object ca = r.get("created_at");
        try { s.createdAt = ca == null ? 0L : Long.parseLong(String.valueOf(ca)); } catch (Exception e) { s.createdAt = 0L; }
        return s;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("type", type);
        o.put("config", config == null ? new JSONObject() : config);
        o.put("createdAt", createdAt);
        return o;
    }

    public String configStr(String key, String def) {
        return (config != null && config.has(key) && !config.isNull(key)) ? config.optString(key, def) : def;
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
