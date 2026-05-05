package com.se.bds.core.property.internal.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PropertyTypeWebRequest(
        @NotBlank String typeName,
        String description,
        Boolean isActive
) {
}
