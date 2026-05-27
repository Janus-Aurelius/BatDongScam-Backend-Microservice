package com.se100.bds.dtos.responses.contract;

import com.se100.bds.utils.Constants.ContractStatusEnum;
import com.se100.bds.utils.Constants.RoleEnum;
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
@Schema(description = "Detailed response for a purchase contract")
public class PurchaseContractDetailResponse {

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

    @Schema(description = "Date when physical contract was signed")
    private LocalDateTime signedAt;

    @Schema(description = "Additional terms")
    private String specialTerms;

    // Cancellation fields
    @Schema(description = "Reason for cancellation (if cancelled)")
    private String cancellationReason;

    @Schema(description = "Which party cancelled (if cancelled)")
    private RoleEnum cancelledBy;

    @Schema(description = "When the contract was cancelled")
    private LocalDateTime cancelledAt;

    // Related entities
    @Schema(description = "Property information")
    private PropertySummary property;

    @Schema(description = "Customer information (buyer)")
    private UserSummary customer;

    @Schema(description = "Property owner information (seller)")
    private UserSummary owner;

    @Schema(description = "Sales agent handling the contract")
    private UserSummary agent;

    // Linked deposit contract
    @Schema(description = "Linked deposit contract ID (if any)")
    private UUID depositContractId;

    @Schema(description = "Linked deposit contract status")
    private ContractStatusEnum depositContractStatus;

    // Payments
    @Schema(description = "Payments associated with this contract")
    private List<PaymentSummary> payments;

    // Audit
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertySummary {
        private UUID id;
        private String title;
        private String fullAddress;
        private BigDecimal priceAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private UUID id;
        private String paymentType;
        private BigDecimal amount;
        private LocalDate dueDate;
        private LocalDateTime paidTime;
        private String status;
        private String checkoutUrl;
    }
}

