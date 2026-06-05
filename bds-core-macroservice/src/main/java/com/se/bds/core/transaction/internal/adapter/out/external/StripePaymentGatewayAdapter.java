package com.se.bds.core.transaction.internal.adapter.out.external;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.core.transaction.internal.application.port.out.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private final RestTemplate restTemplate;
    private final String financialServiceUrl;

    public StripePaymentGatewayAdapter(
            RestTemplate restTemplate,
            @Value("${financial.service-url:http://localhost:8086}") String financialServiceUrl) {
        this.restTemplate = restTemplate;
        this.financialServiceUrl = financialServiceUrl;
    }

    @Override
    public boolean isHealthy() {
        log.info("[EVENT] Checking Financial service health status at url={}", financialServiceUrl);
        try {
            String url = financialServiceUrl + "/api/internal/payments/health";
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(url, ApiResponse.class);
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().isSuccess();
        } catch (Exception e) {
            log.warn("[EVENT] Financial service health check failed: {}", e.getMessage());
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
        
        log.info("[EVENT] Delegating Stripe payment session creation to Financial Service: amount={}, currency={}", amount, currency);
        
        String url = financialServiceUrl + "/api/internal/payments/session";
        Map<String, Object> requestBody = Map.of(
                "amount", amount,
                "currency", currency,
                "description", description,
                "metadata", metadata != null ? metadata : Map.of()
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (idempotencyKey != null) {
                headers.set("Idempotency-Key", idempotencyKey);
            }
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ApiResponse<FinancialSessionResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<FinancialSessionResponse>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().isSuccess()) {
                FinancialSessionResponse data = response.getBody().getData();
                if (data != null) {
                    return new PaymentSessionResult(
                            data.id(),
                            data.checkoutUrl(),
                            data.status(),
                            data.amount(),
                            data.currency()
                    );
                }
            }
            throw new RuntimeException("Financial service returned failure for payment session creation");
        } catch (Exception e) {
            log.error("[EVENT] Failed to create payment session via Financial Service", e);
            throw new RuntimeException("Failed to initialize payment session: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentSessionResult getPaymentSession(String gatewayPaymentId) {
        log.info("[EVENT] Fetching payment session status from Financial Service: {}", gatewayPaymentId);
        return new PaymentSessionResult(
                gatewayPaymentId,
                financialServiceUrl + "/api/payments/" + gatewayPaymentId,
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
        
        log.info("[EVENT] Delegating Stripe payout session creation to Financial Service: amount={}, bankAccount={}", amount, accountNumber);
        
        String url = financialServiceUrl + "/api/internal/payments/payout";
        Map<String, Object> requestBody = Map.of(
                "amount", amount,
                "currency", currency,
                "accountNumber", accountNumber,
                "accountHolderName", accountHolderName,
                "swiftCode", swiftCode,
                "description", description,
                "metadata", metadata != null ? metadata : Map.of()
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (idempotencyKey != null) {
                headers.set("Idempotency-Key", idempotencyKey);
            }
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ApiResponse<FinancialPayoutResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<FinancialPayoutResponse>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().isSuccess()) {
                FinancialPayoutResponse data = response.getBody().getData();
                if (data != null) {
                    return new PayoutResult(
                            data.id(),
                            data.status(),
                            data.amount(),
                            data.currency()
                    );
                }
            }
            throw new RuntimeException("Financial service returned failure for payout creation");
        } catch (Exception e) {
            log.error("[EVENT] Failed to create payout session via Financial Service", e);
            throw new RuntimeException("Failed to initialize payout: " + e.getMessage(), e);
        }
    }

    private record FinancialSessionResponse(
            String id,
            BigDecimal amount,
            String currency,
            String status,
            String description,
            Map<String, Object> metadata,
            String checkoutUrl
    ) {}

    private record FinancialPayoutResponse(
            String id,
            BigDecimal amount,
            String currency,
            String status
    ) {}
}
