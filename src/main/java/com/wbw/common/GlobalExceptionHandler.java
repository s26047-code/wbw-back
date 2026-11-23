package com.wbw.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.status())
                .body(new ErrorResponse(code.name(), e.getMessage(), List.of(), e.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        List<ErrorResponse.FieldError> globals = e.getBindingResult().getGlobalErrors().stream()
                .map(g -> new ErrorResponse.FieldError(g.getObjectName(), g.getDefaultMessage()))
                .toList();
        List<ErrorResponse.FieldError> all = new java.util.ArrayList<>(fields);
        all.addAll(globals);
        return badRequest(all);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException e) {
        List<ErrorResponse.FieldError> fields = e.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return badRequest(fields);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleBadInput(Exception e) {
        return badRequest(List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다.", List.of(), Map.of()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("UNSUPPORTED_MEDIA_TYPE", "JSON 형식으로 요청해 주세요.", List.of(), Map.of()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "요청한 주소를 찾을 수 없습니다.", List.of(), Map.of()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(ErrorCode.DOCUMENT_VERSION_CONFLICT));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", "요청을 처리할 수 없습니다. 중복되었거나 이미 변경된 데이터입니다.", List.of(), Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        // 상태 코드를 가진 Spring MVC 예외(406, 413 등)는 해당 상태로 응답한다.
        if (e instanceof org.springframework.web.ErrorResponse springError
                && !springError.getStatusCode().is5xxServerError()) {
            return ResponseEntity.status(springError.getStatusCode())
                    .body(new ErrorResponse("REQUEST_ERROR", "요청을 처리할 수 없습니다.", List.of(), Map.of()));
        }
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }

    private ResponseEntity<ErrorResponse> badRequest(List<ErrorResponse.FieldError> fields) {
        ErrorCode code = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(code.status())
                .body(new ErrorResponse(code.name(), code.message(), fields, Map.of()));
    }
}
