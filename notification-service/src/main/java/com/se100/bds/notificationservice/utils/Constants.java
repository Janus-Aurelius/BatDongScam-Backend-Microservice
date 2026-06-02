package com.se100.bds.notificationservice.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

public final class Constants {

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
            return Stream.of(NotificationTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid notification type: %s", name)));
        }
    }

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
            return Stream.of(RelatedEntityTypeEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid entity type: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum NotificationStatusEnum {
        PENDING("PENDING"),
        SENT("SENT"),
        READ("READ"),
        FAILED("FAILED");

        private final String value;

        public static NotificationStatusEnum get(final String name) {
            return Stream.of(NotificationStatusEnum.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid status: %s", name)));
        }
    }
}
