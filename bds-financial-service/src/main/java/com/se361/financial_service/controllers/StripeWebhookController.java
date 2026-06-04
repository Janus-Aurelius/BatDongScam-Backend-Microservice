package com.se361.financial_service.controllers;

import com.se.bds.common.dto.ApiResponse;
import com.se361.financial_service.services.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestHeader(name = "Stripe-Signature", required = false) String sigHeader,
            @RequestBody String payload
    ) {
        log.info("Received Stripe webhook request");
        if (sigHeader == null) {
            log.warn("Missing Stripe-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Missing Stripe-Signature header"));
        }

        try {
            paymentService.handleStripeWebhook(payload, sigHeader);
            return ResponseEntity.ok(new ApiResponse<>(true, "Webhook processed successfully", null));
        } catch (Exception e) {
            log.error("Failed to process Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to process webhook: " + e.getMessage()));
        }
    }
}
