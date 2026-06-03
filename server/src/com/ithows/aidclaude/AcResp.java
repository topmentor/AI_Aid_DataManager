package com.ithows.aidclaude;

import com.ithows.ResultMap;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * AidClaude 컨트롤러 공통 응답 빌더.
 *
 * <p>프레임워크 기본 JSP({@code commonResultJson.jsp})는 값 이스케이프를 하지 않아 임의의
 * 셀 데이터(콤마/따옴표/개행 포함)에서 깨진 JSON을 만든다. 따라서 컨트롤러는 {@code org.json}으로
 * 완성된 JSON 문자열을 만들어 {@code result} 속성에 담고 {@code RESULT_SIMPLE_JSON}을 반환한다.
 * ({@code simpleResultJson2.jsp}가 {@code ${result}}를 원시 출력한다.)</p>
 *
 * <p>응답 형태: {@code {result:"OK|ERROR|NO", msg?, resultMap?, count?, resultList?}}</p>
 */
public final class AcResp {

    public static final String VIEW = "RESULT_SIMPLE_JSON";

    private AcResp() {}

    public static String ok(HttpServletRequest req) {
        return write(req, base("OK", null));
    }

    public static String okMsg(HttpServletRequest req, String msg) {
        return write(req, base("OK", msg));
    }

    public static String map(HttpServletRequest req, JSONObject resultMap) {
        JSONObject o = base("OK", null);
        o.put("resultMap", resultMap == null ? JSONObject.NULL : resultMap);
        return write(req, o);
    }

    public static String list(HttpServletRequest req, JSONArray resultList) {
        JSONObject o = base("OK", null);
        o.put("count", resultList == null ? 0 : resultList.length());
        o.put("resultList", resultList == null ? new JSONArray() : resultList);
        return write(req, o);
    }

    public static String error(HttpServletRequest req, String msg) {
        return write(req, base("ERROR", msg));
    }

    public static String no(HttpServletRequest req, String msg) {
        return write(req, base("NO", msg));
    }

    /** 임의의 완성된 JSON 객체를 그대로 응답한다. */
    public static String raw(HttpServletRequest req, JSONObject full) {
        return write(req, full);
    }

    private static JSONObject base(String result, String msg) {
        JSONObject o = new JSONObject();
        o.put("result", result);
        if (msg != null) o.put("msg", msg);
        return o;
    }

    private static String write(HttpServletRequest req, JSONObject o) {
        req.setAttribute("result", o.toString());
        return VIEW;
    }

    // ── ResultMap/Map → JSON 변환 ──────────────────────────────────────────

    public static JSONObject toJson(Map<?, ?> m) {
        JSONObject o = new JSONObject();
        if (m != null) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                Object v = e.getValue();
                o.put(String.valueOf(e.getKey()), v == null ? JSONObject.NULL : v);
            }
        }
        return o;
    }

    public static JSONArray toJsonArray(List<ResultMap> rows) {
        JSONArray a = new JSONArray();
        if (rows != null) {
            for (ResultMap m : rows) a.put(toJson(m));
        }
        return a;
    }
}
