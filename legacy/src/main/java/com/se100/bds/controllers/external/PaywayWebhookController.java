package com.se100.bds.controllers.external;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.error.ErrorResponse;
import com.se100.bds.services.domains.payment.PaymentService;
import com.se100.bds.services.payment.payway.PaywayWebhookSignatureVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks/payway")
@Tag(name = "101. Payway Webhooks", description = "Payway Webhook API")
@Slf4j
public class PaywayWebhookController extends AbstractBaseController {

    private static final String SIGNATURE_PREFIX = "sha256=";

    @Value("${payway.verify-key:}")
    private String verifyKey;

    private final PaymentService paymentService;

    @PostMapping
    @Operation(
            summary = "Handle Payway webhook callback (optional X-Signature: sha256=<hex> over raw body)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Webhook accepted",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid signature",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<Void>> handle(
            @RequestHeader(name = "X-Signature", required = false) String signature,
            @RequestBody String rawBody
    ) {
        // Contract: signature header exists only if signing secret is set.
        // If we have a secret configured, require and verify signature.
        if (StringUtils.hasText(verifyKey)) {
            if (!StringUtils.hasText(signature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(responseFactory.createSingleResponse(HttpStatus.UNAUTHORIZED, "Missing X-Signature header", null));
            }

            String normalizedSig = signature;
            if (normalizedSig.startsWith(SIGNATURE_PREFIX)) {
                normalizedSig = normalizedSig.substring(SIGNATURE_PREFIX.length());
            }

            byte[] rawBytes = rawBody != null ? rawBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
            boolean ok = PaywayWebhookSignatureVerifier.verify(verifyKey, rawBytes, normalizedSig);
            if (!ok) {
                log.warn("Rejected Payway webhook: invalid signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(responseFactory.createSingleResponse(HttpStatus.UNAUTHORIZED, "Invalid signature", null));
            }
        }

        paymentService.handlePaywayWebhook(rawBody);
        return responseFactory.successSingle(null, "Webhook verified");
    }
}
