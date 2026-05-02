package com.se100.bds.services.payment.payway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaywayWebhookEvent<T> {

    private String id;

    private String type;

    /** Unix epoch seconds */
    private Long created;

    private DataWrapper<T> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataWrapper<T> {
        @JsonProperty("object")
        private T object;

        /**
         * Optional error message (present for some failed payout events, etc.).
         */
        private String error;
    }
}
