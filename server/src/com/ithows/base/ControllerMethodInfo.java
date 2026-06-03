/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author dreamct
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ControllerMethodInfo {
    public String id();
    public String controllerClass() default "";
    public String controllerPage() default "";
    public String template() default "";
    public String commandClass() default "";
    public String commandName() default "";
    public int version() default 0;

    /**
     * 로그인 필요 여부. true이면 미로그인 시 로그인 페이지로 리다이렉트.
     * 기본값 false (API 엔드포인트 등 비로그인 접근 허용).
     */
    public boolean loginRequired() default false;

    /**
     * 접근 허용 최소 보안 레벨.
     *   0 = 모든 사용자 (기본값)
     *   1 = General 이상
     *   2 = Super 이상
     *   3 = Admin 전용
     */
    public int requiredSecurityLevel() default 0;
}


/*
public @interface MethodControllerMetaInfo {
    public String template();
    public String commandClass();
    public String commandName();
}*/
