package com.se361.financial_service.dtos.responses.error;

import lombok.experimental.SuperBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class DetailedErrorResponse extends ErrorResponse{
    private Map<String, String> items;
}
