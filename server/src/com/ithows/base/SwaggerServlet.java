package com.ithows.base;

import com.ithows.BaseDebug;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.reflections.Reflections;

/**
 * SSF 프레임워크의 @ControllerMethodInfo 어노테이션을 스캔하여
 * OpenAPI 3.0 JSON을 생성하고 Swagger UI를 제공하는 서블릿.
 *
 * URL 매핑:
 *   /docs/          → Swagger UI 페이지
 *   /docs/api-docs  → OpenAPI 3.0 JSON
 */
public class SwaggerServlet extends HttpServlet {

    private String openApiJson = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        BaseDebug.info("SwaggerServlet initializing...");
        buildOpenApiSpec();
        BaseDebug.info("SwaggerServlet initialized - OpenAPI spec generated");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/api-docs")) {
            serveApiDocs(request, response);
        } else {
            serveSwaggerUI(request, response);
        }
    }

    /**
     * OpenAPI 3.0 JSON 스펙 제공
     */
    private void serveApiDocs(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // 매 요청마다 재생성 옵션 (개발 편의)
        String refresh = request.getParameter("refresh");
        if ("true".equals(refresh) || openApiJson == null) {
            buildOpenApiSpec();
        }

        // contextPath를 서버 URL에 동적 주입
        String contextPath = request.getContextPath();
        String json = openApiJson;
        try {
            JSONObject spec = new JSONObject(json);
            JSONArray servers = new JSONArray();
            JSONObject server = new JSONObject();
            server.put("url", contextPath);
            server.put("description", "Current Server");
            servers.put(server);
            spec.put("servers", servers);
            json = spec.toString(2);
        } catch (JSONException e) {
            // fallback: 원본 JSON 그대로 사용
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }

    /**
     * Swagger UI JSP 페이지로 포워딩
     */
    private void serveSwaggerUI(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.getRequestDispatcher("/WEB-INF/jsp/docs/swagger-ui.jsp").forward(request, response);
    }

    /**
     * @ControllerMethodInfo 어노테이션을 스캔하여 OpenAPI 3.0 JSON을 생성
     */
    private void buildOpenApiSpec() {
        try {
            JSONObject spec = new JSONObject();
            spec.put("openapi", "3.0.3");

            // Info
            JSONObject info = new JSONObject();
            info.put("title", "SSF2026 API");
            info.put("description", "SSF2026 REST API 문서");
            info.put("version", "1.0.0");
            spec.put("info", info);

            // Paths
            JSONObject paths = new JSONObject();

            // Tags 수집용
            Map<String, String> tagMap = new TreeMap<>();

            // 컨트롤러 스캔
            Reflections reflections = new Reflections("com.ithows.controller");
            Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(ControllerClassInfo.class);

            for (Class<?> c : annotated) {
                ControllerClassInfo cci = c.getAnnotation(ControllerClassInfo.class);
                String controllerPage = cci != null ? cci.controllerPage() : "";
                String defaultTag = getTagFromController(c, controllerPage);

                Method[] methods = c.getDeclaredMethods();
                for (Method m : methods) {
                    ControllerMethodInfo cmi = m.getAnnotation(ControllerMethodInfo.class);
                    if (cmi == null) continue;

                    String endpointId = cmi.id();
                    ApiInfo apiInfo = m.getAnnotation(ApiInfo.class);

                    // 태그 결정
                    String tag = defaultTag;
                    if (apiInfo != null && !apiInfo.tag().isEmpty()) {
                        tag = apiInfo.tag();
                    }
                    tagMap.put(tag, "");

                    // summary/description
                    String summary = endpointId;
                    String description = "";
                    String httpMethod = "get";

                    if (apiInfo != null) {
                        if (!apiInfo.summary().isEmpty()) summary = apiInfo.summary();
                        if (!apiInfo.description().isEmpty()) description = apiInfo.description();
                        httpMethod = apiInfo.method().toLowerCase();
                    } else {
                        // @ApiInfo 없으면 자동 추론
                        summary = inferSummary(m.getName(), endpointId);
                        description = buildAutoDescription(cmi, c);
                    }

                    // Path item 생성
                    JSONObject pathItem = paths.has(endpointId) ? paths.getJSONObject(endpointId) : new JSONObject();

                    // HTTP 메서드별 operation 생성
                    List<String> httpMethods = parseHttpMethods(httpMethod);
                    for (String hm : httpMethods) {
                        JSONObject operation = buildOperation(endpointId, tag, summary, description, cmi, apiInfo, hm);
                        pathItem.put(hm, operation);
                    }

                    paths.put(endpointId, pathItem);
                }
            }

            spec.put("paths", paths);

            // Tags
            JSONArray tagsArray = new JSONArray();
            for (String tagName : tagMap.keySet()) {
                JSONObject tagObj = new JSONObject();
                tagObj.put("name", tagName);
                tagsArray.put(tagObj);
            }
            spec.put("tags", tagsArray);

            // Components (공통 응답 스키마)
            spec.put("components", buildComponents());

            this.openApiJson = spec.toString(2);

        } catch (Exception e) {
            BaseDebug.log(e, "SwaggerServlet: OpenAPI spec generation failed");
            this.openApiJson = "{\"error\": \"Failed to generate spec\"}";
        }
    }

    /**
     * 개별 API operation 생성
     */
    private JSONObject buildOperation(String endpointId, String tag, String summary,
                                       String description, ControllerMethodInfo cmi,
                                       ApiInfo apiInfo, String httpMethod) throws JSONException {
        JSONObject operation = new JSONObject();
        operation.put("summary", summary);
        if (!description.isEmpty()) {
            operation.put("description", description);
        }
        operation.put("operationId", endpointId.replace("/", "_").replace(".", "_") + "_" + httpMethod);

        // Tags
        JSONArray tags = new JSONArray();
        tags.put(tag);
        operation.put("tags", tags);

        // Security 정보 표시
        if (cmi.loginRequired()) {
            String secNote = "[Login Required]";
            if (cmi.requiredSecurityLevel() > 0) {
                secNote += " [Security Level: " + cmi.requiredSecurityLevel() + "]";
            }
            String existingDesc = operation.optString("description", "");
            operation.put("description", secNote + (existingDesc.isEmpty() ? "" : "\n\n" + existingDesc));
        }

        // Parameters
        if (apiInfo != null && apiInfo.parameters().length > 0) {
            // @ApiInfo에 정의된 파라미터 사용
            if ("get".equals(httpMethod)) {
                operation.put("parameters", buildApiInfoParametersAsQuery(apiInfo));
            } else {
                operation.put("requestBody", buildApiInfoRequestBody(apiInfo));
            }
        } else {
            // 자동 추론: API 엔드포인트는 JSON 파라미터 사용
            if (isExternalApi(endpointId)) {
                if ("get".equals(httpMethod)) {
                    operation.put("parameters", buildDefaultGetParameters());
                } else {
                    operation.put("requestBody", buildDefaultPostRequestBody());
                }
            }
        }

        // Responses
        operation.put("responses", buildDefaultResponses());

        return operation;
    }

    /**
     * @ApiInfo의 파라미터를 GET 쿼리 파라미터로 변환
     */
    private JSONArray buildApiInfoParametersAsQuery(ApiInfo apiInfo) throws JSONException {
        // API 패턴: GET 방식은 param=JSON 형태
        JSONArray params = new JSONArray();
        JSONObject paramObj = new JSONObject();
        paramObj.put("name", "param");
        paramObj.put("in", "query");
        paramObj.put("required", true);
        paramObj.put("description", "JSON 형식 파라미터");

        // 스키마에 파라미터 설명 포함
        JSONObject schema = new JSONObject();
        schema.put("type", "string");

        // 예시 JSON 생성
        JSONObject example = new JSONObject();
        for (ApiInfo.Param p : apiInfo.parameters()) {
            String exampleVal = p.example().isEmpty() ? getTypeExample(p.type()) : p.example();
            example.put(p.name(), exampleVal);
        }
        schema.put("example", example.toString());

        paramObj.put("schema", schema);
        params.put(paramObj);

        return params;
    }

    /**
     * @ApiInfo의 파라미터를 POST body 스키마로 변환
     */
    private JSONObject buildApiInfoRequestBody(ApiInfo apiInfo) throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("required", true);

        JSONObject content = new JSONObject();
        JSONObject jsonType = new JSONObject();
        JSONObject schema = new JSONObject();
        schema.put("type", "object");

        JSONObject properties = new JSONObject();
        JSONArray required = new JSONArray();

        for (ApiInfo.Param p : apiInfo.parameters()) {
            JSONObject prop = new JSONObject();
            prop.put("type", mapType(p.type()));
            if (!p.description().isEmpty()) prop.put("description", p.description());
            if (!p.example().isEmpty()) prop.put("example", p.example());
            properties.put(p.name(), prop);
            if (p.required()) required.put(p.name());
        }

        schema.put("properties", properties);
        if (required.length() > 0) schema.put("required", required);

        jsonType.put("schema", schema);
        content.put("application/json", jsonType);
        requestBody.put("content", content);

        return requestBody;
    }

    /**
     * 기본 GET 파라미터 (param=JSON 패턴)
     */
    private JSONArray buildDefaultGetParameters() throws JSONException {
        JSONArray params = new JSONArray();
        JSONObject paramObj = new JSONObject();
        paramObj.put("name", "param");
        paramObj.put("in", "query");
        paramObj.put("required", false);
        paramObj.put("description", "JSON 형식 요청 파라미터");

        JSONObject schema = new JSONObject();
        schema.put("type", "string");
        schema.put("example", "{\"key\": \"value\"}");
        paramObj.put("schema", schema);
        params.put(paramObj);
        return params;
    }

    /**
     * 기본 POST request body
     */
    private JSONObject buildDefaultPostRequestBody() throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("required", false);
        requestBody.put("description", "JSON 형식 요청 본문");

        JSONObject content = new JSONObject();
        JSONObject jsonType = new JSONObject();
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        jsonType.put("schema", schema);
        content.put("application/json", jsonType);
        requestBody.put("content", content);

        return requestBody;
    }

    /**
     * 기본 응답 스키마
     */
    private JSONObject buildDefaultResponses() throws JSONException {
        JSONObject responses = new JSONObject();

        JSONObject ok = new JSONObject();
        ok.put("description", "성공");
        JSONObject content = new JSONObject();
        JSONObject jsonType = new JSONObject();
        JSONObject schema = new JSONObject();
        schema.put("$ref", "#/components/schemas/ApiResponse");
        jsonType.put("schema", schema);
        content.put("application/json", jsonType);
        ok.put("content", content);
        responses.put("200", ok);

        return responses;
    }

    /**
     * 공통 컴포넌트 스키마
     */
    private JSONObject buildComponents() throws JSONException {
        JSONObject components = new JSONObject();
        JSONObject schemas = new JSONObject();

        // ApiResponse 스키마
        JSONObject apiResponse = new JSONObject();
        apiResponse.put("type", "object");
        JSONObject props = new JSONObject();

        JSONObject resultProp = new JSONObject();
        resultProp.put("type", "string");
        resultProp.put("description", "결과 코드 (OK/NO/ERROR 또는 숫자)");
        props.put("result", resultProp);

        JSONObject msgProp = new JSONObject();
        msgProp.put("type", "string");
        msgProp.put("description", "결과 메시지");
        props.put("msg", msgProp);

        JSONObject resultMapProp = new JSONObject();
        resultMapProp.put("type", "object");
        resultMapProp.put("description", "결과 데이터 (단건)");
        props.put("resultMap", resultMapProp);

        JSONObject resultListProp = new JSONObject();
        resultListProp.put("type", "array");
        resultListProp.put("description", "결과 데이터 (목록)");
        JSONObject items = new JSONObject();
        items.put("type", "object");
        resultListProp.put("items", items);
        props.put("resultList", resultListProp);

        apiResponse.put("properties", props);
        schemas.put("ApiResponse", apiResponse);

        components.put("schemas", schemas);
        return components;
    }

    // ── 유틸리티 메서드 ──

    private String getTagFromController(Class<?> c, String controllerPage) {
        String name = c.getSimpleName().replace("Controller", "");
        if (controllerPage.contains("/api/")) return "External API";
        if (controllerPage.contains("/app/")) return "App API";
        if (controllerPage.contains("/admin/")) return "Admin";
        if (name.equals("Welcome")) return "System";
        if (name.equals("Health")) return "Health Check";
        if (name.equals("UserAPI")) return "User API";
        return name;
    }

    private boolean isExternalApi(String endpointId) {
        return endpointId.startsWith("/api/") ||
               endpointId.startsWith("/app/") ||
               endpointId.equals("/authLocation.do") ||
               endpointId.equals("/findFLocation.do") ||
               endpointId.equals("/getServerPosition.do") ||
               endpointId.equals("/getCellids.do") ||
               endpointId.equals("/getLTECellInfo.do");
    }

    private String inferSummary(String methodName, String endpointId) {
        // 메서드 이름에서 summary 추론
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1)
                .replaceAll("([A-Z])", " $1").trim();
    }

    private String buildAutoDescription(ControllerMethodInfo cmi, Class<?> controller) {
        StringBuilder sb = new StringBuilder();
        sb.append("Controller: `").append(controller.getSimpleName()).append("`");
        return sb.toString();
    }

    private List<String> parseHttpMethods(String method) {
        List<String> methods = new ArrayList<>();
        String lower = method.toLowerCase().replace(" ", "");
        if (lower.contains("get")) methods.add("get");
        if (lower.contains("post")) methods.add("post");
        if (methods.isEmpty()) methods.add("get");
        return methods;
    }

    private String mapType(String type) {
        switch (type.toLowerCase()) {
            case "int": case "integer": case "long": return "integer";
            case "float": case "double": case "number": return "number";
            case "bool": case "boolean": return "boolean";
            case "array": case "list": return "array";
            case "object": case "map": return "object";
            default: return "string";
        }
    }

    private String getTypeExample(String type) {
        switch (type.toLowerCase()) {
            case "int": case "integer": case "long": return "0";
            case "float": case "double": case "number": return "0.0";
            case "bool": case "boolean": return "true";
            default: return "";
        }
    }
}
