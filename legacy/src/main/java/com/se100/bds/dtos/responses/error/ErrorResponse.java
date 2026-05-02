package com.se100.bds.dtos.responses.error;

import com.se100.bds.dtos.responses.AbstractBaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ErrorResponse extends AbstractBaseResponse {

    @Schema(
            name = "statusCode",
            description = "HTTP status code",
            type = "Integer",
            example = "400"
    )
    private int statusCode;

    @Schema(
            name = "message",
            description = "Response messages field",
            type = "String",
            example = "This is message field"
    )
    private String message;
}
