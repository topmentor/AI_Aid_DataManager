package com.ithows.controller;

import com.ithows.BaseDebug;
import com.ithows.FileInfo;
import com.ithows.HttpUtil;
import com.ithows.JakartaUpload;
import com.ithows.ResultMap;
import com.ithows.SessionInfo;
import com.ithows.base.ApiInfo;
import com.ithows.base.ApiKeyRequired;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;
import com.ithows.dao.TutorialDAO;
import com.ithows.service.UploadConst;
import com.ithows.util.DateTimeUtils;
import com.ithows.util.PythonCallUtil;
import com.ithows.util.UtilJSON;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * SSF 프레임워크 REST API 튜토리얼 컨트롤러.
 *
 * refcode/ 의 실제 컨트롤러에서 추출한 대표 패턴들을 하나의 클래스에 모아
 * 프레임워크 사용법을 한눈에 익힐 수 있도록 구성한 예제다.
 *
 * 전제 테이블:
 * <pre>
 *   CREATE TABLE tutorial_item (
 *       itemId      VARCHAR(32)   PRIMARY KEY,
 *       name        VARCHAR(100)  NOT NULL,
 *       category    VARCHAR(50),
 *       price       INT           DEFAULT 0,
 *       active      TINYINT       DEFAULT 1,
 *       createTime  DATETIME      DEFAULT CURRENT_TIMESTAMP
 *   );
 * </pre>
 *
 * 담고 있는 패턴:
 *   1.  헬스체크                    GET  /tutorial/ping.do
 *   2.  파라미터 에코                GET  /tutorial/echo.do
 *   3.  단건 조회                   GET  /tutorial/getItem.do
 *   4.  목록 조회                   GET  /tutorial/getList.do
 *   5.  검색 + 페이지네이션           GET  /tutorial/searchItems.do
 *   6.  POST JSON 생성              POST /tutorial/createItem.do
 *   7.  POST JSON 수정              POST /tutorial/updateItem.do
 *   8.  삭제 (soft)                 POST /tutorial/deleteItem.do
 *   9.  트랜잭션 (일괄 삽입)          POST /tutorial/createItemsBatch.do
 *  10.  DB → 파일 export            GET  /tutorial/exportItems.do
 *  11.  파일 업로드 (multipart)      POST /tutorial/uploadFile.do
 *  12.  파일 다운로드                GET  /tutorial/downloadFile.do
 *  13.  Python 스크립트 대리 호출     POST /tutorial/callPython.do
 *  14.  API Key 인증                GET  /tutorial/secureApi.do   (@ApiKeyRequired)
 *  15.  로그인 + 보안 레벨            GET  /tutorial/adminPing.do  (loginRequired + level 1)
 */
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class TutorialController {

    static {
        BaseDebug.info("---------------------- " + TutorialController.class.toString() + " Loading!!");
    }

    // =====================================================================
    // 1. 헬스체크 — 파라미터 없음, 간단한 JSON 상태 응답
    //    반환값 "RESULT_PAGE_JSON" 은 simpleResultJson.jsp 로 forward 된다.
    //    ( { "result": "...", "msg": "...", "data": "..." } 포맷)
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/ping.do")
    @ApiInfo(summary = "Ping", description = "서비스가 살아있는지 확인한다.",
             tag = "Tutorial", method = "GET")
    public String ping(HttpSession session, HttpServletRequest request,
                       HttpServletResponse response, Object command) {

        request.setAttribute("result", "OK");
        request.setAttribute("msg",    "pong");
        request.setAttribute("data",   DateTimeUtils.getTimeDateNow());

        return "RESULT_PAGE_JSON";
    }

    // =====================================================================
    // 2. 파라미터 에코 — HttpUtil.getParameterXxx() 사용법 샘플
    //    기본값 지정으로 누락에 안전하게 대응.
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/echo.do")
    @ApiInfo(
        summary = "Echo",
        description = "전달받은 파라미터를 그대로 돌려준다. 타입별 파라미터 추출 예시.",
        tag = "Tutorial",
        method = "GET",
        parameters = {
            @ApiInfo.Param(name = "text",    type = "string",  description = "문자열",  example = "hello"),
            @ApiInfo.Param(name = "number",  type = "integer", description = "정수",    example = "42"),
            @ApiInfo.Param(name = "ratio",   type = "number",  description = "실수",    example = "0.5"),
            @ApiInfo.Param(name = "enabled", type = "boolean", description = "참/거짓", example = "true")
        }
    )
    public String echo(HttpSession session, HttpServletRequest request,
                       HttpServletResponse response, Object command) {

        String  text    = HttpUtil.getParameterString (request, "text",    "");
        int     number  = HttpUtil.getParameterInt    (request, "number",  0);
        double  ratio   = HttpUtil.getParameterDouble (request, "ratio",   0.0);
        boolean enabled = HttpUtil.getParameterBoolean(request, "enabled");

        ResultMap map = new ResultMap();
        map.put("text",    text);
        map.put("number",  number);
        map.put("ratio",   ratio);
        map.put("enabled", enabled);

        request.setAttribute("result",    "OK");
        request.setAttribute("msg",       "echo");
        request.setAttribute("restime",   DateTimeUtils.getTimeDateNow());
        request.setAttribute("resultMap", map);

        return "RESULT_COMMON_JSON";
    }

    // =====================================================================
    // 3. 단건 조회 — GET, itemId 로 한 건을 가져온다.
    //    결과가 없으면 result="NO", 있으면 result="OK" + resultMap.
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/getItem.do")
    @ApiInfo(
        summary = "아이템 단건 조회",
        description = "itemId 로 아이템 한 건을 조회한다.",
        tag = "Tutorial",
        method = "GET",
        parameters = {
            @ApiInfo.Param(name = "itemId", type = "string", required = true,
                           description = "아이템 ID", example = "ITEM-001")
        }
    )
    public String getItem(HttpSession session, HttpServletRequest request,
                          HttpServletResponse response, Object command) {

        String itemId = HttpUtil.getParameterString(request, "itemId", "");

        // 필수 파라미터 검증
        if (itemId.isEmpty()) {
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "itemId is required");
            return "RESULT_COMMON_JSON";
        }

        try {
            ResultMap item = TutorialDAO.selectById(itemId);

            if (item == null || item.isEmpty()) {
                request.setAttribute("result", "NO");
                request.setAttribute("msg",    "Item not found");
            } else {
                request.setAttribute("result",    "OK");
                request.setAttribute("msg",       "Success");
                request.setAttribute("resultMap", item);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Server error");
        }

        request.setAttribute("restime", DateTimeUtils.getTimeDateNow());
        return "RESULT_COMMON_JSON";
    }

    // =====================================================================
    // 4. 목록 조회 — 전체 활성 아이템
    //    List<ResultMap> 은 request.setAttribute("resultList", ...) 로 전달.
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/getList.do")
    @ApiInfo(
        summary = "아이템 전체 목록",
        description = "활성(active=1) 아이템 전체를 최신순으로 반환한다.",
        tag = "Tutorial", method = "GET"
    )
    public String getList(HttpSession session, HttpServletRequest request,
                          HttpServletResponse response, Object command) {

        try {
            List<ResultMap> list = TutorialDAO.selectAllActive();

            if (list == null || list.isEmpty()) {
                request.setAttribute("result", "NO");
                request.setAttribute("msg",    "No data");
                request.setAttribute("resultList", new ArrayList<ResultMap>());
            } else {
                request.setAttribute("result",     "OK");
                request.setAttribute("msg",        "Success");
                request.setAttribute("resultList", list);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Server error");
        }

        request.setAttribute("restime", DateTimeUtils.getTimeDateNow());
        return "RESULT_COMMON_JSON";
    }

    // =====================================================================
    // 5. 검색 + 페이지네이션
    //    keyword: name LIKE '%keyword%'    category: 정확 매칭
    //    pageNo (1-based), pageSize
    //    응답에 total, pageNo, pageSize, resultList 를 모두 담는다.
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/searchItems.do")
    @ApiInfo(
        summary = "아이템 검색 (페이지네이션)",
        description = "이름 부분일치 + 카테고리 일치로 검색. 페이지/총건수 포함.",
        tag = "Tutorial",
        method = "GET",
        parameters = {
            @ApiInfo.Param(name = "keyword",  type = "string",  description = "이름 부분일치", example = "phone"),
            @ApiInfo.Param(name = "category", type = "string",  description = "카테고리",     example = "electronics"),
            @ApiInfo.Param(name = "pageNo",   type = "integer", description = "1부터 시작",    example = "1"),
            @ApiInfo.Param(name = "pageSize", type = "integer", description = "페이지 크기(기본 20, 최대 200)", example = "20")
        }
    )
    public String searchItems(HttpSession session, HttpServletRequest request,
                              HttpServletResponse response, Object command) {

        String keyword  = HttpUtil.getParameterString(request, "keyword",  "");
        String category = HttpUtil.getParameterString(request, "category", "");
        int    pageNo   = HttpUtil.getParameterInt   (request, "pageNo",   1);
        int    pageSize = HttpUtil.getParameterInt   (request, "pageSize", 20);

        try {
            List<ResultMap> list  = TutorialDAO.search(keyword, category, pageNo, pageSize);
            int             total = TutorialDAO.countSearch(keyword, category);

            ResultMap meta = new ResultMap();
            meta.put("total",    total);
            meta.put("pageNo",   pageNo);
            meta.put("pageSize", pageSize);
            meta.put("pageCount", (total + pageSize - 1) / pageSize);

            request.setAttribute("result",     "OK");
            request.setAttribute("msg",        "Success");
            request.setAttribute("resultMap",  meta);
            request.setAttribute("resultList", list == null ? new ArrayList<ResultMap>() : list);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Server error");
        }

        request.setAttribute("restime", DateTimeUtils.getTimeDateNow());
        return "RESULT_COMMON_JSON";
    }

    // =====================================================================
    // 6. POST JSON body 로 생성
    //    body 예시: { "itemId":"ITEM-100", "name":"Widget", "category":"misc", "price":1990 }
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/createItem.do")
    @ApiInfo(
        summary = "아이템 등록",
        description = "JSON body 로 새 아이템을 등록한다.",
        tag = "Tutorial",
        method = "POST",
        parameters = {
            @ApiInfo.Param(name = "itemId",   type = "string",  required = true, description = "아이템 ID"),
            @ApiInfo.Param(name = "name",     type = "string",  required = true, description = "이름"),
            @ApiInfo.Param(name = "category", type = "string",  description = "카테고리"),
            @ApiInfo.Param(name = "price",    type = "integer", description = "가격",  example = "9900")
        }
    )
    public String createItem(HttpSession session, HttpServletRequest request,
                             HttpServletResponse response, Object command) {

        JSONObject jParam = null;
        try {
            jParam = HttpUtil.getBodyJson(request);
        } catch (Exception e) {
            jParam = null;
        }

        if (jParam == null) {
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Invalid JSON body");
            return "RESULT_COMMON_JSON";
        }

        try {
            String itemId   = jParam.getString("itemId");
            String name     = jParam.getString("name");
            String category = jParam.optString("category", "");
            int    price    = jParam.optInt("price", 0);

            if (itemId.isEmpty() || name.isEmpty()) {
                request.setAttribute("result", "ERROR");
                request.setAttribute("msg",    "itemId and name are required");
                return "RESULT_COMMON_JSON";
            }

            int rows = TutorialDAO.insertItem(itemId, name, category, price);

            if (rows > 0) {
                request.setAttribute("result", "OK");
                request.setAttribute("msg",    "Created");
            } else {
                request.setAttribute("result", "ERROR");
                request.setAttribute("msg",    "Failed to insert (duplicate ID?)");
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Server error");
        }

        request.setAttribute("restime", DateTimeUtils.getTimeDateNow());
        return "RESULT_COMMON_JSON";
    }

    // =====================================================================
    // 7. POST JSON body 로 수정
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/updateItem.do")
    @ApiInfo(
        summary = "아이템 수정",
        description = "JSON body 의 itemId 에 해당하는 아이템을 수정한다.",
        tag = "Tutorial",
        method = "POST",
        parameters = {
            @ApiInfo.Param(name = "itemId",   type = "string",  required = true, description = "수정할 아이템 ID"),
            @ApiInfo.Param(name = "name",     type = "string",  required = true, description = "새 이름"),
            @ApiInfo.Param(name = "category", type = "string",  description = "새 카테고리"),
            @ApiInfo.Param(name = "price",    type = "integer", description = "새 가격")
        }
    )
    public String updateItem(HttpSession session, HttpServletRequest request,
                             HttpServletResponse response, Object command) {

        JSONObject jParam = null;
        try {
            jParam = HttpUtil.getBodyJson(request);
        } catch (Exception e) {
            jParam = null;
        }

        if (jParam == null) {
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Invalid JSON body");
            return "RESULT_COMMON_JSON";
        }

        try {
            String itemId   = jParam.getString("itemId");
            String name     = jParam.getString("name");
            String category = jParam.optString("category", "");
            int    price    = jParam.optInt("price", 0);

            int rows = TutorialDAO.updateItem(itemId, name, category, price);

            if (rows > 0) {
                request.setAttribute("result", "OK");
                request.setAttribute("msg",    "Updated");
            } else {
                request.setAttribute("result", "NO");
                request.setAttribute("msg",    "No matching item");
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Server error");
        }

        request.setAttribute("restime", DateTimeUtils.getTimeDateNow());
        return "RESULT_COMMON_JSON";
    }

    // =====================================================================
    // 8. 삭제 (soft delete — active=0)
    //    GET 파라미터 또는 JSON body 둘 다 허용.
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/deleteItem.do")
    @ApiInfo(
        summary = "아이템 비활성화",
        description = "itemId 에 해당하는 아이템을 soft delete 한다 (active=0).",
        tag = "Tutorial",
        method = "POST",
        parameters = {
            @ApiInfo.Param(name = "itemId", type = "string", required = true, description = "아이템 ID")
        }
    )
    public String deleteItem(HttpSession session, HttpServletRequest request,
                             HttpServletResponse response, Object command) {

        // GET/POST 모두 허용: 파라미터 먼저 확인 후 없으면 body 에서 추출
        String itemId = HttpUtil.getParameterString(request, "itemId", "");
        if (itemId.isEmpty()) {
            try {
                JSONObject body = HttpUtil.getBodyJson(request);
                if (body != null) itemId = body.optString("itemId", "");
            } catch (Exception ignore) {
            }
        }

        if (itemId.isEmpty()) {
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "itemId is required");
            return "RESULT_COMMON_JSON";
        }

        try {
            int rows = TutorialDAO.deactivate(itemId);
            if (rows > 0) {
                request.setAttribute("result", "OK");
                request.setAttribute("msg",    "Deactivated");
            } else {
                request.setAttribute("result", "NO");
                request.setAttribute("msg",    "No matching item");
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Server error");
        }

        request.setAttribute("restime", DateTimeUtils.getTimeDateNow());
        return "RESULT_COMMON_JSON";
    }

    // =====================================================================
    // 9. 트랜잭션 — 여러 건을 원자적으로 삽입
    //    body: { "items": [ {"itemId":"A1","name":"a","category":"x","price":100}, ... ] }
    //    TutorialDAO.insertItemsAtomic() 이 내부에서 commit / rollback 처리.
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/createItemsBatch.do")
    @ApiInfo(
        summary = "아이템 일괄 등록 (트랜잭션)",
        description = "여러 아이템을 한 트랜잭션으로 등록한다. 하나라도 실패하면 전체 롤백.",
        tag = "Tutorial",
        method = "POST",
        responseDescription = "inserted 필드에 실제 삽입 건수 포함"
    )
    public String createItemsBatch(HttpSession session, HttpServletRequest request,
                                   HttpServletResponse response, Object command) {

        JSONObject jParam = null;
        try {
            jParam = HttpUtil.getBodyJson(request);
        } catch (Exception e) {
            jParam = null;
        }

        if (jParam == null || !jParam.has("items")) {
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Body must contain 'items' array");
            return "RESULT_COMMON_JSON";
        }

        try {
            JSONArray arr = jParam.getJSONArray("items");
            List<String[]> rows = new ArrayList<String[]>();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                rows.add(new String[]{
                    o.getString("itemId"),
                    o.getString("name"),
                    o.optString("category", ""),
                    String.valueOf(o.optInt("price", 0))
                });
            }

            int inserted = TutorialDAO.insertItemsAtomic(rows);

            ResultMap meta = new ResultMap();
            meta.put("requested", rows.size());
            meta.put("inserted",  inserted);

            if (inserted >= 0) {
                request.setAttribute("result",    "OK");
                request.setAttribute("msg",       "Batch inserted");
                request.setAttribute("resultMap", meta);
            } else {
                request.setAttribute("result",    "ERROR");
                request.setAttribute("msg",       "Transaction rolled back");
                request.setAttribute("resultMap", meta);
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Server error: " + e.getMessage());
        }

        request.setAttribute("restime", DateTimeUtils.getTimeDateNow());
        return "RESULT_COMMON_JSON";
    }

    // =====================================================================
    // 10. 파일 다운로드 — 아이템 목록을 JSON 파일로 export
    //     1) 임시 파일에 JSON 을 저장
    //     2) HttpUtil.sendBinaryFileToClient 로 클라이언트에 전송
    //     3) return "NO_PAGE" 로 JSP forward 를 건너뛴다.
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/exportItems.do")
    @ApiInfo(
        summary = "아이템 목록 JSON 다운로드",
        description = "활성 아이템 전체를 JSON 파일로 다운로드한다.",
        tag = "Tutorial", method = "GET"
    )
    public String exportItems(HttpSession session, HttpServletRequest request,
                              HttpServletResponse response, Object command) {

        try {
            List<ResultMap> list = TutorialDAO.selectAllActive();
            if (list == null) list = new ArrayList<ResultMap>();

            // ResultMap 리스트를 JSON 으로 직렬화
            JSONArray arr = UtilJSON.convertArrayListToJSONArray(new ArrayList<ResultMap>(list));
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("exportedAt", DateTimeUtils.getTimeDateNow());
            jsonObj.put("count",      arr.length());
            jsonObj.put("items",      arr);

            // 임시 파일 경로 (구성에 따라 UploadConst.getResultLogFileDir() 등 프로젝트 표준 사용 권장)
            String filename = "tutorial_export_" + DateTimeUtils.getTimeDateNow2() + ".json";
            String tempDir  = System.getProperty("java.io.tmpdir");
            if (!tempDir.endsWith("/") && !tempDir.endsWith("\\")) tempDir += java.io.File.separator;
            String fullPath = tempDir + filename;

            boolean ok = UtilJSON.writeJsonToFile(jsonObj, fullPath);
            if (!ok) {
                // 파일 생성 실패 → JSON 에러 응답
                request.setAttribute("result", "ERROR");
                request.setAttribute("msg",    "Failed to write temp file");
                return "RESULT_COMMON_JSON";
            }

            HttpUtil.sendBinaryFileToClient(request, response, fullPath);

        } catch (IOException e) {
            e.printStackTrace();
            // 응답이 이미 시작됐을 수 있으므로 여기선 로그만 남기고 NO_PAGE 반환
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Server error: " + e.getMessage());
            return "RESULT_COMMON_JSON";
        }

        return "NO_PAGE";
    }

    // =====================================================================
    // 11. 파일 업로드 — multipart/form-data 로 전송되는 단일 파일을 서버에 저장
    //
    //     core helper : com.ithows.JakartaUpload
    //     form field  : name="file" (하나만 받는 예제 — 여러 개 받으려면
    //                   mRequest.getFileInfo("file2"), ... 로 확장 가능)
    //     저장 위치   : UploadConst.getTempUploadDir() + "/tutorial/"
    //     파일명 보호 : FilenameUtils.getName() + timestamp prefix 로 충돌/경로
    //                   주입 방지
    //
    //     호출 예:
    //       curl -F "file=@hello.txt" \
    //            http://localhost:8088/SSF2026/tutorial/uploadFile.do
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/uploadFile.do")
    @ApiInfo(
        summary = "파일 업로드",
        description = "multipart/form-data 의 file 필드로 단일 파일을 업로드한다. " +
                      "저장된 서버 파일명은 timestamp_originalName 형태이며, " +
                      "응답의 savedName 을 /tutorial/downloadFile.do 의 fileName 파라미터로 넘겨 다운로드할 수 있다.",
        tag = "Tutorial",
        method = "POST",
        parameters = {
            @ApiInfo.Param(name = "file", type = "file", required = true,
                           description = "업로드할 단일 파일 (form field name: file)")
        },
        responseDescription = "resultMap 에 originalName, savedName, sizeBytes, savedPath 를 담아 반환"
    )
    public String uploadFile(HttpSession session, HttpServletRequest request,
                             HttpServletResponse response, Object command) {

        // 1. 저장 디렉토리 준비
        String uploadDir = UploadConst.getTempUploadDir() + "/tutorial";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        try {
            // 2. multipart 파싱 — 1GB 제한, UTF-8
            JakartaUpload mRequest = new JakartaUpload(
                    request, 1024L * 1024L * 1024L, uploadDir, "UTF-8");

            // 3. name="file" 필드의 파일 정보 획득
            FileInfo fInfo = mRequest.getFileInfo("file");
            if (fInfo == null || fInfo.getFileName() == null || fInfo.getFileName().isEmpty()) {
                request.setAttribute("result", "ERROR");
                request.setAttribute("msg",    "No file received (expected form field 'file')");
                return "RESULT_COMMON_JSON";
            }

            // 4. 안전한 파일명 구성 (경로 주입 방지 + 충돌 방지)
            String originalName = FilenameUtils.getName(fInfo.getFileName());
            String savedName    = DateTimeUtils.getTimeDateNow2() + "_" + originalName;
            String savedPath    = uploadDir + "/" + savedName;

            // 5. JakartaUpload 가 이미 uploadDir 에 originalName 으로 저장했으므로
            //    충돌 방지용 savedName 으로 rename
            File saved = new File(uploadDir + "/" + originalName);
            if (saved.exists()) {
                saved.renameTo(new File(savedPath));
            }

            ResultMap meta = new ResultMap();
            meta.put("originalName", originalName);
            meta.put("savedName",    savedName);
            meta.put("sizeBytes",    fInfo.getFileSize());
            meta.put("savedPath",    savedPath);

            request.setAttribute("result",    "OK");
            request.setAttribute("msg",       "Uploaded");
            request.setAttribute("resultMap", meta);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "Upload failed: " + e.getMessage());
        }

        request.setAttribute("restime", DateTimeUtils.getTimeDateNow());
        return "RESULT_COMMON_JSON";
    }

    // =====================================================================
    // 12. 파일 다운로드 — uploadFile.do 로 저장해둔 파일을 내려받는다.
    //
    //     core helper : HttpUtil.sendBinaryFileToClient()
    //                   (Content-Disposition / Content-Length / stream 복사 자동 처리)
    //     보안        : FilenameUtils.getName() 로 basename 만 추출 →
    //                   "../" 같은 path traversal 차단.
    //     반환값      : "NO_PAGE" — 파일 전송 후 JSP forward 생략
    //
    //     호출 예:
    //       curl -OJ \
    //         "http://localhost:8088/SSF2026/tutorial/downloadFile.do?fileName=20260421103000_hello.txt"
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/downloadFile.do")
    @ApiInfo(
        summary = "파일 다운로드",
        description = "uploadFile.do 로 업로드한 파일을 fileName 파라미터로 다운로드한다. " +
                      "path traversal 방지를 위해 서버는 fileName 의 basename 만 사용한다.",
        tag = "Tutorial",
        method = "GET",
        parameters = {
            @ApiInfo.Param(name = "fileName", type = "string", required = true,
                           description = "uploadFile.do 응답의 savedName 값",
                           example = "20260421103000_hello.txt")
        }
    )
    public String downloadFile(HttpSession session, HttpServletRequest request,
                               HttpServletResponse response, Object command) {

        String rawName = HttpUtil.getParameterString(request, "fileName", "");
        if (rawName.isEmpty()) {
            request.setAttribute("result", "ERROR");
            request.setAttribute("msg",    "fileName is required");
            return "RESULT_COMMON_JSON";
        }

        // Path traversal 방어: basename 만 추출
        String safeName = FilenameUtils.getName(rawName);
        String fullPath = UploadConst.getTempUploadDir() + "/tutorial/" + safeName;

        File target = new File(fullPath);
        if (!target.exists() || !target.isFile()) {
            request.setAttribute("result", "NO");
            request.setAttribute("msg",    "File not found: " + safeName);
            return "RESULT_COMMON_JSON";
        }

        try {
            HttpUtil.sendBinaryFileToClient(request, response, fullPath);
        } catch (IOException e) {
            e.printStackTrace();
            // 응답 스트림이 이미 열렸을 수 있으므로 여기선 로그만 남기고 NO_PAGE 반환
        }

        return "NO_PAGE";
    }

    // =====================================================================
    // 13. Python 스크립트 대리 호출 — 외부 Python 프로세스로 작업 위임
    //
    //   흐름:
    //     [Java]   request JSON 파일 생성
    //       │
    //       ▼
    //     [Python] python_process/<script>.py 실행
    //              - --request  : 요청 파일 경로
    //              - --response : 응답 파일 경로
    //       │
    //       ▼
    //     [Java]   response JSON 파일 읽기 → 응답에 그대로 전달
    //
    //   요청 body 예시:
    //     {
    //       "script":  "tutorial_echo.py",      // 선택, 기본 tutorial_echo.py
    //       "payload": { "numbers":[1,2,3,4,5], "message":"hello" },
    //       "timeoutSec": 30                    // 선택, 기본 30
    //     }
    //
    //   response: resultMap 에 파이썬 응답 JSON 을 그대로 담아 반환.
    //             (RESULT_RAW_JSON → commonResultRawJson.jsp 가 구조 그대로 출력)
    //
    //   참고: com.ithows.util.PythonCallUtil — 임시파일 생성/삭제, 타임아웃,
    //         stderr 흡수, 에러시 {"result":"ERROR","msg":...} 반환을 담당.
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/callPython.do")
    @ApiInfo(
        summary = "Python 스크립트 대리 호출",
        description = "python_process 폴더의 스크립트를 호출하고 결과를 그대로 반환한다. " +
                      "자바-파이썬 간 데이터는 요청/응답 JSON 파일로 주고받는다.",
        tag = "Tutorial",
        method = "POST",
        parameters = {
            @ApiInfo.Param(name = "script",     type = "string",  description = "호출할 스크립트 파일명", example = "tutorial_echo.py"),
            @ApiInfo.Param(name = "payload",    type = "object",  description = "파이썬에 전달할 JSON 데이터"),
            @ApiInfo.Param(name = "timeoutSec", type = "integer", description = "타임아웃(초)", example = "30")
        },
        responseDescription = "resultMap 에 Python 이 반환한 JSON 을 그대로 포함"
    )
    public String callPython(HttpSession session, HttpServletRequest request,
                             HttpServletResponse response, Object command) {

        JSONObject body;
        try {
            body = HttpUtil.getBodyJson(request);
        } catch (Exception e) {
            body = null;
        }

        if (body == null) {
            request.setAttribute("result",    "ERROR");
            request.setAttribute("msg",       "Invalid JSON body");
            request.setAttribute("resultMap", "{}");
            return "RESULT_RAW_JSON";
        }

        
        
        String     script     = body.optString("script", "tutorial_echo.py");
        JSONObject payload    = body.optJSONObject("payload");
        int        timeoutSec = body.optInt("timeoutSec", 30);

        // payload 가 없으면 body 전체를 payload 로 간주 (간편 호출)
        if (payload == null) {
            try {
                payload = new JSONObject(body.toString());
                payload.remove("script");
                payload.remove("payload");
                payload.remove("timeoutSec");
            } catch (JSONException ex) {
                Logger.getLogger(TutorialController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

                
        JSONObject pyResult = PythonCallUtil.callPython(script, payload, timeoutSec);

        // 파이썬이 돌려준 result/msg 를 최상위로 승격, 전체 JSON 은 resultMap 에 raw 로 담음
        String topResult = pyResult.optString("result", "ERROR");
        String topMsg    = pyResult.optString("msg",    "");

        request.setAttribute("result",    topResult);
        request.setAttribute("msg",       topMsg);
        request.setAttribute("info",      "script=" + script);
        request.setAttribute("resultMap", pyResult.toString());

        return "RESULT_RAW_JSON";
    }

    // =====================================================================
    // 13-b. Python 환경 진단 — /tutorial/callPython.do 가 동작하지 않을 때
    //       현재 서버가 어떤 경로/명령으로 Python 을 호출하려 하는지 확인한다.
    //       브라우저에서 바로 열어볼 수 있는 GET 엔드포인트.
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/pythonInfo.do")
    @ApiInfo(
        summary = "Python 환경 진단",
        description = "현재 해석된 python_command / python_script_dir / python_temp_dir 와 " +
                      "설치된 스크립트 목록, python --version 실행 결과를 반환한다.",
        tag = "Tutorial", method = "GET"
    )
    public String pythonInfo(HttpSession session, HttpServletRequest request,
                             HttpServletResponse response, Object command) {

        JSONObject info = PythonCallUtil.getDiagnosticInfo();

        request.setAttribute("result",    "OK");
        request.setAttribute("msg",       "Python diagnostic info");
        request.setAttribute("resultMap", info.toString());

        return "RESULT_RAW_JSON";
    }

    // =====================================================================
    // 14. API Key 인증 (@ApiKeyRequired) — X-API-Key 헤더 필수
    //     검증 실패 시 DispatcherServlet 이 자동으로 거부 응답을 반환하므로
    //     여기에 도달하면 이미 인증된 요청이다.
    // =====================================================================
    @ControllerMethodInfo(id = "/tutorial/secureApi.do")
    @ApiKeyRequired
    @ApiInfo(
        summary = "API Key 보호 엔드포인트",
        description = "X-API-Key 헤더가 유효할 때만 동작한다.",
        tag = "Tutorial", method = "GET"
    )
    public String secureApi(HttpSession session, HttpServletRequest request,
                            HttpServletResponse response, Object command) {

        ResultMap map = new ResultMap();
        map.put("authenticated", true);
        map.put("callerIp",      HttpUtil.getClientIp(request));

        request.setAttribute("result",    "OK");
        request.setAttribute("msg",       "Hello, API client");
        request.setAttribute("restime",   DateTimeUtils.getTimeDateNow());
        request.setAttribute("resultMap", map);

        return "RESULT_COMMON_JSON";
    }

    // =====================================================================
    // 15. 로그인 + 보안 레벨 — 세션 로그인 필수 + 레벨 1(General) 이상
    //     loginRequired / requiredSecurityLevel 은 DispatcherServlet 에서
    //     자동 검증되므로 여기 도달 == 권한 충족.
    // =====================================================================
    @ControllerMethodInfo(
        id = "/tutorial/adminPing.do",
        loginRequired = true,
        requiredSecurityLevel = 1
    )
    @ApiInfo(
        summary = "로그인 필요 Ping",
        description = "로그인 + General 이상 보안 레벨이 필요한 엔드포인트.",
        tag = "Tutorial", method = "GET"
    )
    public String adminPing(HttpSession session, HttpServletRequest request,
                            HttpServletResponse response, Object command) {

        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

        ResultMap map = new ResultMap();
        map.put("userId", sInfo.getUserId());
        map.put("level",  sInfo.getUserSecurityLevel());

        request.setAttribute("result",    "OK");
        request.setAttribute("msg",       "Authenticated ping");
        request.setAttribute("restime",   DateTimeUtils.getTimeDateNow());
        request.setAttribute("resultMap", map);

        return "RESULT_COMMON_JSON";
    }
    
    
    ////////////////////////////////////////////////////////////////
    ///// 
    
    
    // 페이지로 전송 : 일반 케이스 
    @ControllerMethodInfo(id = "/test/getPage.do")
    public String getPage(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

        return "/test/testPage.jsp";
    }

    
    // Text 바로 리턴(단순 텍스트 전송) : 텍스트 내용 대로 전송
    @ControllerMethodInfo(id = "/test/getString.do")
    public String getString(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

        String sendText = "Hello World";

        JSONObject jObj = new JSONObject();
        jObj.put("name", "Kim");
        jObj.put("age", 10);
        
        sendText = jObj.toString();
        
        return sendText;

    }

    // 정해진 형식으로 전송 : ResultMap에 담아서 보내는 케이스 
    @ControllerMethodInfo(id = "/test/getMap.do")
    public String getMap(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

        String userIdStr = "";
        String passwdStr = "";

        if (request.getMethod().toLowerCase().equals("get")) {     //Get 방식 + 
            userIdStr = HttpUtil.getParameterString(request, "user_id", "soxadmin");
            passwdStr = HttpUtil.getParameterString(request, "user_pw", "");

        } else {      
            //Post 방식중 body에 json 텍스트로 넘길 때 (raw body)
            // 폼형식으로 전송 ( application/x-www-form-urlencoded 형식 --> map 형식 (key=value)) 모두 처리  
            
            try {
                
                JSONObject paramMap = HttpUtil.getBodyJson(request);
                if(paramMap != null){
                    userIdStr = paramMap.getString("user_id");
                    passwdStr = paramMap.getString("user_pw");
                }else{
                    System.out.println("body null");
                    
                    // POST 방식이라도 parameter가 JSON객체로 담긴 경우
                    userIdStr = HttpUtil.getParameterString(request, "user_id", "soxadmin");
                    passwdStr = HttpUtil.getParameterString(request, "user_pw", "");
                }

            } catch (Exception ex) {
                System.out.println("Request Body Parse Error");
                
            }
        }


        System.out.println("userIdStr = " + userIdStr);
        System.out.println("passwdStr = " + passwdStr);

        if (passwdStr == null || passwdStr.equals("")) {
            request.setAttribute("result", "NO");
            request.setAttribute("msg", "No password");
            return "RESULT_COMMON_JSON";
        }

        ResultMap currentMan = new ResultMap();
        currentMan.put("name", "kim");
        currentMan.put("id", userIdStr);
        currentMan.put("pw", passwdStr);

        if (currentMan == null) {
            request.setAttribute("result", "NO");
            request.setAttribute("msg", "fail");
            return "RESULT_COMMON_JSON";
        }

        request.setAttribute("result", "OK");
        request.setAttribute("msg", "Success");
        request.setAttribute("resultMap", currentMan);

        return "RESULT_COMMON_JSON";
    }

    
    // 정해진 형식으로 전송 : ArrayList<ResultMap>에 담아서 보내는 케이스 
    @ControllerMethodInfo(id = "/test/getList2.do")
    public String getList2(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

        String userIdStr = "";
        String passwdStr = "";

        if (request.getMethod().toLowerCase().equals("get")) {     //Get 방식 + 
            userIdStr = HttpUtil.getParameterString(request, "user_id", "soxadmin");
            passwdStr = HttpUtil.getParameterString(request, "user_pw", "");

        } else {      
            try {
                
                //Post 방식중 body에 json 텍스트로 넘길 때 
                JSONObject paramMap = HttpUtil.getBodyJson(request);
                if(paramMap != null){
                    userIdStr = paramMap.getString("user_id");
                    passwdStr = paramMap.getString("user_pw");
                }else{
                    System.out.println("body null");
                    userIdStr = HttpUtil.getParameterString(request, "user_id", "soxadmin");
                    passwdStr = HttpUtil.getParameterString(request, "user_pw", "");
                }

            } catch (Exception ex) {
                System.out.println("Request Param Parse Error");
            }
        }

        System.out.println("userIdStr = " + userIdStr);
        System.out.println("passwdStr = " + passwdStr);

        if (passwdStr == null || passwdStr.equals("")) {
            request.setAttribute("result", "NO");
            request.setAttribute("msg", "No password");
            return "RESULT_COMMON_JSON";
        }

        ResultMap currentMan = new ResultMap();
        currentMan.put("name", "kim");
        currentMan.put("id", userIdStr);
        currentMan.put("pw", passwdStr);

        ResultMap man = new ResultMap();
        man.put("name", "park");
        man.put("id", "p001");
        man.put("pw", "***");

        ArrayList<ResultMap> list = new ArrayList<>();
        list.add(man);
        list.add(currentMan);

        if (list == null || list.size() == 0) {
            request.setAttribute("result", "NO");
            request.setAttribute("msg", "fail");
            return "RESULT_COMMON_JSON";
        }

        request.setAttribute("result", "OK");
        request.setAttribute("msg", "Success");
        request.setAttribute("resultList", list);

        return "RESULT_COMMON_JSON";
    }

    // 임의의 형식을 JSon에 담아 보내는 케이스 
    // toString()으로 직접 내보내도 되지만 내용중에 jsp확장자가 포함될 수 있는 경우를 대비 
    @ControllerMethodInfo(id = "/test/getUserJson.do")
    public String getUserJson(HttpSession session, HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
        SessionInfo sInfo = HttpUtil.getSessionInfo(session);

        String userIdStr = "";
        String passwdStr = "";
        userIdStr = HttpUtil.getParameterString(request, "user_id", "soxadmin");
        passwdStr = HttpUtil.getParameterString(request, "user_pw", "");

        
        JSONObject jObj = new JSONObject();
        jObj.put("name", userIdStr);
        jObj.put("age", 10);

        request.setAttribute("result", jObj.toString());

        return "RESULT_SIMPLE_JSON";
    }
    
}
