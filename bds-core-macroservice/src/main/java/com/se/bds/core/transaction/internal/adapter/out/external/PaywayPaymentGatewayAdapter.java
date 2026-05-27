package com.se.bds.core.transaction.internal.adapter.out.external;

import com.se.bds.core.transaction.internal.application.port.out.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class PaywayPaymentGatewayAdapter implements PaymentGatewayPort {

    private final String apiKey;
    private final String merchantId;
    private final String serviceUrl;

    public PaywayPaymentGatewayAdapter(
            @Value("${payway.api-key:default-key}") String apiKey,
            @Value("${payway.merchant-id:default-merchant}") String merchantId,
            @Value("${payway.service-url:http://localhost:3000}") String serviceUrl) {
        this.apiKey = apiKey;
        this.merchantId = merchantId;
        this.serviceUrl = serviceUrl;
    }

    @Override
    public boolean isHealthy() {
        log.info("[EVENT] Checking Payway gateway health status at service-url={}", serviceUrl);
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofMillis(2000))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(serviceUrl + "/api/health"))
                    .timeout(java.time.Duration.ofMillis(2000))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            boolean healthy = response.statusCode() == 200;
            log.info("[EVENT] Payway gateway health status response: statusCode={}, healthy={}", response.statusCode(), healthy);
            return healthy;
        } catch (Exception e) {
            log.warn("[EVENT] Payway gateway health check failed: {}", e.getMessage());
            // Fallback for development/testing when external sandbox is not running:
            if ("default-key".equals(apiKey)) {
                log.info("[EVENT] Payway fallback mock triggered: treating health check as healthy in sandbox/default mode");
                return true;
            }
            return false;
        }
    }

    @Override
    @org.springframework.retry.annotation.Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @org.springframework.retry.annotation.Backoff(delay = 500, multiplier = 2.0)
    )
    public PaymentSessionResult createPaymentSession(
            BigDecimal amount,
            String currency,
            String description,
            Map<String, Object> metadata,
            String idempotencyKey) {
        
        log.info("[EVENT] Creating Payway payment session: amount={}, currency={}, description={}", amount, currency, description);
        
        // TODO: integrate with Payway sandbox for end-to-end payment flow
        
        String gatewayPaymentId = "payway_" + UUID.randomUUID().toString().substring(0, 8);
        String mockCheckoutUrl = "https://checkout.payway.com.vn/pay?session_id=" + gatewayPaymentId;

        log.info("[EVENT] Payment session created: paymentId={}, checkoutUrl={}", gatewayPaymentId, mockCheckoutUrl);

        return new PaymentSessionResult(
                gatewayPaymentId,
                mockCheckoutUrl,
                "PENDING",
                amount,
                currency
        );
    }

    @Override
    public PaymentSessionResult getPaymentSession(String gatewayPaymentId) {
        log.info("[EVENT] Fetching Payway payment session: {}", gatewayPaymentId);
        
        // TODO: integrate with Payway sandbox for end-to-end payment flow
        
        return new PaymentSessionResult(
                gatewayPaymentId,
                "https://checkout.payway.com.vn/pay?session_id=" + gatewayPaymentId,
                "SUCCESS",
                BigDecimal.ZERO,
                "VND"
        );
    }

    @Override
    public PayoutResult createPayoutSession(
            BigDecimal amount,
            String currency,
            String accountNumber,
            String accountHolderName,
            String swiftCode,
            String description,
            Map<String, Object> metadata,
            String idempotencyKey) {
        
        log.info("[EVENT] Triggering Payway payout: amount={}, bankAccount={}, holder={}", amount, accountNumber, accountHolderName);
        
        // TODO: integrate with Payway sandbox for end-to-end payment flow
        
        String gatewayPayoutId = "payout_" + UUID.randomUUID().toString().substring(0, 8);
        return new PayoutResult(
                gatewayPayoutId,
                "SUCCESS",
                amount,
                currency
        );
    }
}
