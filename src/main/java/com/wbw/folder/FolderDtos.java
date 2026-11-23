package com.wbw.folder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class FolderDtos {

    private FolderDtos() {
    }

    public record CreateFolderRequest(
            @NotBlank(message = "폴더 이름을 입력해 주세요.")
            @Size(max = 50, message = "폴더 이름은 50자 이하여야 합니다.") String name
    ) {
    }

    /** name, sortOrder 중 보낸 값만 변경한다. */
    public record UpdateFolderRequest(
            @Size(min = 1, max = 50, message = "폴더 이름은 1~50자여야 합니다.") String name,
            @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.") Integer sortOrder
    ) {
    }

    public record FolderResponse(
            Long id,
            String name,
            int sortOrder,
            long documentCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record DeleteFolderResponse(Long id, long deletedDocumentCount) {
    }
}
