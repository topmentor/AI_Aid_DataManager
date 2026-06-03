package com.ithows.aidclaude;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 로컬 데스크톱(Electron) 렌더러가 cross-origin으로 /api/*를 호출하므로 CORS 허용.
 * 로컬 전용이므로 모든 오리진 허용. OPTIONS preflight는 즉시 200으로 응답한다.
 */
public class CorsFilter implements Filter {

    @Override public void init(FilterConfig cfg) {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "*");
        response.setHeader("Vary", "Origin");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, X-CSRF-TOKEN, X-API-KEY");
        response.setHeader("Access-Control-Max-Age", "86400");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return; // preflight — 체인 중단
        }
        chain.doFilter(req, res);
    }

    @Override public void destroy() {}
}
