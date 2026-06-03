package com.ithows.dao;

import com.ithows.ResultMap;
import com.ithows.aida.AppDb;
import com.ithows.aida.SqliteUtil;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

/**
 * 데이터 소스 카탈로그 DAO — 중앙 app.db {@code aida_catalog}.
 * {@code config}는 JSON 문자열로 저장한다(컨트롤러에서 파싱).
 */
public class CatalogDAO {

    private CatalogDAO() {}

    public static List<ResultMap> list() {
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.queryForMapList(c,
                    "SELECT id, name, type, config, created_at FROM aida_catalog ORDER BY created_at", null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ResultMap get(String id) {
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.queryForMap(c,
                    "SELECT id, name, type, config, created_at FROM aida_catalog WHERE id=?",
                    new Object[]{id});
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** 신규 소스 추가. 생성된 id 반환(실패 시 null). */
    public static String add(String name, String type, String configJson) {
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis() / 1000L;
        try (Connection c = AppDb.conn()) {
            int n = SqliteUtil.update(c,
                    "INSERT INTO aida_catalog(id, name, type, config, created_at) VALUES(?,?,?,?,?)",
                    new Object[]{id, name, type, configJson, now});
            return n > 0 ? id : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int update(String id, String name, String type, String configJson) {
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.update(c,
                    "UPDATE aida_catalog SET name=?, type=?, config=? WHERE id=?",
                    new Object[]{name, type, configJson, id});
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int remove(String id) {
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.update(c, "DELETE FROM aida_catalog WHERE id=?", new Object[]{id});
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
