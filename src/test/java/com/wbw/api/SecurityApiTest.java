package com.wbw.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityApiTest extends ApiTestSupport {

    @Test
    void apiRequiresLoginAndReturnsJson401() throws Exception {
        mvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        mvc.perform(getAs(ALICE, "/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void loggedInButUnknownUserIsUnauthorized() throws Exception {
        mvc.perform(getAs("not-registered-sub", "/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequestWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/api/folders").with(as(ALICE))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "x"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void swaggerDocsArePublicAndHideLoginUserParameter() throws Exception {
        String docs = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("wbw 작문 플랫폼 API"))
                .andExpect(jsonPath("$.paths['/api/documents/{documentId}'].put").exists())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(docs).doesNotContain("\"userId\"");
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void swaggerLoginStartsGoogleLogin() throws Exception {
        mvc.perform(get("/swagger-login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/oauth2/authorization/google"));
    }

    @Test
    void fontsArePublic() throws Exception {
        mvc.perform(get("/api/fonts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].family").value("Pretendard"))
                .andExpect(jsonPath("$[0].generic").value("sans-serif"));
    }

    @Test
    void corsPreflightAllowsConfiguredFrontend() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/documents")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Credentials", "true"));
    }
}
