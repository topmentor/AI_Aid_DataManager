package com.ithows.base;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 공격 방어를 위한 HttpServletRequest 래퍼.
 *
 * 요청 파라미터와 헤더에서 위험한 HTML/스크립트 문자를 이스케이프 처리합니다.
 * - getParameter(), getParameterValues(), getParameterMap() 오버라이드
 * - getHeader() 오버라이드
 *
 * @author SSF Security Module
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    public XssRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return sanitize(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] sanitized = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            sanitized[i] = sanitize(values[i]);
        }
        return sanitized;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> original = super.getParameterMap();
        Map<String, String[]> sanitized = new HashMap<>();
        for (Map.Entry<String, String[]> entry : original.entrySet()) {
            String[] values = entry.getValue();
            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = sanitize(values[i]);
            }
            sanitized.put(entry.getKey(), cleanValues);
        }
        return sanitized;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return sanitize(value);
    }

    /**
     * HTML 특수문자를 이스케이프 처리하여 XSS 공격을 방어합니다.
     *
     * 변환 대상:
     * - < → &lt;
     * - > → &gt;
     * - " → &quot;
     * - ' → &#39;
     * - & → &amp;  (이미 이스케이프된 것은 제외)
     * - script 태그 제거
     * - on* 이벤트 핸들러 제거 (onclick, onerror 등)
     * - javascript: 프로토콜 제거
     */
    private String sanitize(String value) {
        if (value == null) {
            return null;
        }

        // script 태그 제거 (대소문자 무시)
        value = value.replaceAll("(?i)<\\s*script[^>]*>", "");
        value = value.replaceAll("(?i)</\\s*script\\s*>", "");

        // on* 이벤트 핸들러 제거 (onclick, onerror, onload 등)
        value = value.replaceAll("(?i)\\s+on\\w+\\s*=\\s*([\"'][^\"']*[\"']|\\S+)", "");

        // javascript: 프로토콜 제거
        value = value.replaceAll("(?i)javascript\\s*:", "");

        // HTML 특수문자 이스케이프
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                case '&':
                    // 이미 이스케이프된 엔티티(&lt; &gt; &amp; 등)는 건드리지 않음
                    if (isHtmlEntity(value, i)) {
                        sb.append('&');
                    } else {
                        sb.append("&amp;");
                    }
                    break;
                default:
                    sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 현재 위치의 &가 이미 HTML 엔티티의 일부인지 확인
     */
    private boolean isHtmlEntity(String value, int ampPos) {
        // &amp; &lt; &gt; &quot; &#39; &#숫자; 패턴 확인
        int semiPos = value.indexOf(';', ampPos);
        if (semiPos < 0 || semiPos - ampPos > 8) {
            return false;
        }
        String entity = value.substring(ampPos, semiPos + 1);
        return entity.matches("&(amp|lt|gt|quot|#\\d{1,5}|#x[0-9a-fA-F]{1,4});");
    }
}
