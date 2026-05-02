package com.se100.bds.services.domains.payment.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider-agnostic webhook event emitted by payment gateways.
 * Domain code should depend on this rather than Payway-specific DTOs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayWebhookEvent {

    private PaymentGatewayProvider provider;

    private PaymentGatewayEventType type;

    /** Payway event id (top-level event.id) */
    private String externalEventId;

    /** Payment/payout id at the gateway (data.object.id) */
    private String gatewayObjectId;

    /** Optional error (data.error) */
    private String error;

    /** Unix epoch seconds (top-level created) */
    private Long created;

    /** Raw request body for debugging/audit (optional) */
    private String rawBody;
}
