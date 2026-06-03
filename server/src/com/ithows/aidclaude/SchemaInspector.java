package com.ithows.aidclaude;

import com.ithows.aidclaude.model.SourceRef;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 데이터 소스 스키마/미리보기 검사. (기존 IPC: catalog:getSchema / catalog:previewData)
 *
 * <p>M4: CSV. JSON/JSONL/MariaDB/Shapefile은 M7에서 추가.</p>
 */
public final class SchemaInspector {

    private SchemaInspector() {}

    /** {@code {type, rowCount, columns:[{name,type,sample}]}} */
    public static JSONObject inspect(SourceRef s) throws Exception {
        switch (s.type == null ? "" : s.type) {
            case "csv": return inspectCsv(s);
            default: throw new Exception("스키마 미지원 소스 유형: " + s.type);
        }
    }

    /** {@code {title, headers:[], rows:[[]]}} */
    public static JSONObject preview(SourceRef s, int limit) throws Exception {
        switch (s.type == null ? "" : s.type) {
            case "csv": return previewCsv(s, limit);
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
            JSONArray cols = new JSONArray();
            CSVRecord first = null;
            long rowCount = 0;
            for (CSVRecord rec : p) {
                if (first == null) first = rec;
                rowCount++;
            }
            for (String h : headers) {
                JSONObject c = new JSONObject();
                c.put("name", h);
                c.put("type", "string");
                c.put("sample", (first != null && first.isMapped(h)) ? first.get(h) : "");
                cols.put(c);
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

    private static Reader utf8(File f) throws Exception {
        return new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
    }

    static List<String> emptyList() { return new ArrayList<>(); }
}
