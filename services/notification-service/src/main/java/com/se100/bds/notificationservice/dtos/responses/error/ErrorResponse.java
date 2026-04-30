package com.se100.bds.notificationservice.dtos.responses.error;

import com.se100.bds.notificationservice.dtos.responses.AbstractBaseResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ErrorResponse extends AbstractBaseResponse {
    private int statusCode;
    private String message;
}
