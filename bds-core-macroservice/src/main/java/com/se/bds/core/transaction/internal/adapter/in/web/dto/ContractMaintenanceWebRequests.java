package com.se.bds.core.transaction.internal.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ContractMaintenanceWebRequests {
    public record UpdateContractDraftRequest(
            @NotNull LocalDate startDate,
            LocalDate endDate,
            String specialTerms
    ) {}

    public record RateContractRequest(
            @NotNull @Min(1) @Max(5) Short rating,
            String comment
    ) {}
}
