package com.wbw.auth;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 프론트엔드가 앱 시작 시 호출하여 XSRF-TOKEN 쿠키를 발급받는다. */
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
}
