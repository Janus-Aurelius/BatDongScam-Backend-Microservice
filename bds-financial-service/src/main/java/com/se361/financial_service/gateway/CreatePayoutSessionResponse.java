package com.se361.financial_service.gateway;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePayoutSessionResponse {
    private String id;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String accountNumber;
    private String accountHolderName;
    private String swiftCode;
    private String description;
    private Map<String, Object> metadata;
    private String webhookUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
