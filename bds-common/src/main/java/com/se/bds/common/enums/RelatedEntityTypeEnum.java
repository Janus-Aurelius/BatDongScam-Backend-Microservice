package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum RelatedEntityTypeEnum {
    PROPERTY("PROPERTY"),
    CONTRACT("CONTRACT"),
    PAYMENT("PAYMENT"),
    APPOINTMENT("APPOINTMENT"),
    USER("USER");

    private final String value;

    public static RelatedEntityTypeEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(RelatedEntityTypeEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid related entity type name: %s", name)));
    }
}
