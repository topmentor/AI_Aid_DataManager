package com.ithows.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 문서화를 위한 어노테이션.
 * ControllerMethodInfo와 함께 사용하여 Swagger 문서를 자동 생성합니다.
 *
 * 사용 예:
 * @ApiInfo(
 *     summary = "위치 인증",
 *     description = "복합신호 기반 위치 인증 API",
 *     tag = "인증 API",
 *     method = "POST",
 *     parameters = {
 *         @ApiInfo.Param(name = "req_posmethod", type = "string", description = "측위 방식 (AGNSS/WiFi/CellID/Fused)"),
 *         @ApiInfo.Param(name = "latitude", type = "number", description = "위도")
 *     }
 * )
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiInfo {

    /** API 요약 (한 줄) */
    String summary() default "";

    /** API 상세 설명 */
    String description() default "";

    /** API 그룹 태그 */
    String tag() default "";

    /** HTTP 메서드 (GET, POST, GET/POST) */
    String method() default "GET/POST";

    /** 파라미터 목록 */
    Param[] parameters() default {};

    /** 응답 설명 */
    String responseDescription() default "";

    /**
     * 파라미터 정의용 내부 어노테이션
     */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Param {
        String name();
        String type() default "string";
        String description() default "";
        boolean required() default false;
        String example() default "";
    }
}
