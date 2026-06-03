package com.ithows.aidclaude;

/**
 * 소스명 → SQL 테이블명 변환. 기존 sqlite-loader.ts {@code toTableName}과 동일 규칙.
 */
public final class Names {

    private Names() {}

    /** 영숫자·한글 외 문자는 {@code _}로, 양끝 {@code _}는 제거. 빈 결과는 "source". */
    public static String toTableName(String sourceName) {
        if (sourceName == null) return "source";
        // 한글(가-힣, ㄱ-ㅎ, ㅏ-ㅣ)과 영숫자 외 → _
        String s = sourceName.replaceAll("[^a-zA-Z0-9\\uac00-\\ud7a3\\u3131-\\u314e\\u314f-\\u3163]", "_");
        s = s.replaceAll("^_+|_+$", "");
        return s.isEmpty() ? "source" : s;
    }
}
