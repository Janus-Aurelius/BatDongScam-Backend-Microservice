package com.se.bds.core.transaction.internal.adapter.in.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreatePurchaseContractWebRequest(
        @NotNull UUID propertyId,
        @NotNull UUID customerId,
        UUID depositContractId,
        @NotNull @Positive BigDecimal agreedPrice,
        @NotNull @Positive BigDecimal advancePaymentAmount,
        @Future LocalDate advancePaymentDeadline,
        @Future LocalDate finalPaymentDeadline,
        String note
        ) {
}
