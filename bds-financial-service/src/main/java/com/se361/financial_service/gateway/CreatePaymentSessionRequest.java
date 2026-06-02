package com.se361.financial_service.gateway;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentSessionRequest {
    private BigDecimal amount;
    private String currency;
    private String description;
    private Map<String, Object> metadata;
    private String returnUrl;
    private String webhookUrl;
}
