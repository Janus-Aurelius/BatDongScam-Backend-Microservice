package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum ReportTypeEnum {
    FINANCIAL("FINANCIAL"),
    AGENT_PERFORMANCE("AGENT_PERFORMANCE"),
    PROPERTY_STATISTICS("PROPERTY_STATISTICS"),
    PROPERTY_OWNER_CONTRIBUTION("PROPERTY_OWNER_CONTRIBUTION"),
    CUSTOMER_ANALYTICS("CUSTOMER_ANALYTICS"),
    VIOLATION("VIOLATION");

    private final String value;

    public static ReportTypeEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(ReportTypeEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid report type name: %s", name)));
    }
}
