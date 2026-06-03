package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum PenaltyAppliedEnum {
    WARNING("WARNING"),
    REMOVED_POST("REMOVED_POST"),
    SUSPENDED_ACCOUNT("SUSPENDED_ACCOUNT");

    private final String value;

    public static PenaltyAppliedEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(PenaltyAppliedEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid penalty applied name: %s", name)));
    }
}
