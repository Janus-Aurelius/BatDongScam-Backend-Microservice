package com.se100.bds.services.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned by the payment gateway when creating a payout session")
public class CreatePayoutSessionResponse {

    @Schema(description = "Gateway-side payout id (UUID)")
    private String id;

    @Schema(description = "Amount in minor units")
    private BigDecimal amount;

    private String currency;

    /** created | paid | failed */
    private String status;

    @Schema(name = "account_number")
    private String accountNumber;

    @Schema(name = "account_holder_name")
    private String accountHolderName;

    @Schema(name = "swift_code")
    private String swiftCode;

    private String description;

    private Map<String, Object> metadata;

    @Schema(name = "webhook_url")
    private String webhookUrl;

    @Schema(name = "created_at")
    private OffsetDateTime createdAt;

    @Schema(name = "updated_at")
    private OffsetDateTime updatedAt;
}
