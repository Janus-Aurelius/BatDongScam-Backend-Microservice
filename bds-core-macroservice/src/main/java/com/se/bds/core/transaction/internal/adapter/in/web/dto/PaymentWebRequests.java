package com.se.bds.core.transaction.internal.adapter.in.web.dto;

import com.se.bds.common.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public class PaymentWebRequests {
    public record UpdatePaymentStatusRequest(@NotNull PaymentStatus status) {}
}
