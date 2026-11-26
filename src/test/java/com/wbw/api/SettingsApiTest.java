package com.wbw.api;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettingsApiTest extends ApiTestSupport {

    private Map<String, Object> valid() {
        Map<String, Object> m = new HashMap<>();
        m.put("theme", "DARK");
        m.put("fontFamily", "Nanum Myeongjo");
        m.put("fontSize", 18);
        m.put("letterSpacing", 0.02);
        m.put("lineHeight", 2.0);
        m.put("paragraphSpacing", 1.0);
        m.put("editorWidth", 760);
        return m;
    }

    @Test
    void defaultsAreCreatedOnFirstRead() throws Exception {
        mvc.perform(getAs(ALICE, "/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("LIGHT"))
                .andExpect(jsonPath("$.fontFamily").value("Noto Serif KR"))
                .andExpect(jsonPath("$.fontSize").value(17))
                .andExpect(jsonPath("$.editorWidth").value(720));
    }

    @Test
    void updateAndReadBack() throws Exception {
        mvc.perform(putAs(ALICE, "/api/settings", valid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("DARK"));
        mvc.perform(getAs(ALICE, "/api/settings"))
                .andExpect(jsonPath("$.fontFamily").value("Nanum Myeongjo"))
                .andExpect(jsonPath("$.lineHeight").value(2.0));
        // 다른 사용자 설정에는 영향 없음
        mvc.perform(getAs(BOB, "/api/settings")).andExpect(jsonPath("$.theme").value("LIGHT"));
    }

    @Test
    void pretendardCanBeSavedAsDefaultFont() throws Exception {
        Map<String, Object> m = valid();
        m.put("fontFamily", "Pretendard");
        mvc.perform(putAs(ALICE, "/api/settings", m))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontFamily").value("Pretendard"));
    }

    @Test
    void customThemeRequiresColors() throws Exception {
        Map<String, Object> m = valid();
        m.put("theme", "CUSTOM");
        mvc.perform(putAs(ALICE, "/api/settings", m)).andExpect(status().isBadRequest());

        m.put("backgroundColor", "#f5efe3");
        m.put("textColor", "#2b2622");
        mvc.perform(putAs(ALICE, "/api/settings", m))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backgroundColor").value("#F5EFE3"));
    }

    @Test
    void rejectsInvalidValues() throws Exception {
        Map<String, Object> m = valid();
        m.put("backgroundColor", "red");
        mvc.perform(putAs(ALICE, "/api/settings", m)).andExpect(status().isBadRequest());

        m = valid();
        m.put("fontSize", 200);
        mvc.perform(putAs(ALICE, "/api/settings", m)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fontSize"));

        m = valid();
        m.put("fontFamily", "Comic Sans MS");
        mvc.perform(putAs(ALICE, "/api/settings", m)).andExpect(status().isBadRequest());

        m = valid();
        m.put("theme", "RAINBOW");
        mvc.perform(putAs(ALICE, "/api/settings", m)).andExpect(status().isBadRequest());
    }
}
