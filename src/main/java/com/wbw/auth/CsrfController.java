package com.wbw.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 프론트엔드가 앱 시작 시 호출하여 XSRF-TOKEN 쿠키를 발급받는다. */
@RestController
@Tag(name = "인증", description = "CSRF 토큰")
public class CsrfController {

    @Operation(summary = "CSRF 토큰 발급", description = "앱 시작 시 1회 호출하면 XSRF-TOKEN 쿠키가 발급됩니다. 변경 요청에는 X-XSRF-TOKEN 헤더에 쿠키 값을 넣으세요.")
    @GetMapping("/api/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
}
