package com.ithows.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API Key 필수 어노테이션.
 *
 * 이 어노테이션이 붙은 컨트롤러 메서드는 HTTP 요청의 "X-API-Key" 헤더 값을
 * UserKeyDAO.checkAPIKey() 로 검증한 뒤에만 실행된다.
 * 로그인 여부(loginRequired) 및 보안 레벨과 독립적으로 동작한다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiKeyRequired {
}
