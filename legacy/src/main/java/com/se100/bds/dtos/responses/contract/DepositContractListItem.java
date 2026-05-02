package com.se100.bds.dtos.responses.contract;

import com.se100.bds.utils.Constants.ContractStatusEnum;
import com.se100.bds.utils.Constants.MainContractTypeEnum;
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
@Schema(description = "List item for deposit contracts")
public class DepositContractListItem {

    @Schema(description = "Contract ID")
    private UUID id;

    @Schema(description = "Contract status")
    private ContractStatusEnum status;

    @Schema(description = "Contract number (physical document ID)")
    private String contractNumber;

    @Schema(description = "Type of main contract this deposit is for")
    private MainContractTypeEnum mainContractType;

    @Schema(description = "Deposit amount")
    private BigDecimal depositAmount;

    @Schema(description = "Agreed price for the main contract")
    private BigDecimal agreedPrice;

    @Schema(description = "Start date of contract terms")
    private LocalDate startDate;

    @Schema(description = "End date of contract terms")
    private LocalDate endDate;

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

    // Linked status
    @Schema(description = "Whether this deposit is linked to a main contract")
    private boolean linkedToMainContract;

    // Audit
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}

