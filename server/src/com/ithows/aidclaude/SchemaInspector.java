package com.ithows.aidclaude;

import com.ithows.ResultMap;
import com.ithows.aidclaude.model.SourceRef;
import com.ithows.aidclaude.shapefile.DbfReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 데이터 소스 스키마/미리보기 검사. (기존 IPC: catalog:getSchema / catalog:previewData)
 * 지원: csv, json, jsonl, mariadb, shapefile.
 */
public final class SchemaInspector {

    private SchemaInspector() {}

    /** 평면 소스: {@code {type, rowCount, columns:[{name,type,sample}]}}.
     *  MariaDB: {@code {type:"mariadb", tables:[{tableName, columns:[{name,type,nullable}], rowCount}]}}. */
    public static JSONObject inspect(SourceRef s) throws Exception {
        switch (s.type == null ? "" : s.type) {
            case "csv":       return inspectCsv(s);
            case "json":      return inspectJsonTable(s, JsonSource.readJson(s));
            case "jsonl":     return inspectJsonTable(s, JsonSource.readJsonl(s));
            case "mariadb":   return inspectMariaDb(s);
            case "shapefile": return inspectShapefile(s);
            default: throw new Exception("스키마 미지원 소스 유형: " + s.type);
        }
    }

    /** {@code {title, headers:[], rows:[[]]}} */
    public static JSONObject preview(SourceRef s, int limit) throws Exception {
        switch (s.type == null ? "" : s.type) {
            case "csv":       return previewCsv(s, limit);
            case "json":      return tableToPreview(s.name, JsonSource.readJson(s), limit);
            case "jsonl":     return tableToPreview(s.name, JsonSource.readJsonl(s), limit);
            case "mariadb":   return previewMariaDb(s, limit);
            case "shapefile": return previewShapefile(s, limit);
            default: throw new Exception("미리보기 미지원 소스 유형: " + s.type);
        }
    }

    // ── CSV ────────────────────────────────────────────────────────────────

    private static CSVFormat csvFormat(SourceRef s) {
        String delim = s.configStr("delimiter", ",");
        char d = (delim == null || delim.isEmpty()) ? ',' : delim.charAt(0);
        return CSVFormat.DEFAULT.withDelimiter(d).withFirstRecordAsHeader().withIgnoreEmptyLines(true);
    }

    private static JSONObject inspectCsv(SourceRef s) throws Exception {
        File f = new File(s.configStr("filePath", ""));
        if (!f.exists()) throw new Exception("파일이 존재하지 않습니다: " + f.getPath());
        JSONObject out = new JSONObject();
        out.put("type", "csv");
        try (Reader r = utf8(f); CSVParser p = new CSVParser(r, csvFormat(s))) {
            List<String> headers = p.getHeaderNames();
            CSVRecord first = null;
            long rowCount = 0;
            for (CSVRecord rec : p) { if (first == null) first = rec; rowCount++; }
            JSONArray cols = new JSONArray();
            for (String h : headers) {
                cols.put(new JSONObject().put("name", h).put("type", "string")
                        .put("sample", (first != null && first.isMapped(h)) ? first.get(h) : ""));
            }
            out.put("columns", cols);
            out.put("rowCount", rowCount);
        }
        return out;
    }

    private static JSONObject previewCsv(SourceRef s, int limit) throws Exception {
        File f = new File(s.configStr("filePath", ""));
        if (!f.exists()) throw new Exception("파일이 존재하지 않습니다: " + f.getPath());
        JSONObject out = new JSONObject();
        out.put("title", s.name);
        try (Reader r = utf8(f); CSVParser p = new CSVParser(r, csvFormat(s))) {
            List<String> headers = p.getHeaderNames();
            JSONArray headerArr = new JSONArray();
            for (String h : headers) headerArr.put(h);
            out.put("headers", headerArr);
            JSONArray rows = new JSONArray();
            int n = 0;
            for (CSVRecord rec : p) {
                if (n++ >= limit) break;
                JSONArray row = new JSONArray();
                for (String h : headers) row.put(rec.isMapped(h) ? rec.get(h) : "");
                rows.put(row);
            }
            out.put("rows", rows);
        }
        return out;
    }

    // ── JSON / JSONL (정규화 테이블 공통) ────────────────────────────────────

    private static JSONObject inspectJsonTable(SourceRef s, JsonSource.Table t) {
        JSONObject out = new JSONObject();
        out.put("type", s.type);
        JSONArray cols = new JSONArray();
        for (String fld : t.fields) {
            Object sample = t.rows.isEmpty() ? "" : t.rows.get(0).get(fld);
            cols.put(new JSONObject().put("name", fld).put("type", "string")
                    .put("sample", sample == null ? "" : String.valueOf(sample)));
        }
        out.put("columns", cols);
        out.put("rowCount", (long) t.rows.size());
        return out;
    }

    private static JSONObject tableToPreview(String title, JsonSource.Table t, int limit) {
        JSONObject out = new JSONObject();
        out.put("title", title);
        JSONArray headers = new JSONArray();
        for (String fld : t.fields) headers.put(fld);
        out.put("headers", headers);
        JSONArray rows = new JSONArray();
        for (int i = 0; i < t.rows.size() && i < limit; i++) {
            Map<String, Object> r = t.rows.get(i);
            JSONArray row = new JSONArray();
            for (String fld : t.fields) {
                Object v = r.get(fld);
                row.put(v == null ? "" : String.valueOf(v));
            }
            rows.put(row);
        }
        out.put("rows", rows);
        return out;
    }

    // ── MariaDB ──────────────────────────────────────────────────────────────

    private static JSONObject inspectMariaDb(SourceRef s) throws Exception {
        JSONObject out = new JSONObject();
        out.put("type", "mariadb");
        JSONArray tables = new JSONArray();
        String db = MariaDbUtil.database(s);
        try (Connection c = MariaDbUtil.connect(s)) {
            List<ResultMap> cols = SqliteUtil.queryForMapList(c,
                    "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE FROM information_schema.COLUMNS "
                  + "WHERE TABLE_SCHEMA=? ORDER BY TABLE_NAME, ORDINAL_POSITION", new Object[]{db});
            List<ResultMap> cnts = SqliteUtil.queryForMapList(c,
                    "SELECT TABLE_NAME, TABLE_ROWS FROM information_schema.TABLES "
                  + "WHERE TABLE_SCHEMA=? AND TABLE_TYPE='BASE TABLE'", new Object[]{db});
            Map<String, Long> rowCount = new LinkedHashMap<>();
            for (ResultMap r : cnts) rowCount.put(String.valueOf(r.get("TABLE_NAME")), r.getLong("TABLE_ROWS", 0));
            Map<String, JSONArray> byTable = new LinkedHashMap<>();
            for (ResultMap r : cols) {
                String tn = String.valueOf(r.get("TABLE_NAME"));
                byTable.computeIfAbsent(tn, k -> new JSONArray()).put(new JSONObject()
                        .put("name", String.valueOf(r.get("COLUMN_NAME")))
                        .put("type", String.valueOf(r.get("DATA_TYPE")))
                        .put("nullable", "YES".equalsIgnoreCase(String.valueOf(r.get("IS_NULLABLE")))));
            }
            for (Map.Entry<String, JSONArray> e : byTable.entrySet()) {
                tables.put(new JSONObject().put("tableName", e.getKey()).put("columns", e.getValue())
                        .put("rowCount", rowCount.getOrDefault(e.getKey(), 0L)));
            }
        }
        out.put("tables", tables);
        return out;
    }

    private static JSONObject previewMariaDb(SourceRef s, int limit) throws Exception {
        String db = MariaDbUtil.database(s);
        try (Connection c = MariaDbUtil.connect(s)) {
            ResultMap firstTable = SqliteUtil.queryForMap(c,
                    "SELECT TABLE_NAME FROM information_schema.TABLES "
                  + "WHERE TABLE_SCHEMA=? AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME LIMIT 1",
                    new Object[]{db});
            if (firstTable == null) throw new Exception("테이블이 없습니다");
            String tn = String.valueOf(firstTable.get("TABLE_NAME"));
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM `" + tn.replace("`", "``") + "` LIMIT " + limit)) {
                return rsToPreview(tn, rs);
            }
        }
    }

    // ── Shapefile (DBF 속성) ──────────────────────────────────────────────────

    private static JSONObject inspectShapefile(SourceRef s) throws Exception {
        DbfReader r = DbfReader.open(new File(s.configStr("shpPath", "")), s.configStr("encoding", "euc-kr"));
        JSONObject out = new JSONObject();
        out.put("type", "shapefile");
        JSONArray cols = new JSONArray();
        List<Map<String, Object>> sample = r.readRecords(1);
        for (String fld : r.fields) {
            Object sv = sample.isEmpty() ? "" : sample.get(0).get(fld);
            cols.put(new JSONObject().put("name", fld).put("type", "string")
                    .put("sample", sv == null ? "" : String.valueOf(sv)));
        }
        out.put("columns", cols);
        out.put("rowCount", r.recordCount);
        return out;
    }

    private static JSONObject previewShapefile(SourceRef s, int limit) throws Exception {
        DbfReader r = DbfReader.open(new File(s.configStr("shpPath", "")), s.configStr("encoding", "euc-kr"));
        JSONObject out = new JSONObject();
        out.put("title", s.name);
        JSONArray headers = new JSONArray();
        for (String fld : r.fields) headers.put(fld);
        out.put("headers", headers);
        JSONArray rows = new JSONArray();
        for (Map<String, Object> rec : r.readRecords(limit)) {
            JSONArray row = new JSONArray();
            for (String fld : r.fields) { Object v = rec.get(fld); row.put(v == null ? "" : String.valueOf(v)); }
            rows.put(row);
        }
        out.put("rows", rows);
        return out;
    }

    // ── 공통 ──────────────────────────────────────────────────────────────────

    private static JSONObject rsToPreview(String title, ResultSet rs) throws Exception {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();
        JSONObject out = new JSONObject();
        out.put("title", title);
        JSONArray headers = new JSONArray();
        for (int i = 1; i <= cols; i++) headers.put(md.getColumnLabel(i));
        out.put("headers", headers);
        JSONArray rows = new JSONArray();
        while (rs.next()) {
            JSONArray row = new JSONArray();
            for (int i = 1; i <= cols; i++) { Object v = rs.getObject(i); row.put(v == null ? "" : String.valueOf(v)); }
            rows.put(row);
        }
        out.put("rows", rows);
        return out;
    }

    private static Reader utf8(File f) throws Exception {
        return new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
    }
}
