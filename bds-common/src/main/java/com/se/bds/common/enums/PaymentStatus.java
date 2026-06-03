package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum PaymentStatus {
    PENDING("PENDING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
    REFUNDED("REFUNDED"),
    SYSTEM_PENDING("SYSTEM_PENDING"),
    SYSTEM_SUCCESS("SYSTEM_SUCCESS"),
    SYSTEM_FAILED("SYSTEM_FAILED");

    private final String value;

    public static PaymentStatus get(final String name) {
        if (name == null) return null;
        return Stream.of(PaymentStatus.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid payment status name: %s", name)));
    }
}
