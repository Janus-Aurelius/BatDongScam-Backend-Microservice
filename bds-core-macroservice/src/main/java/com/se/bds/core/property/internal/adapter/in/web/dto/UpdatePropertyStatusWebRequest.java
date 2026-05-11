package com.se.bds.core.property.internal.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePropertyStatusWebRequest(
        @NotBlank String status
) {
}
