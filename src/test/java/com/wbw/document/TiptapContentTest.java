package com.wbw.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbw.common.ApiException;
import com.wbw.common.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TiptapContentTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String s) throws Exception {
        return mapper.readTree(s);
    }

    @Test
    void extractsParagraphsWithLineBreaksAndKeepsFormattingOutOfText() throws Exception {
        JsonNode doc = json("""
                {"type":"doc","content":[
                  {"type":"paragraph","attrs":{"lineHeight":2},"content":[
                    {"type":"text","text":"굵은 ","marks":[{"type":"bold"}]},
                    {"type":"text","text":"글씨","marks":[{"type":"textStyle","attrs":{"fontFamily":"Nanum Myeongjo","fontSize":"20px"}}]}
                  ]},
                  {"type":"paragraph"},
                  {"type":"paragraph","content":[{"type":"text","text":"둘째"},{"type":"hardBreak"},{"type":"text","text":"줄"}]}
                ]}
                """);
        assertThat(TiptapContent.extractText(doc)).isEqualTo("굵은 글씨\n\n둘째\n줄");
    }

    @Test
    void extractsNestedBlocks() throws Exception {
        JsonNode doc = json("""
                {"type":"doc","content":[
                  {"type":"heading","attrs":{"level":1},"content":[{"type":"text","text":"제목"}]},
                  {"type":"blockquote","content":[{"type":"paragraph","content":[{"type":"text","text":"인용"}]}]},
                  {"type":"bulletList","content":[
                    {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"하나"}]}]},
                    {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"둘"}]}]}
                  ]}
                ]}
                """);
        assertThat(TiptapContent.extractText(doc)).isEqualTo("제목\n인용\n하나\n둘");
    }

    @Test
    void emptyDocHasNoText() throws Exception {
        assertThat(TiptapContent.extractText(json(TiptapContent.EMPTY_DOC))).isEmpty();
    }

    @Test
    void rejectsNonDocContent() throws Exception {
        assertThatThrownBy(() -> TiptapContent.validateAndSerialize(json("{\"type\":\"paragraph\"}"), mapper, 1000))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.DOCUMENT_CONTENT_INVALID);
        assertThatThrownBy(() -> TiptapContent.validateAndSerialize(json("[1,2]"), mapper, 1000))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> TiptapContent.validateAndSerialize(json("{\"type\":\"doc\",\"content\":\"x\"}"), mapper, 1000))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsTooLargeContent() throws Exception {
        String big = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\""
                + "가".repeat(500) + "\"}]}]}";
        assertThatThrownBy(() -> TiptapContent.validateAndSerialize(json(big), mapper, 1000))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.DOCUMENT_CONTENT_TOO_LARGE);
    }

    @Test
    void formattingSurvivesSerializationRoundTrip() throws Exception {
        JsonNode doc = json("""
                {"type":"doc","content":[{"type":"paragraph","attrs":{"lineHeight":1.8,"paragraphSpacing":1.2},
                 "content":[{"type":"text","text":"서식","marks":[{"type":"underline"},
                 {"type":"textStyle","attrs":{"letterSpacing":"0.05em","fontSize":"18px"}}]}]}]}
                """);
        String saved = TiptapContent.validateAndSerialize(doc, mapper, 10_000);
        assertThat(TiptapContent.parse(saved, mapper)).isEqualTo(doc);
    }

    @Test
    void previewCollapsesWhitespaceAndLimitsLength() {
        assertThat(TiptapContent.preview("첫 줄\n\n둘째   줄")).isEqualTo("첫 줄 둘째 줄");
        assertThat(TiptapContent.preview("가".repeat(300)).codePointCount(0, 150)).isEqualTo(150);
        assertThat(TiptapContent.preview("가".repeat(300))).hasSize(150);
    }
}
