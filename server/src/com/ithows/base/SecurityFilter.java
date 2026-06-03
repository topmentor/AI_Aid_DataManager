package com.ithows.base;

import com.ithows.BaseDebug;
import com.ithows.HttpUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSF SecurityFilter
 *
 * 보안 기능을 일괄 처리하는 서블릿 필터.
 * - HTTP 보안 헤더 설정
 * - XSS 입력값 필터링
 * - CSRF 토큰 검증
 * - Rate Limiting (IP 기반)
 *
 * @author SSF Security Module
 */
public class SecurityFilter implements Filter {

    // CSRF 토큰 이름
    public static final String CSRF_TOKEN_NAME = "_csrf";
    public static final String CSRF_TOKEN_SESSION_ATTR = "_csrf_token";

    // CSRF 활성화 여부 (web.xml init-param "csrfEnabled"로 제어, 기본값 false)
    private boolean csrfEnabled = false;

    // Rate Limiting 설정
    private int rateLimitMaxRequests = 100;        // 윈도우당 최대 요청 수
    private long rateLimitWindowMs = 60 * 1000;    // 윈도우 크기 (1분)
    private final ConcurrentHashMap<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    // CSRF 검증 제외 경로 (외부 API + 로그인/로그아웃)
    private static final Set<String> CSRF_EXCLUDE_PATHS = new HashSet<>(Arrays.asList(
            "/login.do",
            "/logout.do",
            "/authLocation.do",
            "/findFLocation.do",
            "/getServerPosition.do",
            "/getPosition.do",
            "/getCellids.do",
            "/getLTECellInfo.do",
            "/health.do",
            "/api/requestNewKey.do",
            "/api/checkKeyValidation.do",
            "/api/checkHealth.do",
            "/api/checkDB.do",
            "/api/checkAllInfo.do",
            "/app/registDevice.do",
            "/app/getDeviceList.do"
    ));

    // Rate Limiting 적용 경로 (외부 API)
    private static final Set<String> RATE_LIMIT_PATHS = new HashSet<>(Arrays.asList(
            "/authLocation.do",
            "/findFLocation.do",
            "/getServerPosition.do",
            "/getPosition.do",
            "/getCellids.do",
            "/getLTECellInfo.do"
    ));

    // 보안 헤더 제외 확장자 (정적 리소스)
    private static final Set<String> STATIC_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".js", ".css", ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg", ".woff", ".woff2", ".ttf", ".eot"
    ));

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;

        // init-param에서 Rate Limit 설정 읽기
        String maxReq = filterConfig.getInitParameter("rateLimitMaxRequests");
        if (maxReq != null && !maxReq.isEmpty()) {
            try {
                this.rateLimitMaxRequests = Integer.parseInt(maxReq);
            } catch (NumberFormatException e) {
                // 기본값 사용
            }
        }

        String windowMs = filterConfig.getInitParameter("rateLimitWindowMs");
        if (windowMs != null && !windowMs.isEmpty()) {
            try {
                this.rateLimitWindowMs = Long.parseLong(windowMs);
            } catch (NumberFormatException e) {
                // 기본값 사용
            }
        }

        String csrf = filterConfig.getInitParameter("csrfEnabled");
        if (csrf != null && "true".equalsIgnoreCase(csrf)) {
            this.csrfEnabled = true;
        }

        BaseDebug.info("***SecurityFilter Initialized (rateLimit=" + rateLimitMaxRequests + " req/" + (rateLimitWindowMs / 1000) + "s, csrf=" + csrfEnabled + ")");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestURI = request.getRequestURI();
        String path = requestURI.substring(request.getContextPath().length());

        // 1. 정적 리소스는 보안 헤더만 적용 후 통과
        if (isStaticResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. HTTP 보안 헤더 설정
        setSecurityHeaders(response);

        // 3. XSS 입력값 필터링 (래핑된 요청 사용)
        HttpServletRequest wrappedRequest = new XssRequestWrapper(request);

        // 4. Rate Limiting (API 엔드포인트)
        if (RATE_LIMIT_PATHS.contains(path)) {
            String clientIp = HttpUtil.getClientIp(request);
            if (!checkRateLimit(clientIp)) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write("{\"result\":429,\"msg\":\"Too many requests. Please try again later.\"}");
                return;
            }
        }

        // 5. CSRF 토큰 검증 — 현재 비활성화 (추후 옵션으로 활성화)
        // CSRF를 활성화하려면 csrfEnabled = true로 설정하고,
        // 모든 POST 폼에 <input type="hidden" name="_csrf" value="${_csrf}" /> 추가 필요
        if (csrfEnabled && "POST".equalsIgnoreCase(request.getMethod()) && !CSRF_EXCLUDE_PATHS.contains(path)) {
            if (!validateCsrfToken(wrappedRequest)) {
                response.setStatus(403);
                response.setContentType("text/html; charset=UTF-8");
                response.getWriter().write("<html><body><h3>403 Forbidden</h3><p>CSRF token validation failed.</p></body></html>");
                BaseDebug.info("[SecurityFilter] CSRF token validation failed: " + path + " from " + HttpUtil.getClientIp(request));
                return;
            }
        }

        // 6. CSRF 토큰 생성 (세션이 있고 토큰이 없는 경우)
        if (csrfEnabled) {
            ensureCsrfToken(request);
        }

        chain.doFilter(wrappedRequest, response);
    }

    @Override
    public void destroy() {
        rateLimitMap.clear();
    }

    // ==================== HTTP 보안 헤더 ====================

    private void setSecurityHeaders(HttpServletResponse response) {
        // 클릭재킹 방어
        response.setHeader("X-Frame-Options", "SAMEORIGIN");

        // HTTPS 강제 (HSTS)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // MIME 타입 스니핑 방지
        response.setHeader("X-Content-Type-Options", "nosniff");

        // XSS 필터 활성화
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Content-Security-Policy
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://unpkg.com https://*.daumcdn.net; " +
                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net data:; " +
                "img-src 'self' data: https://*.tile.openstreetmap.org https://*.google.com https://*.googleapis.com https://*.daumcdn.net https://*.kakao.com https://*.vworld.kr; " +
                "connect-src 'self' https://*.google.com https://*.googleapis.com https://*.daumcdn.net https://*.vworld.kr https://nominatim.openstreetmap.org");

        // Referrer 정책
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 브라우저 기능 제한
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(self)");

        // 캐시 제어 (동적 페이지)
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }

    // ==================== CSRF 토큰 ====================

    /**
     * 세션에 CSRF 토큰이 없으면 생성
     */
    private void ensureCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(CSRF_TOKEN_SESSION_ATTR) == null) {
            String token = generateCsrfToken();
            session.setAttribute(CSRF_TOKEN_SESSION_ATTR, token);
        }
    }

    /**
     * CSRF 토큰 검증
     * - 요청 파라미터 또는 헤더에서 토큰을 읽어 세션 토큰과 비교
     */
    private boolean validateCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            // 세션이 없으면 로그인 전이므로 CSRF 검증 건너뜀 (로그인 폼 등)
            return true;
        }

        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTR);
        if (sessionToken == null) {
            // 토큰이 아직 발급되지 않은 세션 (최초 POST 등)
            return true;
        }

        // 파라미터에서 토큰 확인
        String requestToken = request.getParameter(CSRF_TOKEN_NAME);

        // 헤더에서도 확인 (AJAX 요청 지원)
        if (requestToken == null || requestToken.isEmpty()) {
            requestToken = request.getHeader("X-CSRF-TOKEN");
        }

        if (requestToken == null || requestToken.isEmpty()) {
            return false;
        }

        return sessionToken.equals(requestToken);
    }

    /**
     * 보안 랜덤 CSRF 토큰 생성
     */
    private String generateCsrfToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 현재 세션의 CSRF 토큰을 반환 (JSP에서 사용)
     */
    public static String getCsrfToken(HttpSession session) {
        if (session == null) {
            return "";
        }
        String token = (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTR);
        return token != null ? token : "";
    }

    // ==================== Rate Limiting ====================

    /**
     * IP 기반 Rate Limiting 체크
     * @return true = 허용, false = 제한 초과
     */
    private boolean checkRateLimit(String clientIp) {
        long now = System.currentTimeMillis();

        RateLimitEntry entry = rateLimitMap.compute(clientIp, (key, existing) -> {
            if (existing == null || (now - existing.windowStart) > rateLimitWindowMs) {
                // 새 윈도우 시작
                return new RateLimitEntry(now, 1);
            } else {
                existing.count++;
                return existing;
            }
        });

        // 주기적으로 만료된 엔트리 정리 (1000개 초과 시)
        if (rateLimitMap.size() > 1000) {
            cleanupRateLimitMap(now);
        }

        return entry.count <= rateLimitMaxRequests;
    }

    private void cleanupRateLimitMap(long now) {
        rateLimitMap.entrySet().removeIf(e -> (now - e.getValue().windowStart) > rateLimitWindowMs);
    }

    // ==================== 유틸리티 ====================

    private boolean isStaticResource(String path) {
        if (path == null) return false;
        String lowerPath = path.toLowerCase();
        for (String ext : STATIC_EXTENSIONS) {
            if (lowerPath.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    // ==================== Inner Classes ====================

    private static class RateLimitEntry {
        long windowStart;
        int count;

        RateLimitEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
