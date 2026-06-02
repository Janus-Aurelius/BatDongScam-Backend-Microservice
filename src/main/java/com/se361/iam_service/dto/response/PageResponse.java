package com.se361.iam_service.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private int statusCode;
    private String message;
    private List<T> data;
    private PagingInfo paging;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagingInfo {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
