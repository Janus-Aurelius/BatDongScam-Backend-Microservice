package com.se100.bds.services.payment.payway;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class PaywayWebhookSignatureVerifierTest {
    @Test
    void shouldVerifyKnownHmacHex() {
        String key = "secret";
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        // hex(HMAC-SHA256("secret", "hello"))
        String expected = "88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b";
        assertTrue(PaywayWebhookSignatureVerifier.verify(key, body, expected));
        assertTrue(PaywayWebhookSignatureVerifier.verify(key, body, expected.toUpperCase()));
        assertFalse(PaywayWebhookSignatureVerifier.verify(key, body, expected.substring(1)));
        assertFalse(PaywayWebhookSignatureVerifier.verify(key, body, expected.replace('8', '9')));
    }
}








