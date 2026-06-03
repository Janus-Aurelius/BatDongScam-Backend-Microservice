package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ContributionTierEnum {
    BRONZE("BRONZE"),
    SILVER("SILVER"),
    GOLD("GOLD"),
    PLATINUM("PLATINUM");

    private final String value;

    public static ContributionTierEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(ContributionTierEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid contribution tier name: %s", name)));
    }
}
