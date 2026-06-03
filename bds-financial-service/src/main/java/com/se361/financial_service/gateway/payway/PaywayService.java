package com.se361.financial_service.gateway.payway;

import com.se361.financial_service.gateway.*;
import com.se361.financial_service.gateway.payway.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class PaywayService implements PaymentGatewayService {

    private static final String WEBHOOK_ROUTE = "/webhooks/payway";

    @Value("${payway.service-url:http://localhost:3000}")
    private String serviceUrl;

    @Value("${payway.api-key}")
    private String apiKey;

    @Value("${payway.webhook-base-url}")
    private String webhookBaseUrl;

    @Value("${payway.return-url}")
    private String returnUrl;

    private volatile RestClient restClient;

    private RestClient client() {
        RestClient c = restClient;
        if (c != null) return c;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(5));

        restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    String body = safeReadBody(response);
                    throwPaywayError(response.getStatusCode(), body);
                })
                .build();

        return restClient;
    }

    @Override
    @CircuitBreaker(name = "paywayCircuitBreaker")
    public CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request) {
        return createPaymentSession(request, null);
    }

    @Override
    @CircuitBreaker(name = "paywayCircuitBreaker")
    public CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request, String idempotencyKey) {
        validatePaymentRequest(request);

        if (!StringUtils.hasText(request.getReturnUrl())) {
            request.setReturnUrl(returnUrl);
        }
        if (!StringUtils.hasText(request.getWebhookUrl()) && StringUtils.hasText(webhookBaseUrl)) {
            request.setWebhookUrl(normalizeBaseUrl(webhookBaseUrl) + WEBHOOK_ROUTE);
        }

        PaywayCreatePaymentRequest paywayRequest = PaywayCreatePaymentRequest.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .returnUrl(request.getReturnUrl())
                .webhookUrl(request.getWebhookUrl())
                .build();

        RestClient.RequestBodySpec spec = client()
                .post()
                .uri("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        if (StringUtils.hasText(idempotencyKey)) {
            spec = spec.header("Idempotency-Key", idempotencyKey);
        }

        PaywayPaymentResponse resp = spec
                .body(paywayRequest)
                .retrieve()
                .body(PaywayPaymentResponse.class);

        if (resp == null) throw new IllegalStateException("Payway returned empty response");

        return mapPayment(resp);
    }

    @Override
    @CircuitBreaker(name = "paywayCircuitBreaker")
    public CreatePaymentSessionResponse getPaymentSession(String paymentId) {
        if (!StringUtils.hasText(paymentId)) throw new IllegalArgumentException("paymentId is required");

        PaywayPaymentWithOriginatingAccountResponse resp = client()
                .get()
                .uri("/api/payments/{id}", paymentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .body(PaywayPaymentWithOriginatingAccountResponse.class);

        if (resp == null) throw new IllegalStateException("Payway returned empty response");

        return mapPayment(resp);
    }

    @Override
    @CircuitBreaker(name = "paywayCircuitBreaker")
    public CreatePayoutSessionResponse createPayoutSession(CreatePayoutSessionRequest request, String idempotencyKey) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (!StringUtils.hasText(request.getWebhookUrl()) && StringUtils.hasText(webhookBaseUrl)) {
            request.setWebhookUrl(normalizeBaseUrl(webhookBaseUrl) + WEBHOOK_ROUTE);
        }

        PaywayCreatePayoutRequest paywayRequest = PaywayCreatePayoutRequest.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .accountNumber(request.getAccountNumber())
                .accountHolderName(request.getAccountHolderName())
                .swiftCode(request.getSwiftCode())
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .webhookUrl(request.getWebhookUrl())
                .build();

        RestClient.RequestBodySpec spec = client()
                .post()
                .uri("/api/payouts")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        if (StringUtils.hasText(idempotencyKey)) {
            spec = spec.header("Idempotency-Key", idempotencyKey);
        }

        PaywayPayoutResponse resp = spec
                .body(paywayRequest)
                .retrieve()
                .body(PaywayPayoutResponse.class);

        if (resp == null) throw new IllegalStateException("Payway returned empty response");

        return mapPayout(resp);
    }

    @Override
    @CircuitBreaker(name = "paywayCircuitBreaker")
    public CreatePayoutSessionResponse getPayoutSession(String payoutId) {
        if (!StringUtils.hasText(payoutId)) throw new IllegalArgumentException("payoutId is required");

        PaywayPayoutResponse resp = client()
                .get()
                .uri("/api/payouts/{id}", payoutId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .body(PaywayPayoutResponse.class);

        if (resp == null) throw new IllegalStateException("Payway returned empty response");

        return mapPayout(resp);
    }


    private void validatePaymentRequest(CreatePaymentSessionRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("amount must be >= 0");
        if (!StringUtils.hasText(request.getCurrency()))
            throw new IllegalArgumentException("currency is required");
    }

    private static CreatePaymentSessionResponse mapPayment(PaywayPaymentResponse r) {
        return CreatePaymentSessionResponse.builder()
                .id(r.getId()).amount(r.getAmount()).currency(r.getCurrency())
                .status(r.getStatus()).description(r.getDescription())
                .metadata(r.getMetadata()).returnUrl(r.getReturnUrl())
                .webhookUrl(r.getWebhookUrl()).checkoutUrl(r.getCheckoutUrl())
                .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
                .build();
    }

    private static CreatePaymentSessionResponse mapPayment(PaywayPaymentWithOriginatingAccountResponse r) {
        return CreatePaymentSessionResponse.builder()
                .id(r.getId()).amount(r.getAmount()).currency(r.getCurrency())
                .status(r.getStatus()).description(r.getDescription())
                .metadata(r.getMetadata()).returnUrl(r.getReturnUrl())
                .webhookUrl(r.getWebhookUrl()).checkoutUrl(r.getCheckoutUrl())
                .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
                .build();
    }

    private static CreatePayoutSessionResponse mapPayout(PaywayPayoutResponse r) {
        return CreatePayoutSessionResponse.builder()
                .id(r.getId()).amount(r.getAmount()).currency(r.getCurrency())
                .status(r.getStatus()).accountNumber(r.getAccountNumber())
                .accountHolderName(r.getAccountHolderName()).swiftCode(r.getSwiftCode())
                .description(r.getDescription()).metadata(r.getMetadata())
                .webhookUrl(r.getWebhookUrl()).createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private static String safeReadBody(ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static void throwPaywayError(HttpStatusCode status, String body) {
        int code = status.value();
        String msg = "Payway returned HTTP " + code + ": " + (body != null ? body : "");
        throw new RuntimeException(msg);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) return baseUrl;
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
