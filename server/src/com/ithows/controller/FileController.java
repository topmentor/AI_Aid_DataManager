package com.ithows.controller;

import com.ithows.HttpUtil;
import com.ithows.aidclaude.AcContext;
import com.ithows.aidclaude.AcResp;
import com.ithows.base.ApiInfo;
import com.ithows.base.ControllerClassInfo;
import com.ithows.base.ControllerMethodInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;

/**
 * 파일 유틸 API. 모든 경로는 데이터 홈 하위로 제한(경로 탈출 차단).
 * (기존 IPC: files:readText/writeText/readLines/readBase64/copyToData/copyShapefile)
 *
 * <p>네이티브 파일 선택/저장 다이얼로그는 Electron(IPC)에 잔류. copyToData/copyShapefile은
 * Electron이 전달한 절대경로(srcPath)를 데이터 홈 data/로 복사한다(srcPath는 임의 위치 허용).</p>
 */
@ControllerClassInfo(controllerPage = "/api/_api.jsp")
public class FileController {

    @ControllerMethodInfo(id = "/api/readText.do")
    @ApiInfo(summary = "텍스트 파일 읽기", tag = "Files", method = "POST")
    public String readText(HttpSession session, HttpServletRequest request,
                           HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AcResp.error(request, "요청 본문(JSON)이 필요합니다");
        File f = safe(body.optString("path", ""));
        if (f == null) return AcResp.error(request, "허용되지 않은 경로입니다");
        if (!f.exists()) return AcResp.map(request, new JSONObject().put("content", JSONObject.NULL));
        try {
            String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            return AcResp.map(request, new JSONObject().put("content", s));
        } catch (Exception e) {
            return AcResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/writeText.do")
    @ApiInfo(summary = "텍스트 파일 쓰기", tag = "Files", method = "POST")
    public String writeText(HttpSession session, HttpServletRequest request,
                            HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AcResp.error(request, "요청 본문(JSON)이 필요합니다");
        File f = safe(body.optString("path", ""));
        if (f == null) return AcResp.error(request, "허용되지 않은 경로입니다");
        try {
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            Files.write(f.toPath(), body.optString("content", "").getBytes(StandardCharsets.UTF_8));
            return AcResp.ok(request);
        } catch (Exception e) {
            return AcResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/readLines.do")
    @ApiInfo(summary = "앞부분 N줄 읽기", tag = "Files", method = "POST")
    public String readLines(HttpSession session, HttpServletRequest request,
                            HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AcResp.error(request, "요청 본문(JSON)이 필요합니다");
        File f = safe(body.optString("path", ""));
        if (f == null || !f.exists()) return AcResp.error(request, "허용되지 않거나 없는 경로");
        int count = body.optInt("count", 50);
        try {
            List<String> all = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            org.json.JSONArray arr = new org.json.JSONArray();
            for (int i = 0; i < all.size() && i < count; i++) arr.put(all.get(i));
            return AcResp.list(request, arr);
        } catch (Exception e) {
            return AcResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/readBase64.do")
    @ApiInfo(summary = "바이너리 파일 base64", description = "PNG 등을 base64로 반환(차트 표시용).", tag = "Files", method = "POST")
    public String readBase64(HttpSession session, HttpServletRequest request,
                             HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AcResp.error(request, "요청 본문(JSON)이 필요합니다");
        File f = safe(body.optString("path", ""));
        if (f == null || !f.exists()) return AcResp.error(request, "허용되지 않거나 없는 경로");
        try {
            String b64 = Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath()));
            return AcResp.map(request, new JSONObject().put("base64", b64));
        } catch (Exception e) {
            return AcResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/copyToData.do")
    @ApiInfo(summary = "파일을 data/로 복사", tag = "Files", method = "POST")
    public String copyToData(HttpSession session, HttpServletRequest request,
                             HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AcResp.error(request, "요청 본문(JSON)이 필요합니다");
        String src = body.optString("srcPath", "");
        File s = new File(src);
        if (src.isEmpty() || !s.exists()) return AcResp.error(request, "원본 파일이 없습니다");
        try {
            File dataDir = AcContext.dataDir();
            dataDir.mkdirs();
            String name = s.getName();
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
            String baseN = (ext.isEmpty() ? name : name.substring(0, name.length() - ext.length()))
                    .replaceAll("[^a-zA-Z0-9_\\-]", "_");
            File dest = new File(dataDir, baseN + "_" + System.currentTimeMillis() + ext);
            Files.copy(s.toPath(), dest.toPath());
            return AcResp.map(request, new JSONObject().put("path", dest.getAbsolutePath()));
        } catch (Exception e) {
            return AcResp.error(request, e.getMessage());
        }
    }

    @ControllerMethodInfo(id = "/api/copyShapefile.do")
    @ApiInfo(summary = "Shapefile 세트를 data/로 복사", description = ".shp/.dbf/.shx/.prj/.cpg 동반 복사 + 인코딩 감지", tag = "Files", method = "POST")
    public String copyShapefile(HttpSession session, HttpServletRequest request,
                                HttpServletResponse response, Object command) {
        JSONObject body = bodyOrNull(request);
        if (body == null) return AcResp.error(request, "요청 본문(JSON)이 필요합니다");
        String src = body.optString("srcShpPath", "");
        File shp = new File(src);
        if (src.isEmpty() || !shp.exists()) return AcResp.error(request, "shp 파일이 없습니다");
        try {
            File dataDir = AcContext.dataDir();
            dataDir.mkdirs();
            File srcDir = shp.getParentFile();
            String srcBase = shp.getName().substring(0, shp.getName().length() - 4); // .shp 제거
            long ts = System.currentTimeMillis();
            String destBase = srcBase.replaceAll("[^a-zA-Z0-9_\\-]", "_") + "_" + ts;
            String[] exts = {".shp", ".dbf", ".shx", ".prj", ".cpg"};
            for (String ext : exts) {
                File sf = new File(srcDir, srcBase + ext);
                if (sf.exists()) Files.copy(sf.toPath(), new File(dataDir, destBase + ext).toPath());
            }
            String encoding = "euc-kr";
            File cpg = new File(dataDir, destBase + ".cpg");
            if (cpg.exists()) {
                String c = new String(Files.readAllBytes(cpg.toPath()), StandardCharsets.UTF_8).trim().toLowerCase();
                if (c.equals("utf-8") || c.equals("utf8")) encoding = "utf-8";
                else if (c.equals("utf-16") || c.equals("utf-16le")) encoding = "utf-16le";
            }
            File destShp = new File(dataDir, destBase + ".shp");
            return AcResp.map(request, new JSONObject()
                    .put("shpPath", destShp.getAbsolutePath()).put("encoding", encoding));
        } catch (Exception e) {
            return AcResp.error(request, e.getMessage());
        }
    }

    // ── 경로 보안: 데이터 홈 하위만 허용 ────────────────────────────────────
    private static File safe(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            File f = new File(path).getCanonicalFile();
            String home = AcContext.home().getCanonicalPath();
            String p = f.getPath();
            if (p.equals(home) || p.startsWith(home + File.separator)) return f;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static JSONObject bodyOrNull(HttpServletRequest request) {
        try { return HttpUtil.getBodyJson(request); } catch (Exception e) { return null; }
    }
}
