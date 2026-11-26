package com.wbw.config.swagger;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Swagger에서 구글 로그인을 시작하기 위한 주소.
 * 로그인이 끝나면 프론트엔드 대신 Swagger UI로 돌아온다. (이동 대상은 고정 경로라 오픈 리다이렉트 위험 없음)
 */
@Hidden
@Controller
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerLoginController {

    public static final String RETURN_TO_SWAGGER = "WBW_RETURN_TO_SWAGGER";

    @GetMapping("/swagger-login")
    public String login(HttpServletRequest request) {
        request.getSession(true).setAttribute(RETURN_TO_SWAGGER, Boolean.TRUE);
        return "redirect:/oauth2/authorization/google";
    }
}
