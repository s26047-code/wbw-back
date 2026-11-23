package com.wbw.api;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FolderApiTest extends ApiTestSupport {

    private long createFolder(String sub, String name) throws Exception {
        return read(mvc.perform(postAs(sub, "/api/folders", Map.of("name", name)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private long createDocIn(String sub, Long folderId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("folderId", folderId);
        body.put("content", doc("본문"));
        return read(mvc.perform(postAs(sub, "/api/documents", body)).andExpect(status().isCreated()).andReturn())
                .get("id").asLong();
    }

    @Test
    void createListAndCount() throws Exception {
        long a = createFolder(ALICE, "에세이");
        createFolder(ALICE, "소설");
        createDocIn(ALICE, a);
        createDocIn(ALICE, a);
        createDocIn(ALICE, null);

        mvc.perform(getAs(ALICE, "/api/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("에세이"))
                .andExpect(jsonPath("$[0].documentCount").value(2))
                .andExpect(jsonPath("$[1].documentCount").value(0));

        mvc.perform(getAs(ALICE, "/api/folders/{id}", a))
                .andExpect(jsonPath("$.documentCount").value(2));
        mvc.perform(getAs(BOB, "/api/folders")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void nameValidationAndDuplicates() throws Exception {
        createFolder(ALICE, "일기");
        mvc.perform(postAs(ALICE, "/api/folders", Map.of("name", " 일기 ")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FOLDER_NAME_DUPLICATED"));
        mvc.perform(postAs(ALICE, "/api/folders", Map.of("name", "   ")))
                .andExpect(status().isBadRequest());
        mvc.perform(postAs(ALICE, "/api/folders", Map.of("name", "가".repeat(51))))
                .andExpect(status().isBadRequest());
        // 다른 사용자는 같은 이름을 쓸 수 있다
        createFolder(BOB, "일기");
    }

    @Test
    void renameAndReorder() throws Exception {
        long id = createFolder(ALICE, "임시");
        createFolder(ALICE, "다른 폴더");
        mvc.perform(patchAs(ALICE, "/api/folders/{id}", Map.of("name", "완성", "sortOrder", 5), id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("완성"))
                .andExpect(jsonPath("$.sortOrder").value(5));
        mvc.perform(patchAs(ALICE, "/api/folders/{id}", Map.of("name", "다른 폴더"), id))
                .andExpect(status().isConflict());
        mvc.perform(patchAs(ALICE, "/api/folders/{id}", Map.of("name", "완성"), id))
                .andExpect(status().isOk()); // 자기 자신과 같은 이름은 허용
        mvc.perform(patchAs(BOB, "/api/folders/{id}", Map.of("name", "남의 것"), id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingFolderDeletesItsDocumentsAndShares() throws Exception {
        long folder = createFolder(ALICE, "지울 폴더");
        long doc1 = createDocIn(ALICE, folder);
        createDocIn(ALICE, folder);
        long kept = createDocIn(ALICE, null);
        mvc.perform(postAs(ALICE, "/api/documents/{id}/shares", Map.of(), doc1)).andExpect(status().isCreated());

        mvc.perform(deleteAs(BOB, "/api/folders/{id}", folder)).andExpect(status().isNotFound());

        mvc.perform(deleteAs(ALICE, "/api/folders/{id}", folder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedDocumentCount").value(2));

        mvc.perform(getAs(ALICE, "/api/folders/{id}", folder)).andExpect(status().isNotFound());
        mvc.perform(getAs(ALICE, "/api/documents/{id}", doc1)).andExpect(status().isNotFound());
        mvc.perform(getAs(ALICE, "/api/documents/{id}", kept)).andExpect(status().isOk());
        assertThat(shareRepository.count()).isZero();
        assertThat(documentRepository.count()).isEqualTo(1);
    }
}
