package com.se.bds.core.shared.ids;

import java.util.UUID;

public record PaymentId(UUID value) {
    public static PaymentId of(UUID value) {
        return new PaymentId(value);
    }
}
