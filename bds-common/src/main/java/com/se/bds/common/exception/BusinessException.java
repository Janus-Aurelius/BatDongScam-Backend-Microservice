package com.se.bds.common.exception;

import lombok.Getter;

//standardized exception for business logic errors
@Getter
public class BusinessException extends RuntimeException {
    private String code;
    private String message;
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
