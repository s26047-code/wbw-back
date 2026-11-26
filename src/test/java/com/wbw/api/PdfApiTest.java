package com.wbw.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PdfApiTest extends ApiTestSupport {

    private long createDoc() throws Exception {
        JsonNode content = mapper.readTree("""
                {"type":"doc","content":[
                  {"type":"paragraph","attrs":{"lineHeight":2},"content":[{"type":"text","text":"Hello <PDF> & world","marks":[{"type":"bold"}]}]},
                  {"type":"paragraph","attrs":{"pageBreak":true}},
                  {"type":"paragraph","content":[{"type":"text","text":"second","marks":[{"type":"textStyle","attrs":{"fontSize":"20px","letterSpacing":"0.1em"}}]}]}
                ]}
                """);
        return read(mvc.perform(postAs(ALICE, "/api/documents", Map.of("title", "My/Essay", "content", content)))
                .andReturn()).get("id").asLong();
    }

    @Test
    void exportsPdfWithOptions() throws Exception {
        long id = createDoc();
        MvcResult result = mvc.perform(postAs(ALICE, "/api/documents/{id}/pdf",
                        Map.of("pageSize", "A5", "orientation", "LANDSCAPE", "marginTop", 15, "marginLeft", 10), id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("MyEssay.pdf")))
                .andReturn();
        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void exportsWithDefaultOptionsWhenBodyIsEmpty() throws Exception {
        long id = createDoc();
        mvc.perform(postAs(ALICE, "/api/documents/{id}/pdf", Map.of(), id))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidOptions() throws Exception {
        long id = createDoc();
        mvc.perform(postAs(ALICE, "/api/documents/{id}/pdf", Map.of("marginTop", 100), id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(postAs(ALICE, "/api/documents/{id}/pdf",
                        Map.of("pageSize", "A5", "marginLeft", 60, "marginRight", 60), id))
                .andExpect(status().isBadRequest());
        mvc.perform(postAs(ALICE, "/api/documents/{id}/pdf", Map.of("pageSize", "HUGE"), id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previewHtmlShowsAppliedParagraphStyles() throws Exception {
        long id = createDoc();
        mvc.perform(postAs(ALICE, "/api/documents/{id}/pdf/html", Map.of("pageSize", "A5"), id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/html")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString("line-height:2;")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString("@page{size:148mm 210mm;")));
        mvc.perform(postAs(BOB, "/api/documents/{id}/pdf/html", Map.of(), id)).andExpect(status().isNotFound());
    }

    @Test
    void cannotExportOthersDocument() throws Exception {
        long id = createDoc();
        mvc.perform(postAs(BOB, "/api/documents/{id}/pdf", Map.of(), id)).andExpect(status().isNotFound());
    }
}
