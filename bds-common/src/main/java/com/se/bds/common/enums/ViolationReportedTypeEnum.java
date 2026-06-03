package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ViolationReportedTypeEnum {
    CUSTOMER("CUSTOMER"),
    PROPERTY("PROPERTY"),
    SALES_AGENT("SALES_AGENT"),
    PROPERTY_OWNER("PROPERTY_OWNER");

    private final String value;

    public static ViolationReportedTypeEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(ViolationReportedTypeEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid violation reported type name: %s", name)));
    }
}
