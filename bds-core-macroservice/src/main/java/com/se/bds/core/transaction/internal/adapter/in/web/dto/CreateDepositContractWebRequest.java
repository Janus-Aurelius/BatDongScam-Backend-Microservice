package com.se.bds.core.transaction.internal.adapter.in.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDepositContractWebRequest(
        @NotNull UUID propertyId,
        @NotNull UUID customerId,
        @NotNull @Positive BigDecimal depositAmount,
        @NotNull @Positive BigDecimal agreedPrice,
        @NotNull @Future LocalDate expectedSignDate,
        String note

        ) {
}
