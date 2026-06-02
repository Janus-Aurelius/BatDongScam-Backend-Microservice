package com.se361.financial_service.gateway.payway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaywayWebhookEvent<T> {
    private String id;
    private String type;
    private Long created;
    private DataWrapper<T> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataWrapper<T> {
        @JsonProperty("object")
        private T object;
        private String error;
    }
}
