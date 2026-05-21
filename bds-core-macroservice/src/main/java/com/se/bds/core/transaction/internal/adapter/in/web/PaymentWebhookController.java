package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.core.transaction.internal.application.port.in.PaymentWebhookUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/payway")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentWebhookUseCase paymentWebhookUseCase;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        
        log.info("[ACCOUNTS] Webhook received from gateway, signature={}", signature);

        boolean result = paymentWebhookUseCase.processWebhook(rawBody, signature);
        if (result) {
            log.info("[EVENT] Webhook process request succeeded.");
        } else {
            log.warn("[EVENT] Webhook process request failed, returning 200 OK anyway for ignore-faulty-behavior tactic.");
        }

        return ResponseEntity.ok("OK");
    }
}
