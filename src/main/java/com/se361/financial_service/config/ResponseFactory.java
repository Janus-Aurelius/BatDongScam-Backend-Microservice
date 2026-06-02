package com.se361.financial_service.config;

import com.se361.financial_service.dtos.responses.PageResponse;
import com.se361.financial_service.dtos.responses.SingleResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseFactory {
    public <T> ResponseEntity<SingleResponse<T>> successSingle(T data, String message) {
        return ResponseEntity.ok(
                SingleResponse.<T>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message(message)
                        .data(data)
                        .build()
        );
    }

    public <T> ResponseEntity<SingleResponse<T>> sendSingle(T data, String message, HttpStatus status) {
        return ResponseEntity.status(status).body(
                SingleResponse.<T>builder()
                        .statusCode(status.value())
                        .message(message)
                        .data(data)
                        .build()
        );
    }

    public <T> ResponseEntity<PageResponse<T>> successPage(Page<T> page, String message) {
        return ResponseEntity.ok(
                PageResponse.<T>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message(message)
                        .data(page.getContent())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .currentPage(page.getNumber())
                        .pageSize(page.getSize())
                        .build()
        );
    }
}
