package com.se100.bds.services.payment.payway.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaywayPaymentResponseJacksonTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldDeserializeSnakeCaseResponse() throws Exception {
        String json = "{"
                + "\"id\":\"7f52f1e4-7e0f-4a40-8b68-99e972f1d2a1\","
                + "\"amount\":1200,"
                + "\"currency\":\"USD\","
                + "\"status\":\"pending\","
                + "\"description\":\"test\","
                + "\"metadata\":{\"k\":\"v\"},"
                + "\"return_url\":\"https://ui/return\","
                + "\"webhook_url\":\"https://api/webhooks/payway\","
                + "\"checkout_url\":\"https://checkout/abc\","
                + "\"created_at\":\"2025-01-01T00:00:00Z\","
                + "\"updated_at\":\"2025-01-01T00:00:01Z\""
                + "}";

        PaywayPaymentResponse resp = mapper.readValue(json, PaywayPaymentResponse.class);

        assertEquals(1200, resp.getAmount());
        assertEquals("https://checkout/abc", resp.getCheckoutUrl());
        assertEquals("https://ui/return", resp.getReturnUrl());
        assertEquals(OffsetDateTime.parse("2025-01-01T00:00:00Z"), resp.getCreatedAt());
    }
}

