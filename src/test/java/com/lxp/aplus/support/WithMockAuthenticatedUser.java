package com.lxp.aplus.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 슬라이스 테스트에 인증된 사용자 정보를 주입하기 위한 테스트 전용 애너테이션입니다.
 *
 * JWT 필터와 토큰 검증은 별도 보안 테스트에서 다루고, Controller 테스트에서는 실제
 * {@code AuthenticatedArgumentResolver}가 SecurityContext의 사용자 ID를 command에 전달하는지만
 * 확인하기 위해 사용합니다. 프로젝트의 {@code SecurityUtils}가 Long 타입 principal을 사용자 ID로
 * 해석하므로 일반적인 {@code WithMockUser} 대신 이 애너테이션을 사용합니다. {@code WithMockUser}는
 * principal에 UserDetails를 넣기 때문에 현재 구현에서는 사용자 ID를 찾지 못해 인증에 실패합니다.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockAuthenticatedUserSecurityContextFactory.class)
public @interface WithMockAuthenticatedUser {

    /** Controller 테스트에서 공통으로 사용하는 기본 사용자 ID입니다. */
    long USER_ID = 1L;

    /** SecurityContext의 principal에 주입할 사용자 ID입니다. */
    long userId() default USER_ID;
}
