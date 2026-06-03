package com.se361.financial_service.gateway.paypal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.event.PaymentCompletedEvent;
import com.se361.financial_service.gateway.CreatePaymentSessionRequest;
import com.se361.financial_service.gateway.CreatePaymentSessionResponse;
import com.se361.financial_service.gateway.paypal.dto.PayPalCreateOrderRequest;
import com.se361.financial_service.gateway.paypal.dto.PayPalOrderResponse;
import com.se361.financial_service.gateway.paypal.dto.PayPalTokenResponse;
import com.se361.financial_service.gateway.paypal.dto.PayPalWebhookEvent;
import com.se361.financial_service.repositories.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
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

    @Value("${paypal.webhook-id:default-webhook-id}")
    private String webhookId;

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Token cache
    private volatile String cachedToken;
    private volatile long tokenExpiresAt = 0;

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
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
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

    @CircuitBreaker(name = "paypalCircuitBreaker")
    public CreatePaymentSessionResponse createOrder(CreatePaymentSessionRequest request) {
        String token = getAccessToken();

        // Convert VND to USD if needed (PayPal doesn't support VND)
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

    @CircuitBreaker(name = "paypalCircuitBreaker")
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

    @CircuitBreaker(name = "paypalCircuitBreaker")
    public boolean verifySignature(
            String authAlgo,
            String certUrl,
            String transmissionId,
            String transmissionSig,
            String transmissionTime,
            String rawBody
    ) {
        try {
            String token = getAccessToken();
            Map<String, Object> requestBody = Map.of(
                    "auth_algo", authAlgo,
                    "cert_url", certUrl,
                    "transmission_id", transmissionId,
                    "transmission_sig", transmissionSig,
                    "transmission_time", transmissionTime,
                    "webhook_id", webhookId,
                    "webhook_event", objectMapper.readTree(rawBody)
            );

            ResponseEntity<Map> response = client()
                    .post()
                    .uri("/v1/notifications/verify-webhook-signature")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(requestBody)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String verificationStatus = String.valueOf(response.getBody().get("verification_status"));
                return "SUCCESS".equalsIgnoreCase(verificationStatus);
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to verify PayPal webhook signature", e);
            return false;
        }
    }

    @Transactional
    public void handleWebhookEvent(PayPalWebhookEvent event) {
        if (event == null || event.getEventType() == null) return;

        log.info("PayPal webhook event: {}", event.getEventType());

        switch (event.getEventType()) {
            case "PAYMENT.CAPTURE.COMPLETED" -> {
                String orderId = extractOrderId(event);
                log.info("PayPal payment captured for order {}", orderId);
                if (orderId != null) {
                    paymentRepository.findByPayosPaymentId(orderId).ifPresentOrElse(payment -> {
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setPaidTime(LocalDateTime.now());
                        paymentRepository.save(payment);
                        log.info("PayPal payment {} marked SUCCESS via webhook", payment.getId());
                        publishPaymentCompleted(payment);
                    }, () -> log.warn("PayPal webhook: payment not found for orderId {}", orderId));
                }
            }
            case "PAYMENT.CAPTURE.DENIED", "CHECKOUT.ORDER.CANCELLED" -> {
                String orderId = extractOrderId(event);
                log.warn("PayPal payment failed/cancelled for order {}", orderId);
                if (orderId != null) {
                    paymentRepository.findByPayosPaymentId(orderId).ifPresentOrElse(payment -> {
                        if (payment.getStatus() != PaymentStatus.SUCCESS) {
                            payment.setStatus(PaymentStatus.CANCELLED);
                            paymentRepository.save(payment);
                            log.info("PayPal payment {} marked CANCELLED via webhook", payment.getId());
                        }
                    }, () -> log.warn("PayPal webhook: payment not found for orderId {}", orderId));
                }
            }
            default -> log.info("PayPal webhook: ignoring event type {}", event.getEventType());
        }
    }

    private void publishPaymentCompleted(com.se361.financial_service.entities.Payment payment) {
        try {
            PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                    payment.getId(),
                    payment.getContractId(),
                    payment.getPropertyId(),
                    payment.getPaymentType().name(),
                    payment.getAmount(),
                    payment.getPayerId(),
                    Instant.now()
            );
            String payload = objectMapper.writeValueAsString(completedEvent);
            log.info("[Kafka] Publishing PaymentCompletedEvent to topic=payment-succeeded: {}", payload);
            kafkaTemplate.send("payment-succeeded", payment.getId().toString(), payload);
        } catch (Exception e) {
            log.error("[Kafka] Failed to publish PaymentCompletedEvent for payment={}", payment.getId(), e);
        }
    }

    private String extractOrderId(PayPalWebhookEvent event) {
        if (event.getResource() == null) return null;
        Object id = event.getResource().get("id");
        return id != null ? id.toString() : null;
    }
}
