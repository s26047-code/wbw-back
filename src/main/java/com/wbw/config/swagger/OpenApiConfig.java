package com.wbw.config.swagger;

import com.wbw.auth.LoginUser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static {
        // @LoginUser 파라미터는 세션에서 주입되므로 문서에 노출하지 않는다.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(LoginUser.class);
    }

    @Bean
    public OpenAPI wbwOpenApi() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/").description("현재 접속한 서버")))
                .info(new Info()
                .title("wbw 작문 플랫폼 API")
                .version("v1")
                .description("""
                        ### 사용 방법
                        1. **로그인**: 이 페이지와 같은 브라우저에서 [/swagger-login](/swagger-login) 을 열어 구글 로그인 → 이 페이지로 돌아옵니다.
                        2. 로그인 확인: `GET /api/me` 실행 → 200이면 성공
                        3. 이후 모든 API를 **Try it out** 으로 바로 호출할 수 있습니다. (CSRF 헤더는 자동으로 붙습니다)

                        ### 알아둘 것
                        - 인증: 세션 쿠키(`WBW_SESSION`). 프론트에서는 `credentials: 'include'` 필수
                        - 변경 요청(POST/PUT/PATCH/DELETE)은 `X-XSRF-TOKEN` 헤더 필요 (`XSRF-TOKEN` 쿠키 값)
                        - 에러 응답: `{ code, message, fieldErrors?, details? }` — `message`는 사용자에게 그대로 보여줘도 되는 문구
                        - 글 본문(`content`)은 TipTap JSON. 서식 속성 이름·글자 수 규칙·자동 저장 흐름은 저장소의 `docs/API.md` 참고
                        """));
    }
}
