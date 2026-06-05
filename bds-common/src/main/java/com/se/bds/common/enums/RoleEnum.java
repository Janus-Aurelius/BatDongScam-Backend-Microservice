package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum RoleEnum {
    ADMIN("ADMIN"),
    SALESAGENT("SALESAGENT"),
    PROPERTY_OWNER("PROPERTY_OWNER"),
    CUSTOMER("CUSTOMER");

    private final String value;

    public static RoleEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(RoleEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid role name: %s", name)));
    }
}
