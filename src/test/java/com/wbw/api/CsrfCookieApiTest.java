package com.wbw.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 운영과 같은 쿠키 기반 CSRF 동작을 검증한다.
 * 다른 테스트의 with(csrf())가 CSRF 저장소를 테스트용(세션 기반)으로 바꿔 끼우므로,
 * 이 클래스는 새 애플리케이션 컨텍스트에서 실행한다. (이 클래스 안에서는 csrf()를 쓰지 않는다)
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfCookieApiTest extends ApiTestSupport {

    @Test
    void csrfEndpointIssuesCookie() throws Exception {
        mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
    }

    @Test
    void cookieTokenInHeaderPassesCsrf() throws Exception {
        String token = mvc.perform(get("/api/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN").getValue();
        mvc.perform(post("/api/folders").with(as(ALICE))
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "쿠키 토큰"))))
                .andExpect(status().isCreated());
    }

    @Test
    void bodyTokenInHeaderPassesCsrf() throws Exception {
        // 프론트가 다른 도메인(localhost:5173)에서 쿠키를 읽지 못할 때 쓰는 방식
        MockHttpServletResponse res = mvc.perform(get("/api/csrf")).andReturn().getResponse();
        String cookieToken = res.getCookie("XSRF-TOKEN").getValue();
        String bodyToken = mapper.readTree(res.getContentAsString()).get("token").asText();
        mvc.perform(post("/api/folders").with(as(ALICE))
                        .cookie(new Cookie("XSRF-TOKEN", cookieToken))
                        .header("X-XSRF-TOKEN", bodyToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "본문 토큰"))))
                .andExpect(status().isCreated());
    }

    @Test
    void wrongTokenIsRejected() throws Exception {
        String token = mvc.perform(get("/api/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN").getValue();
        mvc.perform(post("/api/folders").with(as(ALICE))
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "x"))))
                .andExpect(status().isForbidden());
    }
}
