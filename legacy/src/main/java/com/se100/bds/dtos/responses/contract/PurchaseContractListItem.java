package com.se100.bds.dtos.responses.contract;

import com.se100.bds.utils.Constants.ContractStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "List item for purchase contracts")
public class PurchaseContractListItem {

    @Schema(description = "Contract ID")
    private UUID id;

    @Schema(description = "Contract status")
    private ContractStatusEnum status;

    @Schema(description = "Contract number (physical document ID)")
    private String contractNumber;

    @Schema(description = "Property purchase price")
    private BigDecimal propertyValue;

    @Schema(description = "Advance payment amount")
    private BigDecimal advancePaymentAmount;

    @Schema(description = "Commission amount")
    private BigDecimal commissionAmount;

    @Schema(description = "Start date of contract terms")
    private LocalDate startDate;

    // Property summary
    @Schema(description = "Property ID")
    private UUID propertyId;

    @Schema(description = "Property title")
    private String propertyTitle;

    // Customer summary
    @Schema(description = "Customer ID")
    private UUID customerId;

    @Schema(description = "Customer full name")
    private String customerName;

    // Agent summary
    @Schema(description = "Agent ID")
    private UUID agentId;

    @Schema(description = "Agent full name")
    private String agentName;

    // Linked deposit
    @Schema(description = "Whether this purchase has a linked deposit contract")
    private boolean hasDepositContract;

    // Audit
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}

