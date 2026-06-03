package com.ithows.aidclaude;

import com.ithows.ResultMap;
import com.ithows.aidclaude.model.SourceRef;
import com.ithows.dao.CatalogDAO;
import com.ithows.dao.JobDAO;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 모든 job DB의 고아 테이블(미사용) 탐지/삭제.
 * 보존: result, result_bak_NNN, 현재 카탈로그 소스에 매핑되는 테이블. (기존 index.ts buildOrphanFilter 포팅)
 */
public final class OrphanService {

    private OrphanService() {}

    private static final class Filter {
        final Set<String> exact = new HashSet<>();
        final List<String> prefixes = new ArrayList<>();
        boolean isOrphan(String t) {
            if (t.equals("result")) return false;
            if (t.matches("result_bak_\\d+")) return false;
            if (exact.contains(t)) return false;
            for (String p : prefixes) if (t.startsWith(p)) return false;
            return true;
        }
    }

    private static Filter buildFilter() {
        Filter f = new Filter();
        List<ResultMap> sources = CatalogDAO.list();
        if (sources != null) {
            for (ResultMap r : sources) {
                SourceRef s = SourceRef.from(r);
                String base = Names.toTableName(s.name);
                if ("mariadb".equals(s.type)) f.prefixes.add(base + "_");
                else f.exact.add(base);
            }
        }
        return f;
    }

    /** [{jobId, jobLabel, tables:[]}] */
    public static JSONArray list() {
        Filter f = buildFilter();
        JSONArray out = new JSONArray();
        List<ResultMap> jobs = JobDAO.list();
        if (jobs == null) return out;
        for (ResultMap job : jobs) {
            File db = new File(String.valueOf(job.get("workspace_dir")), "data.db");
            if (!db.exists()) continue;
            try (Connection c = SqliteUtil.open(db.getAbsolutePath())) {
                JSONArray orphans = new JSONArray();
                for (String t : SqliteUtil.listTables(c)) if (f.isOrphan(t)) orphans.put(t);
                if (orphans.length() > 0) {
                    String label = String.valueOf(job.get("user_request"));
                    if (label != null && label.length() > 40) label = label.substring(0, 40);
                    out.put(new JSONObject().put("jobId", String.valueOf(job.get("id")))
                            .put("jobLabel", label).put("tables", orphans));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return out;
    }

    /** 전체 고아 테이블 삭제, 삭제 개수 반환. */
    public static int dropAll() {
        Filter f = buildFilter();
        int dropped = 0;
        List<ResultMap> jobs = JobDAO.list();
        if (jobs == null) return 0;
        for (ResultMap job : jobs) {
            File db = new File(String.valueOf(job.get("workspace_dir")), "data.db");
            if (!db.exists()) continue;
            try (Connection c = SqliteUtil.open(db.getAbsolutePath())) {
                List<String> orphans = new ArrayList<>();
                for (String t : SqliteUtil.listTables(c)) if (f.isOrphan(t)) orphans.add(t);
                for (String t : orphans) {
                    try (Statement st = c.createStatement()) {
                        st.execute("DROP TABLE IF EXISTS " + SqliteUtil.quoteIdent(t));
                        dropped++;
                    }
                }
                if (!orphans.isEmpty()) JobService.updateClaudeMdFromDb(String.valueOf(job.get("id")));
            } catch (Exception e) { e.printStackTrace(); }
        }
        return dropped;
    }
}
