package com.wbw.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import java.util.function.Supplier;

/**
 * SPA용 CSRF 처리기.
 * <p>
 * X-XSRF-TOKEN 헤더에 다음 두 값 중 무엇을 보내도 검증된다.
 * <ul>
 *   <li>XSRF-TOKEN 쿠키 값 (프론트와 API가 같은 사이트일 때 권장)</li>
 *   <li>GET /api/csrf 응답 본문의 token 값 (쿠키를 읽을 수 없는 구성일 때)</li>
 * </ul>
 * 매 요청마다 토큰을 로드하여 쿠키가 항상 발급되도록 한다.
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        this.xor.handle(request, response, csrfToken);
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // 마스킹된 토큰(응답 본문)이면 복호화된 값을, 아니면 원본 값(쿠키)을 그대로 사용한다.
        String unmasked = this.xor.resolveCsrfTokenValue(request, csrfToken);
        return unmasked != null ? unmasked : this.plain.resolveCsrfTokenValue(request, csrfToken);
    }
}
