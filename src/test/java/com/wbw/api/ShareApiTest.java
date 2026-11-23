package com.wbw.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShareApiTest extends ApiTestSupport {

    private long createDoc() throws Exception {
        return read(mvc.perform(postAs(ALICE, "/api/documents", Map.of("title", "공유할 글", "content", doc("공유 본문"))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private JsonNode createShare(long docId, Map<String, Object> body) throws Exception {
        return read(mvc.perform(postAs(ALICE, "/api/documents/{id}/shares", body, docId))
                .andExpect(status().isCreated()).andReturn());
    }

    private org.springframework.test.web.servlet.ResultActions verify(String token, String password, MockHttpSession session)
            throws Exception {
        return mvc.perform(post("/api/public/shares/{t}/verify", token).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("password", password))));
    }

    @Test
    void publicShareIsReadableWithoutLogin() throws Exception {
        long docId = createDoc();
        JsonNode share = createShare(docId, Map.of());
        String token = share.get("token").asText();

        mvc.perform(getAs(ALICE, "/api/documents/{id}/shares", docId))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].passwordProtected").value(false))
                .andExpect(jsonPath("$[0].url").value(startsWith("http://localhost:5173/s/")));

        mvc.perform(get("/api/public/shares/{t}", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.title").value("공유할 글"))
                .andExpect(jsonPath("$.content.type").value("doc"))
                .andExpect(jsonPath("$.charsWithSpaces").value(5));

        // 공유 응답에 작성자 정보는 포함되지 않는다
        mvc.perform(get("/api/public/shares/{t}", token)).andExpect(jsonPath("$.userId").doesNotExist());
        mvc.perform(get("/api/public/shares/{t}", "no-such-token")).andExpect(status().isNotFound());
    }

    @Test
    void passwordShareRequiresVerification() throws Exception {
        long docId = createDoc();
        String token = createShare(docId, Map.of("password", "secret123")).get("token").asText();
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/api/public/shares/{t}", token).session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SHARE_PASSWORD_REQUIRED"))
                .andExpect(jsonPath("$.title").doesNotExist());

        verify(token, "wrong", session)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SHARE_PASSWORD_MISMATCH"));

        verify(token, "secret123", session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("공유할 글"));

        // 같은 세션에서는 다시 비밀번호를 묻지 않는다
        mvc.perform(get("/api/public/shares/{t}", token).session(session)).andExpect(status().isOk());
        // 다른 세션은 여전히 비밀번호 필요
        mvc.perform(get("/api/public/shares/{t}", token).session(new MockHttpSession())).andExpect(status().isUnauthorized());
    }

    @Test
    void changingPasswordInvalidatesPreviousVerification() throws Exception {
        long docId = createDoc();
        JsonNode share = createShare(docId, Map.of("password", "first-pass"));
        String token = share.get("token").asText();
        MockHttpSession session = new MockHttpSession();
        verify(token, "first-pass", session).andExpect(status().isOk());

        mvc.perform(patchAs(ALICE, "/api/shares/{id}", Map.of("password", "second-pass"), share.get("id").asLong()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/public/shares/{t}", token).session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void tooManyWrongPasswordsLocksOut() throws Exception {
        long docId = createDoc();
        String token = createShare(docId, Map.of("password", "right-pass")).get("token").asText();
        MockHttpSession session = new MockHttpSession();
        for (int i = 0; i < 4; i++) {
            verify(token, "wrong", session).andExpect(status().isUnauthorized());
        }
        verify(token, "wrong", session).andExpect(status().isTooManyRequests());
        // 잠긴 동안에는 맞는 비밀번호도 거부
        verify(token, "right-pass", session)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("SHARE_TOO_MANY_ATTEMPTS"));
    }

    @Test
    void disableRemovePasswordAndDelete() throws Exception {
        long docId = createDoc();
        JsonNode share = createShare(docId, Map.of("password", "pass1234"));
        long id = share.get("id").asLong();
        String token = share.get("token").asText();

        mvc.perform(patchAs(ALICE, "/api/shares/{id}", Map.of("removePassword", true), id))
                .andExpect(jsonPath("$.passwordProtected").value(false));
        mvc.perform(get("/api/public/shares/{t}", token)).andExpect(status().isOk());

        mvc.perform(patchAs(ALICE, "/api/shares/{id}", Map.of("enabled", false), id))
                .andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(get("/api/public/shares/{t}", token)).andExpect(status().isNotFound());

        mvc.perform(deleteAs(ALICE, "/api/shares/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(getAs(ALICE, "/api/documents/{id}/shares", docId)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void expiryValidation() throws Exception {
        long docId = createDoc();
        Map<String, Object> past = new HashMap<>();
        past.put("expiresAt", Instant.now().minus(1, ChronoUnit.HOURS).toString());
        mvc.perform(postAs(ALICE, "/api/documents/{id}/shares", past, docId)).andExpect(status().isBadRequest());

        Map<String, Object> future = new HashMap<>();
        future.put("expiresAt", Instant.now().plus(1, ChronoUnit.DAYS).toString());
        JsonNode share = createShare(docId, future);
        mvc.perform(get("/api/public/shares/{t}", share.get("token").asText())).andExpect(status().isOk());
    }

    @Test
    void passwordLengthValidated() throws Exception {
        long docId = createDoc();
        mvc.perform(postAs(ALICE, "/api/documents/{id}/shares", Map.of("password", "123"), docId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otherUsersCannotManageShares() throws Exception {
        long docId = createDoc();
        long id = createShare(docId, Map.of()).get("id").asLong();
        mvc.perform(getAs(BOB, "/api/documents/{id}/shares", docId)).andExpect(status().isNotFound());
        mvc.perform(postAs(BOB, "/api/documents/{id}/shares", Map.of(), docId)).andExpect(status().isNotFound());
        mvc.perform(patchAs(BOB, "/api/shares/{id}", Map.of("enabled", false), id)).andExpect(status().isNotFound());
        mvc.perform(deleteAs(BOB, "/api/shares/{id}", id)).andExpect(status().isNotFound());
    }
}
