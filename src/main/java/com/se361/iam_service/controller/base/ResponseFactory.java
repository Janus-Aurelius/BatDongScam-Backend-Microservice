package com.se361.iam_service.controller.base;

import com.se361.iam_service.dto.response.PageResponse;
import com.se361.iam_service.dto.response.SingleResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseFactory {

    public <T> ResponseEntity<SingleResponse<T>> successSingle(T data, String message){
        return ResponseEntity.ok(
                SingleResponse.<T>builder()
                        .statusCode(200)
                        .message(message)
                        .data(data)
                        .build()
        );
    }

    public <T> ResponseEntity<SingleResponse<T>> failedSingle(T data, String message) {
        return ResponseEntity.badRequest().body(
                SingleResponse.<T>builder()
                        .statusCode(400)
                        .message(message)
                        .data(data)
                        .build()
        );
    }

    public <T> ResponseEntity<PageResponse<T>> successPage(Page<T> page, String message) {
        return ResponseEntity.ok(
                PageResponse.<T>builder()
                        .statusCode(200)
                        .message(message)
                        .data(page.getContent())
                        .paging(PageResponse.PagingInfo.builder()
                                .page(page.getNumber())
                                .size(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .build())
                        .build()
        );
    }
}
