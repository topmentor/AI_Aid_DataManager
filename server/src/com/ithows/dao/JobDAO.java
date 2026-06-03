package com.ithows.dao;

import com.ithows.ResultMap;
import com.ithows.aida.AppDb;
import com.ithows.aida.SqliteUtil;

import java.sql.Connection;
import java.util.List;

/**
 * 작업(job) 메타 DAO — 중앙 app.db {@code aida_jobs}.
 */
public class JobDAO {

    private JobDAO() {}

    public static int insert(String id, String userRequest, String status, String workspaceDir) {
        long now = System.currentTimeMillis() / 1000L;
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.update(c,
                    "INSERT INTO aida_jobs(id, user_request, status, error_msg, workspace_dir, created_at, updated_at) "
                  + "VALUES(?,?,?,?,?,?,?)",
                    new Object[]{id, userRequest, status, null, workspaceDir, now, now});
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static List<ResultMap> list() {
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.queryForMapList(c,
                    "SELECT id, user_request, status, error_msg, workspace_dir, created_at, updated_at "
                  + "FROM aida_jobs ORDER BY created_at DESC", null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ResultMap get(String id) {
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.queryForMap(c,
                    "SELECT id, user_request, status, error_msg, workspace_dir, created_at, updated_at "
                  + "FROM aida_jobs WHERE id=?", new Object[]{id});
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int updateStatus(String id, String status, String errorMsg) {
        long now = System.currentTimeMillis() / 1000L;
        try (Connection c = AppDb.conn()) {
            return SqliteUtil.update(c,
                    "UPDATE aida_jobs SET status=?, error_msg=?, updated_at=? WHERE id=?",
                    new Object[]{status, errorMsg, now, id});
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
