package com.se361.financial_service.controllers;

import com.se361.financial_service.dtos.responses.SingleResponse;
import com.se361.financial_service.gateway.payway.PaywayWebhookHandler;
import com.se361.financial_service.gateway.payway.PaywayWebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/webhooks/payway")
@RequiredArgsConstructor
@Slf4j
public class PaywayWebhookController extends AbstractBaseController {

    private static final String SIGNATURE_PREFIX = "sha256=";

    @Value("${payway.verify-key:}")
    private String verifyKey;

    private final PaywayWebhookHandler paywayWebhookHandler;

    @PostMapping
    public ResponseEntity<SingleResponse<Void>> handle(
            @RequestHeader(name = "X-Signature", required = false) String signature,
            @RequestBody String rawBody
    ) {
        if (StringUtils.hasText(verifyKey)) {
            if (!StringUtils.hasText(signature)) {
                return responseFactory.sendSingle(null, "Missing X-Signature header", HttpStatus.UNAUTHORIZED);
            }

            String normalizedSig = signature.startsWith(SIGNATURE_PREFIX)
                    ? signature.substring(SIGNATURE_PREFIX.length())
                    : signature;

            byte[] rawBytes = rawBody != null ? rawBody.getBytes(StandardCharsets.UTF_8) : new byte[0];

            if (!PaywayWebhookSignatureVerifier.verify(verifyKey, rawBytes, normalizedSig)) {
                log.warn("Rejected Payway webhook: invalid signature");
                return responseFactory.sendSingle(null, "Invalid signature", HttpStatus.UNAUTHORIZED);
            }
        }

        // Route by event type prefix in body (simple approach)
        if (rawBody != null && rawBody.contains("\"payout.")) {
            paywayWebhookHandler.handlePayoutEvent(rawBody);
        } else {
            paywayWebhookHandler.handlePaymentEvent(rawBody);
        }

        return responseFactory.successSingle(null, "Webhook accepted");
    }

}
