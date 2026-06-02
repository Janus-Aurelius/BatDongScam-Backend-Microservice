package com.se100.bds.searchservice.exceptions;

import com.se100.bds.searchservice.dtos.responses.error.DetailedErrorResponse;
import com.se100.bds.searchservice.dtos.responses.error.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class AppExceptionHandler {

    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public final ResponseEntity<ErrorResponse> handleBadRequestException(final Exception e) {
        log.error(e.toString(), e.getMessage());
        return build(HttpStatus.BAD_REQUEST, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ErrorResponse> handleAllExceptions(final Exception e) {
        log.error("Exception: {}", e.getMessage(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
    }

    private ResponseEntity<ErrorResponse> build(final HttpStatus httpStatus, final String message) {
        return build(httpStatus, message, new HashMap<>());
    }

    private ResponseEntity<ErrorResponse> build(final HttpStatus httpStatus,
                                                final String message,
                                                final Map<String, String> errors) {
        if (!errors.isEmpty()) {
            return ResponseEntity.status(httpStatus).body(
                    DetailedErrorResponse.builder()
                        .message(message)
                        .statusCode(httpStatus.value())
                        .items(errors)
                        .build());
        }

        return ResponseEntity.status(httpStatus).body(ErrorResponse.builder()
                .message(message)
                .statusCode(httpStatus.value())
                .build());
    }
}
