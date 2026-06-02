package com.se361.financial_service.dtos.responses.error;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class DetailedErrorResponse extends ErrorResponse{
    private Map<String, String> items;

    @Builder
    public DetailedErrorResponse(int statusCode, String message, Map<String, String> items) {
        super(statusCode, message);
        this.items = items;
    }
}
