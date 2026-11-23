package com.wbw.document;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    /** 모든 값은 선택. content 생략 시 빈 문서로 생성된다. */
    public record CreateDocumentRequest(
            @Size(max = 200, message = "제목은 200자 이하여야 합니다.") String title,
            Long folderId,
            JsonNode content
    ) {
    }

    /**
     * 저장 / 자동 저장 요청.
     * version: 클라이언트가 마지막으로 받은 버전. 서버 버전과 다르면 409(DOCUMENT_VERSION_CONFLICT).
     * title, content 중 보낸 값만 변경한다.
     */
    public record UpdateDocumentRequest(
            @Size(max = 200, message = "제목은 200자 이하여야 합니다.") String title,
            JsonNode content,
            @NotNull(message = "version 값이 필요합니다.") @PositiveOrZero Long version
    ) {
        @AssertTrue(message = "title 또는 content 중 하나는 보내야 합니다.")
        public boolean isAnyFieldPresent() {
            return title != null || content != null;
        }
    }

    /** folderId 가 null 이면 '폴더 없음'으로 이동 */
    public record MoveDocumentRequest(Long folderId) {
    }

    public record DocumentResponse(
            Long id,
            String title,
            Long folderId,
            String folderName,
            JsonNode content,
            int charsWithSpaces,
            int charsWithoutSpaces,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    /** 자동 저장 응답 (본문을 다시 보내지 않아 가볍다) */
    public record SaveResult(
            Long id,
            String title,
            int charsWithSpaces,
            int charsWithoutSpaces,
            long version,
            Instant updatedAt
    ) {
    }

    public record DocumentSummary(
            Long id,
            String title,
            Long folderId,
            String folderName,
            String preview,
            int charsWithSpaces,
            int charsWithoutSpaces,
            Instant createdAt,
            Instant updatedAt
    ) {
        static DocumentSummary from(DocumentSummaryView v) {
            return new DocumentSummary(v.getId(), v.getTitle(), v.getFolderId(), v.getFolderName(), v.getPreview(),
                    v.getCharsWithSpaces(), v.getCharsWithoutSpaces(), v.getCreatedAt(), v.getUpdatedAt());
        }
    }

    public record PageResponse<T>(
            List<T> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
