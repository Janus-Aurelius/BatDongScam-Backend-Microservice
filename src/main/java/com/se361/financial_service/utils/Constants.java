package com.se361.financial_service.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

public final class Constants {

    @Getter
    @AllArgsConstructor
    public enum PaymentType {
        DEPOSIT("DEPOSIT"),
        ADVANCE("ADVANCE"),
        FULL_PAY("FULL_PAY"),
        MONTHLY("MONTHLY"),
        SECURITY_DEPOSIT("SECURITY_DEPOSIT"),
        SERVICE_FEE("SERVICE_FEE");

        private final String value;

        public static PaymentType get(final String name){
            return Stream.of(PaymentType.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid role name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PaymentStatus {
        PENDING("PENDING"),
        SUCCESS("SUCCESS"),
        FAILED("FAILED"),
        CANCELLED("CANCELLED"),
        REFUNDED("REFUNDED");

        private final String value;

        public static PaymentStatus get(final String name){
            return Stream.of(PaymentStatus.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid role name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum CommissionStatus {
        PENDING("PENDING"),
        PAID("PAID"),
        CANCELLED("CANCELLED");

        private final String value;

        public static CommissionStatus get(final String name){
            return Stream.of(CommissionStatus.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid role name: %s", name)));
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PaymentGatewayEventType {
        PAYMENT_SUCCEEDED("PAYMENT_SUCCEEDED"),
        PAYMENT_CANCELED("PAYMENT_CANCELED"),
        PAYOUT_PAID("PAYOUT_PAID"),
        PAYOUT_FAILED("PAYOUT_FAILED");

        private final String value;

        public static PaymentGatewayEventType get(final String name){
            return Stream.of(PaymentGatewayEventType.values())
                    .filter(p -> p.name().equals(name.toUpperCase()) || p.getValue().equals(name.toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid role name: %s", name)));
        }
    }

}
