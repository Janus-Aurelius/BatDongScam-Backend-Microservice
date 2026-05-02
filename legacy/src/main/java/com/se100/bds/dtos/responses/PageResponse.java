package com.se100.bds.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class PageResponse<T> extends AbstractBaseResponse {
    private List<T> data;
    private PagingResponse paging;

    public PageResponse(int statusCode, String message, List<T> data, PagingResponse paging) {
        super();
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
        this.paging = paging;
    }

    protected PageResponse() {
        super();
    }

    @Data
    @AllArgsConstructor
    public static class PagingResponse {
        // The current page number being returned
        private int page;
        // The maximum number of items per page
        private int limit;
        // The total number of items across all pages
        private long total;
        // The total number of pages available based on the total items and limit
        private int totalPages;
    }
}