package com.se361.iam_service.controller.base;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.dto.PagedData;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseFactory {

    public <T> ResponseEntity<ApiResponse<T>> successSingle(T data, String message){
        return ResponseEntity.ok(
                ApiResponse.<T>builder()
                        .success(true)
                        .message(message)
                        .data(data)
                        .build()
        );
    }

    public <T> ResponseEntity<ApiResponse<T>> failedSingle(T data, String message) {
        return ResponseEntity.badRequest().body(
                ApiResponse.<T>builder()
                        .success(false)
                        .message(message)
                        .data(data)
                        .build()
        );
    }

    public <T> ResponseEntity<ApiResponse<PagedData<T>>> successPage(Page<T> page, String message) {
        return ResponseEntity.ok(
                ApiResponse.<PagedData<T>>builder()
                        .success(true)
                        .message(message)
                        .data(PagedData.<T>builder()
                                .content(page.getContent())
                                .pageNumber(page.getNumber())
                                .pageSize(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .build())
                        .build()
        );
    }
}
