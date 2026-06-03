package com.se361.financial_service.controllers;

import com.se.bds.common.dto.ApiResponse;
import com.se361.financial_service.gateway.CreatePaymentSessionRequest;
import com.se361.financial_service.gateway.CreatePaymentSessionResponse;
import com.se361.financial_service.gateway.CreatePayoutSessionRequest;
import com.se361.financial_service.gateway.CreatePayoutSessionResponse;
import com.se361.financial_service.gateway.paypal.PayPalService;
import com.se361.financial_service.gateway.payway.PaywayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/payments")
@RequiredArgsConstructor
@Slf4j
public class InternalPaymentController {

    private final PaywayService paywayService;
    private final PayPalService payPalService;

    @PostMapping("/session")
    public ResponseEntity<ApiResponse<CreatePaymentSessionResponse>> createPaymentSession(
            @Valid @RequestBody CreatePaymentSessionRequest request
    ) {
        log.info("[INTERNAL API] Request to create payment session: {}", request);
        CreatePaymentSessionResponse response;
        if ("USD".equalsIgnoreCase(request.getCurrency()) || 
            (request.getMetadata() != null && "paypal".equalsIgnoreCase(String.valueOf(request.getMetadata().get("gateway"))))) {
            response = payPalService.createOrder(request);
        } else {
            response = paywayService.createPaymentSession(request);
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/payout")
    public ResponseEntity<ApiResponse<CreatePayoutSessionResponse>> createPayoutSession(
            @Valid @RequestBody CreatePayoutSessionRequest request
    ) {
        log.info("[INTERNAL API] Request to create payout session: {}", request);
        CreatePayoutSessionResponse response = paywayService.createPayoutSession(request, null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> checkHealth() {
        log.info("[INTERNAL API] Health check ping received");
        return ResponseEntity.ok(ApiResponse.success("UP"));
    }
}
