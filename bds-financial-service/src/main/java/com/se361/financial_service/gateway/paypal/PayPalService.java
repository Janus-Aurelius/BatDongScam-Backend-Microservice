package com.se361.financial_service.gateway.paypal;

import com.se361.financial_service.gateway.CreatePaymentSessionRequest;
import com.se361.financial_service.gateway.CreatePaymentSessionResponse;
import com.se361.financial_service.gateway.paypal.dto.PayPalCreateOrderRequest;
import com.se361.financial_service.gateway.paypal.dto.PayPalOrderResponse;
import com.se361.financial_service.gateway.paypal.dto.PayPalTokenResponse;
import com.se361.financial_service.gateway.paypal.dto.PayPalWebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PayPalService {

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.base-url:https://api-m.sandbox.paypal.com}")
    private String baseUrl;

    @Value("${paypal.return-url}")
    private String returnUrl;

    @Value("${paypal.cancel-url}")
    private String cancelUrl;

    @Value("${paypal.brand-name:BDS Platform}")
    private String brandName;

    // Token cache
    private volatile String cachedToken;
    private volatile long tokenExpiresAt = 0;

    private volatile RestClient restClient;

    private RestClient client() {
        RestClient c = restClient;
        if (c != null) return c;

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        return restClient;
    }

    private String getAccessToken() {
        if (cachedToken != null && Instant.now().getEpochSecond() < tokenExpiresAt - 60) {
            return cachedToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        PayPalTokenResponse tokenResponse = client()
                .post()
                .uri("/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(h -> h.setBasicAuth(clientId, clientSecret))
                .body(form)
                .retrieve()
                .body(PayPalTokenResponse.class);

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new IllegalStateException("Failed to get PayPal access token");
        }

        cachedToken = tokenResponse.getAccessToken();
        tokenExpiresAt = Instant.now().getEpochSecond() + tokenResponse.getExpiresIn();
        return cachedToken;
    }

    public CreatePaymentSessionResponse createOrder(CreatePaymentSessionRequest request) {
        String token = getAccessToken();

        // Convert VND to USD if needed (PayPal doesn't support VND)
        // For simplicity: treat amount as USD cents / 100
        String amountValue = request.getAmount()
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();

        String customId = request.getMetadata() != null
                ? String.valueOf(request.getMetadata().getOrDefault("paymentId", ""))
                : "";

        PayPalCreateOrderRequest orderRequest = PayPalCreateOrderRequest.builder()
                .intent("CAPTURE")
                .purchaseUnits(List.of(
                        PayPalCreateOrderRequest.PurchaseUnit.builder()
                                .amount(PayPalCreateOrderRequest.Amount.builder()
                                        .currencyCode("USD")
                                        .value(amountValue)
                                        .build())
                                .description(request.getDescription())
                                .customId(customId)
                                .build()
                ))
                .applicationContext(PayPalCreateOrderRequest.ApplicationContext.builder()
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .brandName(brandName)
                        .build())
                .build();

        PayPalOrderResponse orderResponse = client()
                .post()
                .uri("/v2/checkout/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(orderRequest)
                .retrieve()
                .body(PayPalOrderResponse.class);

        if (orderResponse == null) {
            throw new IllegalStateException("PayPal returned empty response");
        }

        log.info("Created PayPal order {}", orderResponse.getId());

        return CreatePaymentSessionResponse.builder()
                .id(orderResponse.getId())
                .amount(request.getAmount())
                .currency("USD")
                .status(orderResponse.getStatus())
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .checkoutUrl(orderResponse.getApproveUrl())
                .build();
    }

    public CreatePaymentSessionResponse captureOrder(String orderId) {
        String token = getAccessToken();

        PayPalOrderResponse orderResponse = client()
                .post()
                .uri("/v2/checkout/orders/{id}/capture", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(PayPalOrderResponse.class);

        if (orderResponse == null) {
            throw new IllegalStateException("PayPal capture returned empty response");
        }

        log.info("Captured PayPal order {} -> status {}", orderId, orderResponse.getStatus());

        return CreatePaymentSessionResponse.builder()
                .id(orderResponse.getId())
                .status(orderResponse.getStatus())
                .build();
    }

    public void handleWebhookEvent(PayPalWebhookEvent event) {
        if (event == null || event.getEventType() == null) return;

        log.info("PayPal webhook event: {}", event.getEventType());

        switch (event.getEventType()) {
            case "PAYMENT.CAPTURE.COMPLETED" -> {
                String orderId = extractOrderId(event);
                log.info("PayPal payment captured for order {}", orderId);
                // TODO: find payment by paypal order id and mark SUCCESS
            }
            case "PAYMENT.CAPTURE.DENIED", "CHECKOUT.ORDER.CANCELLED" -> {
                String orderId = extractOrderId(event);
                log.warn("PayPal payment failed/cancelled for order {}", orderId);
                // TODO: find payment by paypal order id and mark CANCELLED
            }
            default -> log.info("PayPal webhook: ignoring event type {}", event.getEventType());
        }
    }

    private String extractOrderId(PayPalWebhookEvent event) {
        if (event.getResource() == null) return null;
        Object id = event.getResource().get("id");
        return id != null ? id.toString() : null;
    }
}
