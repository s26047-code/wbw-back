package com.wbw.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbw.common.ApiException;
import com.wbw.common.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * TipTap(ProseMirror) 문서 JSON 처리 유틸리티.
 * 서식 정보는 이 JSON 안에 그대로 보존되며, 서버는 여기서 순수 텍스트를 추출해 글자 수를 계산한다.
 */
public final class TiptapContent {

    public static final String EMPTY_DOC = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}";

    /** 앞에 줄바꿈을 두어 구분하는 텍스트 블록 */
    private static final Set<String> TEXT_BLOCKS = Set.of("paragraph", "heading", "codeBlock");

    private TiptapContent() {
    }

    /** 형식과 크기를 검증하고 저장용 JSON 문자열로 직렬화한다. */
    public static String validateAndSerialize(JsonNode content, ObjectMapper mapper, int maxBytes) {
        if (content == null || !content.isObject() || !"doc".equals(content.path("type").asText())) {
            throw new ApiException(ErrorCode.DOCUMENT_CONTENT_INVALID, "본문은 type이 \"doc\"인 TipTap JSON이어야 합니다.");
        }
        JsonNode children = content.get("content");
        if (children != null && !children.isArray()) {
            throw new ApiException(ErrorCode.DOCUMENT_CONTENT_INVALID, "본문의 content는 배열이어야 합니다.");
        }
        String json;
        try {
            json = mapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.DOCUMENT_CONTENT_INVALID);
        }
        if (json.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            throw new ApiException(ErrorCode.DOCUMENT_CONTENT_TOO_LARGE);
        }
        return json;
    }

    public static JsonNode parse(String json, ObjectMapper mapper) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 본문 JSON을 읽을 수 없습니다.", e);
        }
    }

    /** 문단은 줄바꿈으로 구분한 순수 텍스트를 반환한다. */
    public static String extractText(JsonNode doc) {
        StringBuilder sb = new StringBuilder();
        append(doc, sb);
        // 끝의 불필요한 줄바꿈 제거
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == '\n') {
            end--;
        }
        return sb.substring(0, end);
    }

    private static void append(JsonNode node, StringBuilder sb) {
        if (node == null || !node.isObject()) {
            return;
        }
        String type = node.path("type").asText("");
        switch (type) {
            case "text" -> sb.append(node.path("text").asText(""));
            case "hardBreak" -> sb.append('\n');
            default -> {
                if (TEXT_BLOCKS.contains(type) && !sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n') {
                    sb.append('\n');
                }
                JsonNode children = node.get("content");
                if (children != null && children.isArray()) {
                    for (JsonNode child : children) {
                        append(child, sb);
                    }
                }
                if (TEXT_BLOCKS.contains(type)) {
                    sb.append('\n');
                }
            }
        }
    }

    /** 목록 화면용 미리보기 (줄바꿈·연속 공백을 하나의 공백으로, 최대 150자) */
    public static String preview(String plainText) {
        String collapsed = plainText.replaceAll("\\s+", " ").trim();
        int[] cps = collapsed.codePoints().limit(150).toArray();
        return new String(cps, 0, cps.length);
    }
}
