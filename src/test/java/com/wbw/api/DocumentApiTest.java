package com.wbw.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentApiTest extends ApiTestSupport {

    private JsonNode createDoc(String sub, String title, String text, Long folderId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("content", doc(text));
        body.put("folderId", folderId);
        return read(mvc.perform(postAs(sub, "/api/documents", body)).andExpect(status().isCreated()).andReturn());
    }

    private long createFolder(String sub, String name) throws Exception {
        return read(mvc.perform(postAs(sub, "/api/folders", Map.of("name", name)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    @Test
    void createWithoutBodyMakesEmptyDocument() throws Exception {
        mvc.perform(postAs(ALICE, "/api/documents", Map.of()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(""))
                .andExpect(jsonPath("$.content.type").value("doc"))
                .andExpect(jsonPath("$.charsWithSpaces").value(0))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void createAndGetKeepsFormattingAndCountsCharacters() throws Exception {
        JsonNode content = mapper.readTree("""
                {"type":"doc","content":[{"type":"paragraph","attrs":{"lineHeight":2,"paragraphSpacing":1},
                 "content":[{"type":"text","text":"안녕 ","marks":[{"type":"bold"}]},
                            {"type":"text","text":"세상","marks":[{"type":"textStyle","attrs":{"fontFamily":"Nanum Myeongjo","fontSize":"20px","letterSpacing":"0.05em"}}]}]},
                 {"type":"paragraph","content":[{"type":"text","text":"둘째 줄"}]}]}
                """);
        long id = read(mvc.perform(postAs(ALICE, "/api/documents", Map.of("title", "  첫 글  ", "content", content)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("첫 글"))
                .andExpect(jsonPath("$.charsWithSpaces").value(9))     // "안녕 세상" 5 + "둘째 줄" 4
                .andExpect(jsonPath("$.charsWithoutSpaces").value(7))
                .andReturn()).get("id").asLong();

        JsonNode fetched = read(mvc.perform(getAs(ALICE, "/api/documents/{id}", id)).andExpect(status().isOk()).andReturn());
        assertThat(fetched.get("content")).isEqualTo(content);
        assertThat(fetched.get("createdAt").asText()).isNotBlank();
    }

    @Test
    void autosaveIncrementsVersionAndRejectsStaleVersion() throws Exception {
        JsonNode created = createDoc(ALICE, "제목", "처음", null);
        long id = created.get("id").asLong();

        JsonNode saved = read(mvc.perform(putAs(ALICE, "/api/documents/{id}",
                        Map.of("content", doc("수정한 본문"), "version", 0), id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.charsWithSpaces").value(6))
                .andExpect(jsonPath("$.charsWithoutSpaces").value(5))
                .andReturn());
        assertThat(saved.has("content")).isFalse(); // 자동 저장 응답은 본문을 다시 보내지 않는다

        // 다른 탭에서 예전 버전(0)으로 저장 시도 → 덮어쓰지 않고 409
        mvc.perform(putAs(ALICE, "/api/documents/{id}", Map.of("content", doc("다른 탭"), "version", 0), id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.details.currentVersion").value(1));

        mvc.perform(getAs(ALICE, "/api/documents/{id}", id))
                .andExpect(jsonPath("$.content.content[0].content[0].text").value("수정한 본문"));
    }

    @Test
    void savingSameContentDoesNotBumpVersion() throws Exception {
        long id = createDoc(ALICE, "제목", "같음", null).get("id").asLong();
        mvc.perform(putAs(ALICE, "/api/documents/{id}", Map.of("content", doc("같음"), "title", "제목", "version", 0), id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void titleOnlyUpdate() throws Exception {
        long id = createDoc(ALICE, "옛 제목", "본문", null).get("id").asLong();
        mvc.perform(putAs(ALICE, "/api/documents/{id}", Map.of("title", "새 제목", "version", 0), id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("새 제목"))
                .andExpect(jsonPath("$.charsWithSpaces").value(2));
    }

    @Test
    void updateValidation() throws Exception {
        long id = createDoc(ALICE, "t", "b", null).get("id").asLong();
        mvc.perform(putAs(ALICE, "/api/documents/{id}", Map.of("title", "x"), id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("version"));
        mvc.perform(putAs(ALICE, "/api/documents/{id}", Map.of("version", 0), id))
                .andExpect(status().isBadRequest());
        mvc.perform(putAs(ALICE, "/api/documents/{id}", Map.of("content", Map.of("type", "paragraph"), "version", 0), id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOCUMENT_CONTENT_INVALID"));
        mvc.perform(putAs(ALICE, "/api/documents/{id}", Map.of("title", "가".repeat(201), "version", 0), id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otherUsersCannotSeeOrModify() throws Exception {
        long id = createDoc(ALICE, "비밀", "앨리스의 글", null).get("id").asLong();
        mvc.perform(getAs(BOB, "/api/documents/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(putAs(BOB, "/api/documents/{id}", Map.of("title", "해킹", "version", 0), id)).andExpect(status().isNotFound());
        mvc.perform(deleteAs(BOB, "/api/documents/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(patchAs(BOB, "/api/documents/{id}/folder", new HashMap<>(), id)).andExpect(status().isNotFound());
        mvc.perform(getAs(BOB, "/api/documents")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(getAs(ALICE, "/api/documents/{id}", id)).andExpect(jsonPath("$.title").value("비밀"));
    }

    @Test
    void cannotCreateDocumentInOthersFolder() throws Exception {
        long bobFolder = createFolder(BOB, "밥 폴더");
        Map<String, Object> body = new HashMap<>();
        body.put("folderId", bobFolder);
        mvc.perform(postAs(ALICE, "/api/documents", body)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FOLDER_NOT_FOUND"));
    }

    @Test
    void listFiltersSortsSearchesAndPages() throws Exception {
        long folder = createFolder(ALICE, "소설");
        createDoc(ALICE, "가을 이야기", "a", folder);
        createDoc(ALICE, "겨울 일기", "b", null);
        createDoc(ALICE, "봄 100% 메모", "c", folder);
        createDoc(BOB, "남의 글", "d", null);

        mvc.perform(getAs(ALICE, "/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items[0].title").value("봄 100% 메모")) // 최근 수정순
                .andExpect(jsonPath("$.items[0].preview").value("c"))
                .andExpect(jsonPath("$.items[0].folderName").value("소설"));

        mvc.perform(getAs(ALICE, "/api/documents").param("folderId", String.valueOf(folder)))
                .andExpect(jsonPath("$.items", hasSize(2)));
        mvc.perform(getAs(ALICE, "/api/documents").param("unfiled", "true"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("겨울 일기"));
        mvc.perform(getAs(ALICE, "/api/documents").param("sort", "title"))
                .andExpect(jsonPath("$.items[0].title").value("가을 이야기"));
        mvc.perform(getAs(ALICE, "/api/documents").param("q", "100%"))
                .andExpect(jsonPath("$.items", hasSize(1)));
        mvc.perform(getAs(ALICE, "/api/documents").param("q", "%"))
                .andExpect(jsonPath("$.items", hasSize(1))); // % 는 와일드카드가 아닌 문자로 검색
        mvc.perform(getAs(ALICE, "/api/documents").param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(getAs(ALICE, "/api/documents").param("sort", "evil")).andExpect(status().isBadRequest());
        mvc.perform(getAs(ALICE, "/api/documents").param("size", "1000")).andExpect(status().isBadRequest());
        mvc.perform(getAs(ALICE, "/api/documents").param("folderId", "999999")).andExpect(status().isNotFound());
    }

    @Test
    void moveDoesNotChangeVersionSoEditorAutosaveKeepsWorking() throws Exception {
        long folder = createFolder(ALICE, "보관");
        long id = createDoc(ALICE, "글", "본문", null).get("id").asLong();

        Map<String, Object> body = new HashMap<>();
        body.put("folderId", folder);
        mvc.perform(patchAs(ALICE, "/api/documents/{id}/folder", body, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folder))
                .andExpect(jsonPath("$.version").value(0));

        mvc.perform(putAs(ALICE, "/api/documents/{id}", Map.of("content", doc("계속 작성"), "version", 0), id))
                .andExpect(status().isOk());

        body.put("folderId", null);
        mvc.perform(patchAs(ALICE, "/api/documents/{id}/folder", body, id))
                .andExpect(jsonPath("$.folderId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void deleteDocument() throws Exception {
        long id = createDoc(ALICE, "지울 글", "x", null).get("id").asLong();
        mvc.perform(postAs(ALICE, "/api/documents/{id}/shares", Map.of(), id)).andExpect(status().isCreated());
        mvc.perform(deleteAs(ALICE, "/api/documents/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(getAs(ALICE, "/api/documents/{id}", id)).andExpect(status().isNotFound());
        assertThat(shareRepository.count()).isZero();
    }
}
