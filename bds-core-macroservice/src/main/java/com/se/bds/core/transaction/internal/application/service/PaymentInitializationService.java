package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import com.se.bds.common.message.validation.MSG18;
import com.se.bds.core.transaction.internal.application.port.in.PaymentInitializationUseCase;
import com.se.bds.core.transaction.internal.application.port.out.PaymentGatewayPort;
import com.se.bds.core.transaction.internal.application.port.out.PaymentRepository;
import com.se.bds.core.transaction.internal.domain.model.Payment;
import com.se.bds.core.transaction.internal.domain.model.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentInitializationService implements PaymentInitializationUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort paymentGatewayPort;

    @Override
    @Transactional
    public PaymentInitResult initializePayment(UUID contractId, UUID paymentId) {
        log.info("[EVENT] Initializing payment session: contractId={}, paymentId={}", contractId, paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));

        if (!payment.getContract().getId().equals(contractId)) {
            throw new BusinessException(MSG12.CODE, "Payment does not belong to contract: " + contractId);
        }

        // Prepare metadata for gateway mapping
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contractId", contractId.toString());
        metadata.put("paymentId", paymentId.toString());
        metadata.put("payerUserId", payment.getPayerUserId() != null ? payment.getPayerUserId().toString() : "");

        // Call the payment gateway adapter
        PaymentGatewayPort.PaymentSessionResult sessionResult = paymentGatewayPort.createPaymentSession(
                payment.getAmount(),
                "VND",
                "Payment for contract " + contractId + " (Type: " + payment.getPaymentType() + ")",
                metadata,
                paymentId.toString()
        );

        // Update payment record
        payment.setPaywayPaymentId(sessionResult.gatewayPaymentId());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        log.info("[EVENT] Payment session updated in db: paymentId={}, paywayPaymentId={}", paymentId, sessionResult.gatewayPaymentId());

        return new PaymentInitResult(
                payment.getId(),
                sessionResult.gatewayPaymentId(),
                sessionResult.checkoutUrl(),
                sessionResult.status()
        );
    }
}
