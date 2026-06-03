package com.se361.financial_service.controllers;

import com.se.bds.common.dto.ApiResponse;
import com.se361.financial_service.services.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/payos")
@RequiredArgsConstructor
@Slf4j
public class PayOSWebhookController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> handleWebhook(@RequestBody String rawBody) {
        try {
            paymentService.handlePayOSWebhook(rawBody);
            return ResponseEntity.ok(new ApiResponse<>(true, "PayOS Webhook processed", null));
        } catch (Exception e) {
            log.error("Error processing PayOS webhook", e);
            return ResponseEntity.ok(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }
}
