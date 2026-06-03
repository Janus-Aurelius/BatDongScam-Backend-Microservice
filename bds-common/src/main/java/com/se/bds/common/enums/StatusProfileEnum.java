package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum StatusProfileEnum {
    ACTIVE("ACTIVE"),
    SUSPENDED("SUSPENDED"),
    PENDING_APPROVAL("PENDING_APPROVAL"),
    DELETED("DELETED"),
    REJECTED("REJECTED");

    private final String value;

    public static StatusProfileEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(StatusProfileEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid status profile name: %s", name)));
    }
}
