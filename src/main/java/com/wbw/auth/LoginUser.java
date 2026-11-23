package com.wbw.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 현재 로그인한 사용자의 ID(Long)를 주입한다.
 * 로그인하지 않았거나 사용자 정보를 찾을 수 없으면 401을 반환한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {
}
