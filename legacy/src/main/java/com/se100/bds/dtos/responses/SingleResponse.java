package com.se100.bds.dtos.responses;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class SingleResponse<T> extends AbstractBaseResponse {
    private T data;

    public SingleResponse(int statusCode, String message, T data) {
        super();
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    public SingleResponse() {
        super();
    }
}