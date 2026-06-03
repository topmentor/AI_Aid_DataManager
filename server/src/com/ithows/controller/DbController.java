package com.ithows.controller;

import com.ithows.HttpUtil;
import com.ithows.ResultMap;
import com.ithows.aidclaude.AcContext;
import com.ithows.aidclaude.AcResp;
import com.ithows.aidclaude.Names;
import com.ithows.aidclaude.SqliteUtil;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.ithows.dao.CatalogDAO;
import com.ithows.dao.JobDAO;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * job data.db 브라우저 + 소스로 저장 API.
 * (기존 IPC: db:listTables / db:previewTable / db:saveAsSource / data:saveAsSource)
 */
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class DbController {

    @ControllerMethodInfo(id = "/api/listTables.do")
    @ApiInfo(summary = "테이블 목록", tag = "DB", method = "POST")
    public String listTables(HttpSession session, HttpServletRequest request,
                             HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AcResp.error(request, "요청 본문(JSON)이 필요합니다");
        File db = jobDb(body.optString("jobId", ""));
        if (db == null) return AcResp.error(request, "작업을 찾을 수 없습니다");
        try (Connection c = SqliteUtil.open(db.getAbsolutePath())) {
            JSONArray arr = new JSONArray();
            for (String t : SqliteUtil.listTables(c)) arr.put(t);
            return AcResp.list(request, arr);
        } catch (Exception e) {
            return AcResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/previewTable.do")
    @ApiInfo(summary = "테이블 미리보기", tag = "DB", method = "POST")
    public String previewTable(HttpSession session, HttpServletRequest request,
                               HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AcResp.error(request, "요청 본문(JSON)이 필요합니다");
        File db = jobDb(body.optString("jobId", ""));
        if (db == null) return AcResp.error(request, "작업을 찾을 수 없습니다");
        String table = body.optString("tableName", "");
        int limit = body.optInt("limit", 500);
        if (table.isEmpty()) return AcResp.error(request, "tableName이 필요합니다");
        try (Connection c = SqliteUtil.open(db.getAbsolutePath());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + SqliteUtil.quoteIdent(table) + " LIMIT " + limit)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            JSONArray headers = new JSONArray();
            for (int i = 1; i <= cols; i++) headers.put(md.getColumnLabel(i));
            JSONArray rows = new JSONArray();
            while (rs.next()) {
                JSONArray row = new JSONArray();
                for (int i = 1; i <= cols; i++) {
                    Object v = rs.getObject(i);
                    row.put(v == null ? "" : String.valueOf(v));
                }
                rows.put(row);
            }
            JSONObject out = new JSONObject();
            out.put("title", table);
            out.put("headers", headers);
            out.put("rows", rows);
            return AcResp.map(request, out);
        } catch (Exception e) {
            return AcResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/saveTableAsSource.do")
    @ApiInfo(summary = "테이블을 CSV 소스로 저장", tag = "DB", method = "POST")
    public String saveTableAsSource(HttpSession session, HttpServletRequest request,
                                    HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AcResp.error(request, "요청 본문(JSON)이 필요합니다");
        File db = jobDb(body.optString("jobId", ""));
        if (db == null) return AcResp.error(request, "작업을 찾을 수 없습니다");
        String table = body.optString("tableName", "");
        String sourceName = body.optString("sourceName", "").trim();
        if (table.isEmpty() || sourceName.isEmpty()) return AcResp.error(request, "tableName/sourceName이 필요합니다");
        try (Connection c = SqliteUtil.open(db.getAbsolutePath());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + SqliteUtil.quoteIdent(table))) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            List<String> headers = new java.util.ArrayList<>();
            for (int i = 1; i <= cols; i++) headers.add(md.getColumnLabel(i));
            StringBuilder sb = new StringBuilder();
            sb.append(csvRow(headers.toArray(new String[0])));
            int n = 0;
            while (rs.next()) {
                String[] cells = new String[cols];
                for (int i = 1; i <= cols; i++) {
                    Object v = rs.getObject(i);
                    cells[i - 1] = v == null ? "" : String.valueOf(v);
                }
                sb.append("\r\n").append(csvRow(cells));
                n++;
            }
            if (n == 0) return AcResp.error(request, "테이블이 비어 있습니다");
            JSONObject src = writeCsvSource(sourceName, sb.toString());
            return AcResp.map(request, src);
        } catch (Exception e) {
            return AcResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/saveDataAsSource.do")
    @ApiInfo(summary = "임의 데이터를 CSV 소스로 저장", tag = "DB", method = "POST")
    public String saveDataAsSource(HttpSession session, HttpServletRequest request,
                                   HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AcResp.error(request, "요청 본문(JSON)이 필요합니다");
        String sourceName = body.optString("sourceName", "").trim();
        JSONArray headers = body.optJSONArray("headers");
        JSONArray rows = body.optJSONArray("rows");
        if (sourceName.isEmpty() || headers == null) return AcResp.error(request, "sourceName/headers가 필요합니다");
        String[] hs = new String[headers.length()];
        for (int i = 0; i < hs.length; i++) hs[i] = headers.optString(i, "");
        StringBuilder sb = new StringBuilder(csvRow(hs));
        if (rows != null) {
            for (int r = 0; r < rows.length(); r++) {
                JSONArray row = rows.optJSONArray(r);
                String[] cells = new String[hs.length];
                for (int i = 0; i < hs.length; i++) cells[i] = (row != null) ? row.optString(i, "") : "";
                sb.append("\r\n").append(csvRow(cells));
            }
        }
        try {
            return AcResp.map(request, writeCsvSource(sourceName, sb.toString()));
        } catch (Exception e) {
            return AcResp.error(request, e.getMessage());
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    /** CSV(UTF-8 BOM)로 data/에 저장 후 카탈로그에 등록, 등록된 소스 JSON 반환. */
    private static JSONObject writeCsvSource(String sourceName, String csvContent) throws Exception {
        File dataDir = AcContext.dataDir();
        dataDir.mkdirs();
        String safe = sourceName.replaceAll("[^\\w\\uac00-\\ud7a3_-]", "_");
        File csv = new File(dataDir, safe + "_" + System.currentTimeMillis() + ".csv");
        // UTF-8 BOM → Excel 한글 호환
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] data = csvContent.getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[bom.length + data.length];
        System.arraycopy(bom, 0, all, 0, bom.length);
        System.arraycopy(data, 0, all, bom.length, data.length);
        Files.write(csv.toPath(), all);

        JSONObject cfg = new JSONObject().put("filePath", csv.getAbsolutePath());
        String id = CatalogDAO.add(sourceName, "csv", cfg.toString());
        ResultMap row = CatalogDAO.get(id);
        return com.ithows.aidclaude.model.SourceRef.from(row).toJson();
    }

    private static String csvRow(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(csvEscape(cells[i]));
        }
        return sb.toString();
    }

    private static String csvEscape(String v) {
        if (v == null) return "";
        if (v.matches("(?s).*[,\"\n\r].*")) return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    private static File jobDb(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) return null;
        ResultMap job = JobDAO.get(jobId.trim());
        if (job == null) return null;
        return new File(String.valueOf(job.get("workspace_dir")), "data.db");
    }

    private static JSONObject bodyOrNull(HttpServletRequest request) {
        try { return HttpUtil.getBodyJson(request); } catch (Exception e) { return null; }
    }
}
