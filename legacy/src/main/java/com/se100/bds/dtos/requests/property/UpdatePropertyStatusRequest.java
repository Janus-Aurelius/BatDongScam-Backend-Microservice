package com.se100.bds.dtos.requests.property;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.se100.bds.utils.Constants;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePropertyStatusRequest {
    @NotNull(message = "Status is required")
    private Constants.PropertyStatusEnum status;
}
