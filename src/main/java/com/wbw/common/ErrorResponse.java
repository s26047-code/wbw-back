package com.wbw.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> fieldErrors,
        Map<String, Object> details
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(ErrorCode code) {
        return new ErrorResponse(code.name(), code.message(), List.of(), Map.of());
    }
}
