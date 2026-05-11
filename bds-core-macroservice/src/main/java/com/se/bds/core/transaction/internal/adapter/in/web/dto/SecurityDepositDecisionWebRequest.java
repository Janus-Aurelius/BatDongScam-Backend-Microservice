package com.se.bds.core.transaction.internal.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SecurityDepositDecisionWebRequest(
        //TODO: check the decision type and business context
        @NotNull String decision, //e.g "REFUND_FULL", "DEDUCT_DAMAGE"
        BigDecimal deductionAmount,
        String reason

) {
}
