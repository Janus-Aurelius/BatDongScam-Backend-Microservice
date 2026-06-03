package com.se361.financial_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.dto.ApiResponse;
import com.se361.financial_service.gateway.paypal.PayPalService;
import com.se361.financial_service.gateway.paypal.dto.PayPalWebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/paypal")
@RequiredArgsConstructor
@Slf4j
public class PayPalWebhookController {

    private final PayPalService payPalService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> handle(
            @RequestHeader(name = "PAYPAL-TRANSMISSION-ID", required = false) String transmissionId,
            @RequestHeader(name = "PAYPAL-TRANSMISSION-SIG", required = false) String transmissionSig,
            @RequestHeader(name = "PAYPAL-TRANSMISSION-TIME", required = false) String transmissionTime,
            @RequestHeader(name = "PAYPAL-CERT-URL", required = false) String certUrl,
            @RequestHeader(name = "PAYPAL-AUTH-ALGO", required = false) String authAlgo,
            @RequestBody String rawBody
    ) {
        // Cryptographic verification
        if (transmissionId != null && transmissionSig != null && transmissionTime != null && certUrl != null && authAlgo != null) {
            boolean isValid = payPalService.verifySignature(
                    authAlgo, certUrl, transmissionId, transmissionSig, transmissionTime, rawBody
            );
            if (!isValid) {
                log.warn("PayPal webhook signature verification failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid signature"));
            }
        } else {
            log.warn("Missing PayPal transmission headers, skipping signature verification");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Missing signature headers"));
        }

        try {
            PayPalWebhookEvent event = objectMapper.readValue(rawBody, PayPalWebhookEvent.class);
            payPalService.handleWebhookEvent(event);
        } catch (Exception e) {
            log.error("Failed to process PayPal webhook: {}", e.getMessage());
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Webhook accepted", null));
    }

}
