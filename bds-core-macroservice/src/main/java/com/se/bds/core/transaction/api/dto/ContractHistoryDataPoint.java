package com.se.bds.core.transaction.api.dto;

import java.time.LocalDate;

public record ContractHistoryDataPoint(
    LocalDate startDate,
    LocalDate endDate,
    String status
) {}
