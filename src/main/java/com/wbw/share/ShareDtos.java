package com.wbw.share;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ShareDtos {

    private ShareDtos() {
    }

    /** password 를 보내면 비밀번호 공유, 생략하면 공개 공유. expiresAt 생략 시 만료 없음 */
    public record CreateShareRequest(
            @Size(min = 4, max = 64, message = "비밀번호는 4~64자여야 합니다.") String password,
            Instant expiresAt
    ) {
    }

    /**
     * 보낸 값만 변경한다.
     * removePassword=true 면 공개 공유로 바꾼다. password 를 보내면 비밀번호를 설정/변경한다.
     * clearExpiresAt=true 면 만료를 없앤다.
     */
    public record UpdateShareRequest(
            Boolean enabled,
            @Size(min = 4, max = 64, message = "비밀번호는 4~64자여야 합니다.") String password,
            Boolean removePassword,
            Instant expiresAt,
            Boolean clearExpiresAt
    ) {
    }

    public record ShareResponse(
            Long id,
            Long documentId,
            String token,
            String url,
            boolean passwordProtected,
            boolean enabled,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record VerifyPasswordRequest(
            @NotBlank(message = "비밀번호를 입력해 주세요.") @Size(max = 64) String password
    ) {
    }

    public record PublicShareResponse(
            String title,
            JsonNode content,
            int charsWithSpaces,
            int charsWithoutSpaces,
            Instant updatedAt
    ) {
    }
}
