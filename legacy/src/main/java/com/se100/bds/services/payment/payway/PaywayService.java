package com.se100.bds.services.payment.payway;

import com.se100.bds.services.payment.PaymentGatewayService;
import com.se100.bds.services.payment.dto.CreatePaymentSessionRequest;
import com.se100.bds.services.payment.dto.CreatePaymentSessionResponse;
import com.se100.bds.services.payment.dto.CreatePayoutSessionRequest;
import com.se100.bds.services.payment.dto.CreatePayoutSessionResponse;
import com.se100.bds.services.payment.dto.PayoutSessionResponse;
import com.se100.bds.services.payment.payway.dto.PaywayCreatePaymentRequest;
import com.se100.bds.services.payment.payway.dto.PaywayCreatePayoutRequest;
import com.se100.bds.services.payment.payway.dto.PaywayPaymentResponse;
import com.se100.bds.services.payment.payway.dto.PaywayPaymentWithOriginatingAccountResponse;
import com.se100.bds.services.payment.payway.dto.PaywayPayoutResponse;
import com.se100.bds.services.payment.payway.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PaywayService implements PaymentGatewayService {
    private static final String WEBHOOK_ROUTE = "/payway";

    @Value("${payway.service-url:http://localhost:3000}")
    private String serviceUrl;

    @Value("${payway.api-key}")
    private String apiKey;

    @Value("${payway.webhook-base-url}")
    private String webhookBaseUrl;

    @Value("${payway.return-url}")
    private String returnUrl;

    private volatile RestClient restClient;

    private static String safeReadBody(ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String summarizePaywayError(int status, String body) {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) return "Payway returned HTTP " + status;
        // Keep it short-ish so it doesn't spam logs, but still useful.
        if (trimmed.length() > 500) trimmed = trimmed.substring(0, 500) + "...";
        return "Payway returned HTTP " + status + ": " + trimmed;
    }

    private static void throwPayway(HttpStatusCode status, String body) {
        int code = status.value();
        String message = summarizePaywayError(code, body);

        if (code == 400) {
            throw new PaywayBadRequestException(message, body);
        }
        if (code == 401 || code == 403) {
            throw new PaywayUnauthorizedException(code, message, body);
        }
        if (code == 404) {
            throw new PaywayNotFoundException(message, body);
        }
        if (code == 422) {
            throw new PaywayUnprocessableEntityException(message, body);
        }
        if (code >= 500) {
            throw new PaywayServerException(code, message, body);
        }

        throw new PaywayApiException(code, message, body);
    }

    private RestClient client() {
        // Lazy-init so we don't need extra @Configuration right now.
        RestClient c = restClient;
        if (c != null) return c;

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        RestClient created = RestClient.builder()
                .baseUrl(serviceUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    String body = safeReadBody(response);
                    throwPayway(response.getStatusCode(), body);
                })
                .build();

        restClient = created;
        return created;
    }

    @Override
    public CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request) {
        return createPaymentSession(request, null);
    }

    @Override
    public CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request, String idempotencyKey) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        if (!StringUtils.hasText(request.getCurrency())) {
            throw new IllegalArgumentException("currency is required");
        }

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
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        if (StringUtils.hasText(idempotencyKey)) {
            spec = spec.header("Idempotency-Key", idempotencyKey);
        }

        PaywayPaymentResponse paywayResp = spec
                .body(paywayRequest)
                .retrieve()
                .body(PaywayPaymentResponse.class);

        if (paywayResp == null) {
            throw new IllegalStateException("Payway returned empty response");
        }

        return mapPayment(paywayResp);
    }

    @Override
    public CreatePaymentSessionResponse getPaymentSession(String paymentId) {
        if (!StringUtils.hasText(paymentId)) {
            throw new IllegalArgumentException("paymentId is required");
        }

        // New Payway contract returns PaymentWithOriginatingAccount.
        PaywayPaymentWithOriginatingAccountResponse paywayResp = client()
                .get()
                .uri("/api/payments/{id}", paymentId)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .body(PaywayPaymentWithOriginatingAccountResponse.class);

        if (paywayResp == null) {
            throw new IllegalStateException("Payway returned empty response");
        }

        return mapPayment(paywayResp);
    }

    @Override
    public CreatePayoutSessionResponse createPayoutSession(CreatePayoutSessionRequest request, String idempotencyKey) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        if (!StringUtils.hasText(request.getCurrency())) {
            throw new IllegalArgumentException("currency is required");
        }

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
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        if (StringUtils.hasText(idempotencyKey)) {
            spec = spec.header("Idempotency-Key", idempotencyKey);
        }

        PaywayPayoutResponse paywayResp = spec
                .body(paywayRequest)
                .retrieve()
                .body(PaywayPayoutResponse.class);

        if (paywayResp == null) {
            throw new IllegalStateException("Payway returned empty response");
        }

        return mapPayoutCreate(paywayResp);
    }

    @Override
    public PayoutSessionResponse getPayoutSession(String payoutId) {
        if (!StringUtils.hasText(payoutId)) {
            throw new IllegalArgumentException("payoutId is required");
        }

        PaywayPayoutResponse paywayResp = client()
                .get()
                .uri("/api/payouts/{id}", payoutId)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .body(PaywayPayoutResponse.class);

        if (paywayResp == null) {
            throw new IllegalStateException("Payway returned empty response");
        }

        return mapPayout(paywayResp);
    }

    private static CreatePaymentSessionResponse mapPayment(PaywayPaymentResponse paywayResp) {
        return CreatePaymentSessionResponse.builder()
                .id(paywayResp.getId())
                .amount(paywayResp.getAmount())
                .currency(paywayResp.getCurrency())
                .status(paywayResp.getStatus())
                .description(paywayResp.getDescription())
                .metadata(paywayResp.getMetadata())
                .returnUrl(paywayResp.getReturnUrl())
                .webhookUrl(paywayResp.getWebhookUrl())
                .checkoutUrl(paywayResp.getCheckoutUrl())
                .createdAt(paywayResp.getCreatedAt())
                .updatedAt(paywayResp.getUpdatedAt())
                .build();
    }

    private static CreatePaymentSessionResponse mapPayment(PaywayPaymentWithOriginatingAccountResponse paywayResp) {
        return CreatePaymentSessionResponse.builder()
                .id(paywayResp.getId())
                .amount(paywayResp.getAmount())
                .currency(paywayResp.getCurrency())
                .status(paywayResp.getStatus())
                .description(paywayResp.getDescription())
                .metadata(paywayResp.getMetadata())
                .returnUrl(paywayResp.getReturnUrl())
                .webhookUrl(paywayResp.getWebhookUrl())
                .checkoutUrl(paywayResp.getCheckoutUrl())
                .createdAt(paywayResp.getCreatedAt())
                .updatedAt(paywayResp.getUpdatedAt())
                .build();
    }

    private static CreatePayoutSessionResponse mapPayoutCreate(PaywayPayoutResponse paywayResp) {
        return CreatePayoutSessionResponse.builder()
                .id(paywayResp.getId())
                .amount(paywayResp.getAmount())
                .currency(paywayResp.getCurrency())
                .status(paywayResp.getStatus())
                .accountNumber(paywayResp.getAccountNumber())
                .accountHolderName(paywayResp.getAccountHolderName())
                .swiftCode(paywayResp.getSwiftCode())
                .description(paywayResp.getDescription())
                .metadata(paywayResp.getMetadata())
                .webhookUrl(paywayResp.getWebhookUrl())
                .createdAt(paywayResp.getCreatedAt())
                .updatedAt(paywayResp.getUpdatedAt())
                .build();
    }

    private static PayoutSessionResponse mapPayout(PaywayPayoutResponse paywayResp) {
        return PayoutSessionResponse.builder()
                .id(paywayResp.getId())
                .amount(paywayResp.getAmount())
                .currency(paywayResp.getCurrency())
                .status(paywayResp.getStatus())
                .accountNumber(paywayResp.getAccountNumber())
                .accountHolderName(paywayResp.getAccountHolderName())
                .swiftCode(paywayResp.getSwiftCode())
                .description(paywayResp.getDescription())
                .metadata(paywayResp.getMetadata())
                .webhookUrl(paywayResp.getWebhookUrl())
                .createdAt(paywayResp.getCreatedAt())
                .updatedAt(paywayResp.getUpdatedAt())
                .build();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) return baseUrl;
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
