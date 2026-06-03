package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum PerformanceTierEnum {
    BRONZE("BRONZE"),
    SILVER("SILVER"),
    GOLD("GOLD"),
    PLATINUM("PLATINUM");

    private final String value;

    public static PerformanceTierEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(PerformanceTierEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid performance tier name: %s", name)));
    }
}
