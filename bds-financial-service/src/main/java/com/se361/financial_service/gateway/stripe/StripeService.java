package com.se361.financial_service.gateway.stripe;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.model.Payout;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.PayoutCreateParams;
import com.stripe.net.RequestOptions;
import com.se361.financial_service.gateway.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class StripeService implements PaymentGatewayService {

    @Value("${stripe.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
        Stripe.setMaxNetworkRetries(3);
        log.info("Initialized Stripe SDK with configured API key and max retries=3");
    }

    private RequestOptions getOptions(String idempotencyKey) {
        RequestOptions.RequestOptionsBuilder builder = RequestOptions.builder().setApiKey(apiKey);
        if (StringUtils.hasText(idempotencyKey)) {
            builder.setIdempotencyKey(idempotencyKey);
        }
        return builder.build();
    }

    @Override
    public CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request) {
        return createPaymentSession(request, null);
    }

    @Override
    public CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request, String idempotencyKey) {
        validatePaymentRequest(request);

        String currency = request.getCurrency().toLowerCase();
        long unitAmount;
        if ("vnd".equals(currency)) {
            unitAmount = request.getAmount().longValue();
        } else {
            unitAmount = request.getAmount().multiply(new BigDecimal("100")).longValue();
        }

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.getReturnUrl())
                .setCancelUrl(request.getReturnUrl())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(unitAmount)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(request.getDescription() != null ? request.getDescription() : "Payment")
                                        .build())
                                .build())
                        .build());

        if (request.getMetadata() != null) {
            for (Map.Entry<String, Object> entry : request.getMetadata().entrySet()) {
                paramsBuilder.putMetadata(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        try {
            Session session = Session.create(paramsBuilder.build(), getOptions(idempotencyKey));
            log.info("Created Stripe checkout session: {}", session.getId());
            return mapPaymentSession(session, request);
        } catch (Exception e) {
            log.error("Failed to create Stripe checkout session", e);
            throw new RuntimeException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public CreatePaymentSessionResponse getPaymentSession(String paymentId) {
        if (!StringUtils.hasText(paymentId)) {
            throw new IllegalArgumentException("paymentId is required");
        }
        try {
            Session session = Session.retrieve(paymentId, getOptions(null));
            return mapPaymentSession(session, null);
        } catch (Exception e) {
            log.error("Failed to retrieve Stripe session {}", paymentId, e);
            throw new RuntimeException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public CreatePayoutSessionResponse createPayoutSession(CreatePayoutSessionRequest request, String idempotencyKey) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        String currency = request.getCurrency().toLowerCase();
        long unitAmount;
        if ("vnd".equals(currency)) {
            unitAmount = request.getAmount().longValue();
        } else {
            unitAmount = request.getAmount().multiply(new BigDecimal("100")).longValue();
        }

        PayoutCreateParams.Builder paramsBuilder = PayoutCreateParams.builder()
                .setAmount(unitAmount)
                .setCurrency(currency)
                .setDescription(request.getDescription());

        if (request.getMetadata() != null) {
            for (Map.Entry<String, Object> entry : request.getMetadata().entrySet()) {
                paramsBuilder.putMetadata(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        try {
            // Note: In sandboxes without connected bank accounts configured, this call will fail.
            // Under normal circumstances, this creates a Stripe payout.
            Payout payout = Payout.create(paramsBuilder.build(), getOptions(idempotencyKey));
            log.info("Created Stripe payout: {}", payout.getId());
            return mapPayoutSession(payout, request);
        } catch (Exception e) {
            log.error("Failed to create Stripe payout", e);
            throw new RuntimeException("Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public CreatePayoutSessionResponse getPayoutSession(String payoutId) {
        if (!StringUtils.hasText(payoutId)) {
            throw new IllegalArgumentException("payoutId is required");
        }
        try {
            Payout payout = Payout.retrieve(payoutId, getOptions(null));
            return mapPayoutSession(payout, null);
        } catch (Exception e) {
            log.error("Failed to retrieve Stripe payout {}", payoutId, e);
            throw new RuntimeException("Stripe error: " + e.getMessage(), e);
        }
    }

    private void validatePaymentRequest(CreatePaymentSessionRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("amount must be >= 0");
        if (!StringUtils.hasText(request.getCurrency()))
            throw new IllegalArgumentException("currency is required");
    }

    private CreatePaymentSessionResponse mapPaymentSession(Session session, CreatePaymentSessionRequest originalRequest) {
        BigDecimal amount = BigDecimal.valueOf(session.getAmountTotal() != null ? session.getAmountTotal() : 0);
        String currency = session.getCurrency() != null ? session.getCurrency().toUpperCase() : "VND";
        if (!"VND".equalsIgnoreCase(currency)) {
            amount = amount.divide(BigDecimal.valueOf(100));
        }

        Map<String, Object> metadata = new HashMap<>(session.getMetadata() != null ? session.getMetadata() : Map.of());

        return CreatePaymentSessionResponse.builder()
                .id(session.getId())
                .amount(amount)
                .currency(currency)
                .status(mapSessionStatus(session.getStatus()))
                .description(originalRequest != null ? originalRequest.getDescription() : null)
                .metadata(metadata)
                .returnUrl(session.getSuccessUrl())
                .webhookUrl(originalRequest != null ? originalRequest.getWebhookUrl() : null)
                .checkoutUrl(session.getUrl())
                .createdAt(OffsetDateTime.now()) // Stripe Checkout sessions do not expose instant createdAt in direct getters
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private CreatePayoutSessionResponse mapPayoutSession(Payout payout, CreatePayoutSessionRequest originalRequest) {
        BigDecimal amount = BigDecimal.valueOf(payout.getAmount() != null ? payout.getAmount() : 0);
        String currency = payout.getCurrency() != null ? payout.getCurrency().toUpperCase() : "VND";
        if (!"VND".equalsIgnoreCase(currency)) {
            amount = amount.divide(BigDecimal.valueOf(100));
        }

        Map<String, Object> metadata = new HashMap<>(payout.getMetadata() != null ? payout.getMetadata() : Map.of());

        return CreatePayoutSessionResponse.builder()
                .id(payout.getId())
                .amount(amount)
                .currency(currency)
                .status(payout.getStatus().toUpperCase())
                .accountNumber(originalRequest != null ? originalRequest.getAccountNumber() : null)
                .accountHolderName(originalRequest != null ? originalRequest.getAccountHolderName() : null)
                .swiftCode(originalRequest != null ? originalRequest.getSwiftCode() : null)
                .description(payout.getDescription())
                .metadata(metadata)
                .webhookUrl(originalRequest != null ? originalRequest.getWebhookUrl() : null)
                .createdAt(OffsetDateTime.ofInstant(Instant.ofEpochSecond(payout.getCreated()), ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.ofInstant(Instant.ofEpochSecond(payout.getCreated()), ZoneOffset.UTC))
                .build();
    }

    private String mapSessionStatus(String stripeStatus) {
        if ("complete".equalsIgnoreCase(stripeStatus)) return "SUCCESS";
        if ("open".equalsIgnoreCase(stripeStatus)) return "PENDING";
        if ("expired".equalsIgnoreCase(stripeStatus)) return "EXPIRED";
        return stripeStatus;
    }
}
