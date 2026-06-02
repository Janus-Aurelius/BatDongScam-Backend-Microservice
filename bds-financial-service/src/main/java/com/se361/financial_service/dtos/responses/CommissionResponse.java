package com.se361.financial_service.dtos.responses;

import com.se361.financial_service.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionResponse {
    private UUID id;
    private UUID paymentId;
    private UUID contractId;
    private UUID propertyId;
    private UUID agentId;
    private String agentName;
    private String propertyTitle;
    private BigDecimal commissionAmount;
    private BigDecimal transactionAmount;
    private LocalDate commissionDate;
    private Constants.CommissionStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
