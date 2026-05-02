package com.se100.bds.dtos.responses.contract;

import com.se100.bds.utils.Constants.ContractStatusEnum;
import com.se100.bds.utils.Constants.SecurityDepositStatusEnum;
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
@Schema(description = "List item for rental contracts")
public class RentalContractListItem {

    @Schema(description = "Contract ID")
    private UUID id;

    @Schema(description = "Contract status")
    private ContractStatusEnum status;

    @Schema(description = "Contract number (physical document ID)")
    private String contractNumber;

    @Schema(description = "Number of months for the rental period")
    private Integer monthCount;

    @Schema(description = "Monthly rent amount")
    private BigDecimal monthlyRentAmount;

    @Schema(description = "Commission amount per month")
    private BigDecimal commissionAmount;

    @Schema(description = "Security deposit amount")
    private BigDecimal securityDepositAmount;

    @Schema(description = "Security deposit status")
    private SecurityDepositStatusEnum securityDepositStatus;

    @Schema(description = "Start date of rental period")
    private LocalDate startDate;

    @Schema(description = "End date of rental period")
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

    // Linked deposit
    @Schema(description = "Whether this rental has a linked deposit contract")
    private boolean hasDepositContract;

    // Audit
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
