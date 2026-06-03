package com.ithows.aida;

import com.ithows.ResultMap;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 쿼리 히스토리 + result 테이블 백업. 기존 backup-service.ts 포팅.
 */
public final class BackupService {

    private static final int MAX_QUERY_HISTORY = 20;
    private static final int MAX_RESULT_HISTORY = 10;
    private static final Pattern Q = Pattern.compile("^query_(\\d+)\\.sql$");
    private static final Pattern R = Pattern.compile("^result_bak_(\\d+)$");

    private BackupService() {}

    /** query.sql이 있으면 history/query_NNN.sql로 백업(FIFO 최대 20). */
    public static void backupQuerySql(File workspaceDir) {
        try {
            File src = new File(workspaceDir, "query.sql");
            if (!src.exists()) return;
            String content = new String(Files.readAllBytes(src.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) return;

            File histDir = new File(workspaceDir, "history");
            histDir.mkdirs();
            List<Integer> nums = nums(histDir.list(), Q);
            int next = nums.isEmpty() ? 1 : nums.get(nums.size() - 1) + 1;
            Files.copy(src.toPath(), new File(histDir, "query_" + pad(next) + ".sql").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            if (nums.size() >= MAX_QUERY_HISTORY) {
                for (int i = 0; i <= nums.size() - MAX_QUERY_HISTORY; i++) {
                    new File(histDir, "query_" + pad(nums.get(i)) + ".sql").delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** result 테이블이 있으면 result_bak_NNN으로 복사(FIFO 최대 10). */
    public static void backupResultTable(Connection db) {
        try {
            List<String> tables = SqliteUtil.listTables(db);
            if (!tables.contains("result")) return;
            List<Integer> nums = new ArrayList<>();
            for (String t : tables) {
                Matcher m = R.matcher(t);
                if (m.matches()) nums.add(Integer.parseInt(m.group(1)));
            }
            Collections.sort(nums);
            int next = nums.isEmpty() ? 1 : nums.get(nums.size() - 1) + 1;
            String bak = "result_bak_" + pad(next);
            try (Statement st = db.createStatement()) {
                st.execute("CREATE TABLE " + SqliteUtil.quoteIdent(bak) + " AS SELECT * FROM result");
            }
            if (nums.size() >= MAX_RESULT_HISTORY) {
                for (int i = 0; i <= nums.size() - MAX_RESULT_HISTORY; i++) {
                    try (Statement st = db.createStatement()) {
                        st.execute("DROP TABLE IF EXISTS " + SqliteUtil.quoteIdent("result_bak_" + pad(nums.get(i))));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** history/query_NNN.sql 목록을 최신순으로 반환. */
    public static List<String> listQueryHistory(File workspaceDir) {
        File histDir = new File(workspaceDir, "history");
        String[] files = histDir.list();
        List<String> out = new ArrayList<>();
        if (files != null) {
            for (String f : files) if (Q.matcher(f).matches()) out.add(f);
        }
        Collections.sort(out);
        Collections.reverse(out);
        return out;
    }

    private static List<Integer> nums(String[] files, Pattern p) {
        List<Integer> nums = new ArrayList<>();
        if (files != null) {
            for (String f : files) {
                Matcher m = p.matcher(f);
                if (m.matches()) nums.add(Integer.parseInt(m.group(1)));
            }
        }
        Collections.sort(nums);
        return nums;
    }

    private static String pad(int n) { return String.format("%03d", n); }
}
