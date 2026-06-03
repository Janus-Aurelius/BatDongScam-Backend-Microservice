package com.se.bds.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum PaymentType {
    DEPOSIT("DEPOSIT"),
    ADVANCE("ADVANCE"),
    INSTALLMENT("INSTALLMENT"),
    FULL_PAY("FULL_PAY"),
    MONTHLY("MONTHLY"),
    PENALTY("PENALTY"),
    MONEY_SALE("MONEY_SALE"),
    MONEY_RENTAL("MONEY_RENTAL"),
    SALARY("SALARY"),
    BONUS("BONUS"),
    SERVICE_FEE("SERVICE_FEE"),
    PAYMENT_OVERDUE("PAYMENT_OVERDUE"),
    SECURITY_DEPOSIT("SECURITY_DEPOSIT");

    private final String value;

    public static PaymentType get(final String name) {
        if (name == null) return null;
        return Stream.of(PaymentType.values())
                .filter(p -> p.name().equalsIgnoreCase(name) || p.getValue().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid payment type name: %s", name)));
    }
}
