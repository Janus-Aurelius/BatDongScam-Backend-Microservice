package com.se100.bds.dtos.responses.contract;

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
@Schema(description = "Contract list item for listing and searching")
public class ContractListItem {

    @Schema(description = "Contract ID")
    private UUID id;

    @Schema(description = "Contract number")
    private String contractNumber;

    @Schema(description = "Contract type: PURCHASE, RENTAL")
    private String contractType;

    @Schema(description = "Contract status: DRAFT, PENDING_SIGNING, ACTIVE, COMPLETED, CANCELLED")
    private String status;

    @Schema(description = "Total contract amount")
    private BigDecimal totalContractAmount;

    @Schema(description = "Contract start date")
    private LocalDate startDate;

    @Schema(description = "Contract end date")
    private LocalDate endDate;

    @Schema(description = "When the contract was signed")
    private LocalDateTime signedAt;

    // Property info
    @Schema(description = "Property ID")
    private UUID propertyId;

    @Schema(description = "Property title")
    private String propertyTitle;

    @Schema(description = "Property address")
    private String propertyAddress;

    // Customer info
    @Schema(description = "Customer ID")
    private UUID customerId;

    @Schema(description = "Customer first name")
    private String customerFirstName;

    @Schema(description = "Customer last name")
    private String customerLastName;

    // Agent info
    @Schema(description = "Agent ID")
    private UUID agentId;

    @Schema(description = "Agent first name")
    private String agentFirstName;

    @Schema(description = "Agent last name")
    private String agentLastName;

    @Schema(description = "Agent employee code")
    private String agentEmployeeCode;

    @Schema(description = "When the contract was created")
    private LocalDateTime createdAt;
}
