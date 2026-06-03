package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ViolationStatusEnum {
    PENDING("PENDING"),
    REPORTED("REPORTED"),
    UNDER_REVIEW("UNDER_REVIEW"),
    RESOLVED("RESOLVED"),
    DISMISSED("DISMISSED");

    private final String value;

    public static ViolationStatusEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(ViolationStatusEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid violation status name: %s", name)));
    }
}
