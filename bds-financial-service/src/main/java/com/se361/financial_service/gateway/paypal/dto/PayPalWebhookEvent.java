package com.se361.financial_service.gateway.paypal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalWebhookEvent {

    private String id;
    @JsonProperty("event_type")
    private String eventType;
    @JsonProperty("resource_type")
    private String resourceType;
    private Map<String, Object> resource;
    @JsonProperty("create_time")
    private String createTime;
}
