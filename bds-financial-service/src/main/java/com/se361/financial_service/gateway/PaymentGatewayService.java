package com.se361.financial_service.gateway;

public interface PaymentGatewayService {
    CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request);
    CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request, String idempotencyKey);
    CreatePaymentSessionResponse getPaymentSession(String paymentId);
    CreatePayoutSessionResponse createPayoutSession(CreatePayoutSessionRequest request, String idempotencyKey);
    CreatePayoutSessionResponse getPayoutSession(String payoutId);
}
