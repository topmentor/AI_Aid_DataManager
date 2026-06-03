package com.ithows.dao;

import com.ithows.JdbcDao;
import com.ithows.ResultMap;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 튜토리얼용 DAO — 기본 CRUD + 검색/페이지네이션 + 트랜잭션 패턴을 보여주기 위한 예제.
 *
 * <pre>
 * -- 테이블 스키마 (MariaDB/MySQL)
 * CREATE TABLE tutorial_item (
 *     itemId      VARCHAR(32)   PRIMARY KEY,
 *     name        VARCHAR(100)  NOT NULL,
 *     category    VARCHAR(50),
 *     price       INT           DEFAULT 0,
 *     active      TINYINT       DEFAULT 1,
 *     createTime  DATETIME      DEFAULT CURRENT_TIMESTAMP
 * );
 * </pre>
 *
 * 규칙:
 *   - 모든 메서드는 static 으로 작성 (SSF 프레임워크 관행).
 *   - 반드시 파라미터화 쿼리(?) 사용. 문자열 연결 금지.
 *   - SQLException 은 DAO 에서 처리하고 호출부는 null/0 으로 실패를 판별.
 */
public class TutorialDAO {

    // ----------------------------------------------------------------------
    // 단건 조회
    // ----------------------------------------------------------------------
    public static ResultMap selectById(String itemId) {
        String sql = "SELECT itemId, name, category, price, active, createTime " +
                     "FROM tutorial_item WHERE itemId = ?";
        try {
            return JdbcDao.queryForMapObject(sql, new Object[]{itemId});
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ----------------------------------------------------------------------
    // 전체 목록 (활성만)
    // ----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public static List<ResultMap> selectAllActive() {
        String sql = "SELECT itemId, name, category, price, createTime " +
                     "FROM tutorial_item WHERE active = ? " +
                     "ORDER BY createTime DESC";
        try {
            return JdbcDao.queryForMapList(sql, new Object[]{1});
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ----------------------------------------------------------------------
    // 검색 + 페이지네이션 — 키워드 / 카테고리 / 페이지 번호(1-based) / 페이지 크기
    // keyword 가 빈 문자열이면 전체 매칭. category 도 마찬가지.
    // ----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public static List<ResultMap> search(String keyword, String category, int pageNo, int pageSize) {
        if (pageNo < 1)      pageNo = 1;
        if (pageSize < 1)    pageSize = 20;
        if (pageSize > 200)  pageSize = 200;   // 과도한 조회 방지

        int offset = (pageNo - 1) * pageSize;

        String sql = "SELECT itemId, name, category, price, createTime " +
                     "FROM tutorial_item " +
                     "WHERE active = 1 " +
                     "  AND (? = '' OR name LIKE ?) " +
                     "  AND (? = '' OR category = ?) " +
                     "ORDER BY createTime DESC " +
                     "LIMIT ?, ?";

        Object[] params = new Object[]{
            keyword, "%" + keyword + "%",
            category, category,
            offset, pageSize
        };

        try {
            return JdbcDao.queryForMapList(sql, params);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 검색 결과 총 건수 (페이지네이션의 total 계산용)
    public static int countSearch(String keyword, String category) {
        String sql = "SELECT COUNT(*) FROM tutorial_item " +
                     "WHERE active = 1 " +
                     "  AND (? = '' OR name LIKE ?) " +
                     "  AND (? = '' OR category = ?)";

        Object[] params = new Object[]{
            keyword, "%" + keyword + "%",
            category, category
        };

        try {
            return JdbcDao.queryForInt(sql, params);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ----------------------------------------------------------------------
    // 등록 — 성공 시 1, 실패 시 0
    // ----------------------------------------------------------------------
    public static int insertItem(String itemId, String name, String category, int price) {
        String sql = "INSERT INTO tutorial_item (itemId, name, category, price) " +
                     "VALUES (?, ?, ?, ?)";
        try {
            return JdbcDao.update(sql, new Object[]{itemId, name, category, price});
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ----------------------------------------------------------------------
    // 수정 — 변경된 행 수 반환
    // ----------------------------------------------------------------------
    public static int updateItem(String itemId, String name, String category, int price) {
        String sql = "UPDATE tutorial_item SET name = ?, category = ?, price = ? " +
                     "WHERE itemId = ?";
        try {
            return JdbcDao.update(sql, new Object[]{name, category, price, itemId});
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ----------------------------------------------------------------------
    // 비활성화 (soft delete)
    // ----------------------------------------------------------------------
    public static int deactivate(String itemId) {
        String sql = "UPDATE tutorial_item SET active = 0 WHERE itemId = ?";
        try {
            return JdbcDao.update(sql, new Object[]{itemId});
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ----------------------------------------------------------------------
    // 전체 활성 건수
    // ----------------------------------------------------------------------
    public static int countActive() {
        try {
            return JdbcDao.queryForInt("SELECT COUNT(*) FROM tutorial_item WHERE active = 1",
                                       new Object[]{});
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ----------------------------------------------------------------------
    // 트랜잭션 — 여러 건을 원자적으로 삽입. 하나라도 실패하면 전체 롤백.
    //   ※ SSF 의 JdbcDao 는 Connection 을 autoCommit=false 로 돌려주므로
    //     updateNoCommit() 으로 같은 커넥션에서 여러 쿼리를 실행한 뒤
    //     마지막에 conn.commit() 또는 conn.rollback() 을 호출한다.
    //
    //   items: String[] 배열 요소 = { itemId, name, category, priceStr }
    //   반환값: 성공 시 삽입 건수, 실패 시 -1
    // ----------------------------------------------------------------------
    public static int insertItemsAtomic(List<String[]> items) {
        if (items == null || items.isEmpty()) return 0;

        Connection conn = null;
        int inserted = 0;
        try {
            conn = JdbcDao.getConnection();     // autoCommit=false

            String sql = "INSERT INTO tutorial_item (itemId, name, category, price) " +
                         "VALUES (?, ?, ?, ?)";

            for (String[] row : items) {
                if (row == null || row.length < 4) {
                    throw new SQLException("Invalid row in batch input");
                }
                int price = 0;
                try { price = Integer.parseInt(row[3]); } catch (Exception ignore) {}

                inserted += JdbcDao.updateNoCommit(
                        conn, sql,
                        new Object[]{ row[0], row[1], row[2], price });
            }

            conn.commit();
            return inserted;

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignore) {}
            }
            return -1;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignore) {}
            }
        }
    }
}
