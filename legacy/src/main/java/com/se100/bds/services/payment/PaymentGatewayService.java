package com.se100.bds.services.payment;

import com.se100.bds.services.payment.dto.CreatePaymentSessionRequest;
import com.se100.bds.services.payment.dto.CreatePaymentSessionResponse;
import com.se100.bds.services.payment.dto.CreatePayoutSessionRequest;
import com.se100.bds.services.payment.dto.CreatePayoutSessionResponse;
import com.se100.bds.services.payment.dto.PayoutSessionResponse;

public interface PaymentGatewayService {
    CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request);

    CreatePaymentSessionResponse createPaymentSession(CreatePaymentSessionRequest request, String idempotencyKey);

    CreatePaymentSessionResponse getPaymentSession(String paymentId);

    CreatePayoutSessionResponse createPayoutSession(CreatePayoutSessionRequest request, String idempotencyKey);

    PayoutSessionResponse getPayoutSession(String payoutId);
}
