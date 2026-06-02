package com.se361.financial_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se361.financial_service.dtos.responses.SingleResponse;
import com.se361.financial_service.gateway.paypal.PayPalService;
import com.se361.financial_service.gateway.paypal.dto.PayPalWebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/paypal")
@RequiredArgsConstructor
@Slf4j
public class PayPalWebhookController extends AbstractBaseController {

    private final PayPalService payPalService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<SingleResponse<Void>> handle(@RequestBody String rawBody) {
        try {
            PayPalWebhookEvent event = objectMapper.readValue(rawBody, PayPalWebhookEvent.class);
            payPalService.handleWebhookEvent(event);
        } catch (Exception e) {
            log.error("Failed to process PayPal webhook: {}", e.getMessage());
        }
        return responseFactory.successSingle(null, "Webhook accepted");
    }

}
