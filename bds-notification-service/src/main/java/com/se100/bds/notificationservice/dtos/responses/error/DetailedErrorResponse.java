package com.se100.bds.notificationservice.dtos.responses.error;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@Setter
@SuperBuilder
public class DetailedErrorResponse extends ErrorResponse {
    private Map<String, String> items;
}
