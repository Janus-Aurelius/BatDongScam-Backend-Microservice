package com.se361.financial_service.gateway.paypal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalOrderResponse {

    private String id;
    private String status;
    private List<Link> links;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link {
        private String href;
        private String rel;
        private String method;
    }

    public String getApproveUrl() {
        if (links == null) return null;
        return links.stream()
                .filter(l -> "approve".equals(l.getRel()))
                .map(Link::getHref)
                .findFirst()
                .orElse(null);
    }
}
