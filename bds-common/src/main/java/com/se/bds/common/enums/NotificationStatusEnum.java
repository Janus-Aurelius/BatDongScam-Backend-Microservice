package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum NotificationStatusEnum {
    PENDING("PENDING"),
    SENT("SENT"),
    READ("READ"),
    FAILED("FAILED");

    private final String value;

    public static NotificationStatusEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(NotificationStatusEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid notification status name: %s", name)));
    }
}
