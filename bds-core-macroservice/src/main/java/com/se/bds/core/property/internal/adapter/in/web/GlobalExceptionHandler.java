package com.se.bds.core.property.internal.adapter.in.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Dữ liệu đầu vào không hợp lệ!");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllUncaughtException(Exception ex) {
        log.error("[Centralized Error Tracking] Hệ thống đang bận", ex); // Khớp với Utility_Tree.csv
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Hệ thống đang bận, vui lòng thử lại sau!");
    }
}
