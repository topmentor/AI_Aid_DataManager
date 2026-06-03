package com.ithows.aidclaude;

import com.ithows.aidclaude.model.SourceRef;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * 데이터 소스를 job별 data.db로 적재한다. 기존 sqlite-loader.ts 포팅.
 *
 * <p>M5: CSV. JSON/JSONL/MariaDB/Shapefile은 M7에서 추가.</p>
 */
public final class SourceLoader {

    private SourceLoader() {}

    public static final class LoadedTable {
        public final String tableName;
        public final long rowCount;
        public LoadedTable(String t, long n) { this.tableName = t; this.rowCount = n; }
    }

    /** 소스를 data.db에 적재하고 생성된 테이블 목록을 반환한다. */
    public static List<LoadedTable> load(Connection db, SourceRef s) throws Exception {
        switch (s.type == null ? "" : s.type) {
            case "csv": return loadCsv(db, s);
            default: throw new Exception("적재 미지원 소스 유형: " + s.type);
        }
    }

    private static List<LoadedTable> loadCsv(Connection db, SourceRef s) throws Exception {
        String table = Names.toTableName(s.name);
        File f = new File(s.configStr("filePath", ""));
        if (!f.exists()) throw new Exception("파일이 존재하지 않습니다: " + f.getPath());

        String delim = s.configStr("delimiter", ",");
        char d = (delim == null || delim.isEmpty()) ? ',' : delim.charAt(0);
        CSVFormat fmt = CSVFormat.DEFAULT.withDelimiter(d).withFirstRecordAsHeader().withIgnoreEmptyLines(true);

        List<String> fields;
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
             CSVParser p = new CSVParser(r, fmt)) {
            fields = p.getHeaderNames();
            for (CSVRecord rec : p) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String h : fields) row.put(h, rec.isMapped(h) ? rec.get(h) : null);
                rows.add(row);
            }
        }
        long n = createAndInsert(db, table, fields, rows);
        List<LoadedTable> out = new ArrayList<>();
        out.add(new LoadedTable(table, n));
        return out;
    }

    /** DROP + CREATE(TEXT 컬럼) + 트랜잭션 배치 INSERT. */
    static long createAndInsert(Connection db, String tableName, List<String> fields,
                                List<Map<String, Object>> rows) throws Exception {
        String t = SqliteUtil.quoteIdent(tableName);
        try (Statement st = db.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + t);
        }
        String cols;
        if (fields == null || fields.isEmpty()) {
            cols = "_empty TEXT";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(SqliteUtil.quoteIdent(fields.get(i))).append(" TEXT");
            }
            cols = sb.toString();
        }
        try (Statement st = db.createStatement()) {
            st.execute("CREATE TABLE " + t + " (" + cols + ")");
        }
        if (fields == null || fields.isEmpty() || rows.isEmpty()) return 0;

        String ph = String.join(", ", Collections.nCopies(fields.size(), "?"));
        boolean prevAuto = db.getAutoCommit();
        db.setAutoCommit(false);
        try (PreparedStatement ps = db.prepareStatement("INSERT INTO " + t + " VALUES (" + ph + ")")) {
            int batch = 0;
            for (Map<String, Object> row : rows) {
                for (int i = 0; i < fields.size(); i++) ps.setString(i + 1, toCell(row.get(fields.get(i))));
                ps.addBatch();
                if (++batch % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.setAutoCommit(prevAuto);
        }
        return rows.size();
    }

    /** null → null, 그 외 → 문자열. */
    static String toCell(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
