package com.lxp.aplus.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

/**
 * {@link WithMockAuthenticatedUser}에 지정된 사용자 ID로 테스트용 SecurityContext를 생성합니다.
 *
 * <p>실제 JWT를 만들거나 {@code JwtAuthenticationFilter}를 실행하지 않고도 production의
 * {@code AuthenticatedArgumentResolver}를 그대로 사용할 수 있도록 principal을 Long 타입으로
 * 설정합니다. 이를 통해 Controller 테스트의 범위를 요청 검증, command 변환, 응답 형식에
 * 한정하면서도 인증 사용자 ID 해석 과정은 실제 구현을 따릅니다.</p>
 */
public class WithMockAuthenticatedUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockAuthenticatedUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockAuthenticatedUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                annotation.userId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
        return context;
    }
}
