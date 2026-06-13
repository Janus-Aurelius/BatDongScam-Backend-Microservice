package com.se361.financial_service.dtos.requests;

import com.se.bds.common.enums.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Schema(description = "Request to create a new payment record")
public class CreatePaymentRequest {
    @NotNull(message = "Contract ID is required")
    @Schema(description = "Associated contract ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID contractId;

    @NotNull(message = "Property ID is required")
    @Schema(description = "Associated property ID", example = "b1a2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6")
    private UUID propertyId;

    @NotNull(message = "Payer ID is required")
    @Schema(description = "ID of the user making the payment", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID payerId;

    @Schema(description = "Payer's full name", example = "John Doe")
    private String payerName;

    @Schema(description = "Title of the property", example = "Luxury Villa")
    private String propertyTitle;

    @Schema(description = "Contract reference number", example = "CON-2026-001")
    private String contractNumber;

    @NotNull(message = "Payment type is required")
    @Schema(description = "Type of payment", example = "DEPOSIT")
    private PaymentType paymentType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Schema(description = "Payment amount", example = "500.00")
    private BigDecimal amount;

    @NotNull(message = "Due date is required")
    @Schema(description = "Payment due date", example = "2026-07-01")
    private LocalDate dueDate;

    @Schema(description = "Installment number if applicable", example = "1")
    private Integer installmentNumber;

    @Schema(description = "Preferred payment method", example = "STRIPE")
    private String paymentMethod;

    @Schema(description = "Additional notes", example = "First deposit for booking")
    private String notes;
}
