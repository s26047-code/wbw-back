package com.wbw.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다."),
    FOLDER_NAME_DUPLICATED(HttpStatus.CONFLICT, "같은 이름의 폴더가 이미 있습니다."),

    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "글을 찾을 수 없습니다."),
    DOCUMENT_VERSION_CONFLICT(HttpStatus.CONFLICT, "다른 곳에서 먼저 수정된 글입니다. 최신 내용을 확인해 주세요."),
    DOCUMENT_CONTENT_INVALID(HttpStatus.BAD_REQUEST, "글 본문 형식이 올바르지 않습니다."),
    DOCUMENT_CONTENT_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "글 본문이 너무 큽니다."),

    SHARE_NOT_FOUND(HttpStatus.NOT_FOUND, "공유 링크를 찾을 수 없습니다."),
    SHARE_EXPIRED(HttpStatus.GONE, "만료된 공유 링크입니다."),
    SHARE_PASSWORD_REQUIRED(HttpStatus.UNAUTHORIZED, "비밀번호가 필요한 공유 글입니다."),
    SHARE_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    SHARE_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "비밀번호 입력 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."),

    PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PDF를 만드는 중 오류가 발생했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
