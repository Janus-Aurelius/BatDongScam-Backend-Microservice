package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {
    APPOINTMENT_BOOKED("APPOINTMENT_BOOKED"),
    APPOINTMENT_CANCELLED("APPOINTMENT_CANCELLED"),
    APPOINTMENT_COMPLETED("APPOINTMENT_COMPLETED"),
    APPOINTMENT_ASSIGNED("APPOINTMENT_ASSIGNED"),
    APPOINTMENT_REMINDER("APPOINTMENT_REMINDER"),
    CONTRACT_UPDATE("CONTRACT_UPDATE"),
    PAYMENT_DUE("PAYMENT_DUE"),
    PAYMENT_OVERDUE("PAYMENT_OVERDUE"),
    VIOLATION_WARNING("VIOLATION_WARNING"),
    PROPERTY_APPROVAL("PROPERTY_APPROVAL"),
    PROPERTY_REJECTION("PROPERTY_REJECTION"),
    SYSTEM_ALERT("SYSTEM_ALERT");

    private final String value;

    public static NotificationTypeEnum get(final String name) {
        if (name == null) return null;
        return Stream.of(NotificationTypeEnum.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid notification type name: %s", name)));
    }
}
