package com.se100.bds.notificationservice.exceptions;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class AppExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.error("Business error: code={}, message={}", ex.getCode(), ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class,
            EntityNotFoundException.class
    })
    public final ResponseEntity<ApiResponse<Void>> handleBadRequestException(final Exception e) {
        log.error(e.toString(), e.getMessage());
        String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        return new ResponseEntity<>(ApiResponse.error(msg), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ApiResponse<Void>> handleAllExceptions(final Exception e) {
        log.error("Exception: {}", e.getMessage(), e);
        return new ResponseEntity<>(ApiResponse.error("Server error: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
