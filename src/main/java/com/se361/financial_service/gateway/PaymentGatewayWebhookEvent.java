package com.se361.financial_service.gateway;

import com.se361.financial_service.utils.Constants;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayWebhookEvent {
    private String provider;
    private Constants.PaymentGatewayEventType type;
    private String externalEventId;
    private String gatewayObjectId;
    private String error;
    private Long created;
    private String rawBody;
}
