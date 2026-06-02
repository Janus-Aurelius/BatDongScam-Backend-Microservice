package com.se100.bds.notificationservice.dtos.responses;

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
        private int page;
        private int limit;
        private long total;
        private int totalPages;
    }
}
