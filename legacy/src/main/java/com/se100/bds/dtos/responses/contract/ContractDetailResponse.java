package com.se100.bds.dtos.responses.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed contract response")
public class ContractDetailResponse {

    @Schema(description = "Contract ID")
    private UUID id;

    @Schema(description = "Contract number")
    private String contractNumber;

    @Schema(description = "Contract type: PURCHASE, RENTAL")
    private String contractType;

    @Schema(description = "Contract status: DRAFT, PENDING_SIGNING, ACTIVE, COMPLETED, CANCELLED")
    private String status;

    @Schema(description = "Contract payment type: MORTGAGE, MONTHLY_RENT, PAID_IN_FULL")
    private String contractPaymentType;

    // Financial details
    @Schema(description = "Total contract amount")
    private BigDecimal totalContractAmount;

    @Schema(description = "Deposit amount")
    private BigDecimal depositAmount;

    @Schema(description = "Remaining amount")
    private BigDecimal remainingAmount;

    @Schema(description = "Advance payment amount")
    private BigDecimal advancePaymentAmount;

    @Schema(description = "Number of installments")
    private Integer installmentAmount;

    @Schema(description = "Progress milestone")
    private BigDecimal progressMilestone;

    @Schema(description = "Final payment amount")
    private BigDecimal finalPaymentAmount;

    @Schema(description = "Late payment penalty rate")
    private BigDecimal latePaymentPenaltyRate;

    // Dates
    @Schema(description = "Contract start date")
    private LocalDate startDate;

    @Schema(description = "Contract end date")
    private LocalDate endDate;

    @Schema(description = "When the contract was signed")
    private LocalDateTime signedAt;

    @Schema(description = "When the contract was completed")
    private LocalDateTime completedAt;

    // Terms and conditions
    @Schema(description = "Special terms")
    private String specialTerms;

    @Schema(description = "Special conditions")
    private String specialConditions;

    // Cancellation info
    @Schema(description = "Cancellation reason (if cancelled)")
    private String cancellationReason;

    @Schema(description = "Cancellation penalty amount (if cancelled)")
    private BigDecimal cancellationPenalty;

    @Schema(description = "Who cancelled the contract")
    private String cancelledBy;

    // Rating
    @Schema(description = "Contract rating (1-5)")
    private Short rating;

    @Schema(description = "Rating comment")
    private String comment;

    // Property info
    @Schema(description = "Property ID")
    private UUID propertyId;

    @Schema(description = "Property title")
    private String propertyTitle;

    @Schema(description = "Property address")
    private String propertyAddress;

    @Schema(description = "Property price")
    private BigDecimal propertyPrice;

    @Schema(description = "Property type")
    private String propertyType;

    @Schema(description = "Property transaction type")
    private String propertyTransactionType;

    // Customer info
    @Schema(description = "Customer ID")
    private UUID customerId;

    @Schema(description = "Customer first name")
    private String customerFirstName;

    @Schema(description = "Customer last name")
    private String customerLastName;

    @Schema(description = "Customer phone")
    private String customerPhone;

    @Schema(description = "Customer email")
    private String customerEmail;

    // Owner info
    @Schema(description = "Property owner ID")
    private UUID ownerId;

    @Schema(description = "Owner first name")
    private String ownerFirstName;

    @Schema(description = "Owner last name")
    private String ownerLastName;

    @Schema(description = "Owner phone")
    private String ownerPhone;

    // Agent info
    @Schema(description = "Agent ID")
    private UUID agentId;

    @Schema(description = "Agent first name")
    private String agentFirstName;

    @Schema(description = "Agent last name")
    private String agentLastName;

    @Schema(description = "Agent employee code")
    private String agentEmployeeCode;

    @Schema(description = "Agent phone")
    private String agentPhone;

    // Payment summary
    @Schema(description = "Total payments made")
    private BigDecimal totalPaymentsMade;

    @Schema(description = "Number of payments")
    private int paymentCount;

    @Schema(description = "List of payments")
    private List<PaymentSummary> payments;

    @Schema(description = "When the contract was created")
    private LocalDateTime createdAt;

    @Schema(description = "When the contract was last updated")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private UUID id;
        private String paymentType;
        private String status;
        private BigDecimal amount;
        private LocalDate dueDate;
        private LocalDateTime paidTime;
        private Integer installmentNumber;
    }
}
