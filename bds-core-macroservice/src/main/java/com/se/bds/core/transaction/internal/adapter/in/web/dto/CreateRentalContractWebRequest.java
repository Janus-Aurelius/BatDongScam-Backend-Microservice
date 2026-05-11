package com.se.bds.core.transaction.internal.adapter.in.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateRentalContractWebRequest(
        @NotNull UUID propertyId,
        @NotNull UUID customerId,
        UUID depositContractId,
        @NotNull @Positive BigDecimal monthlyRentAmount,
        @NotNull @Positive BigDecimal securityDepositAmount,
        @NotNull @Positive Integer durationMonths,
        @NotNull @Positive Integer paymentCycleMonths,
        @NotNull @Future LocalDate expectedStartDate,
        String note
        ) {
}
