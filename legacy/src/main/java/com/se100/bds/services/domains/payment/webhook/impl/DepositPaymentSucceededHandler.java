package com.se100.bds.services.domains.payment.webhook.impl;

import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.services.domains.contract.DepositContractService;
import com.se100.bds.services.domains.payment.webhook.PaymentGatewayWebhookEvent;
import com.se100.bds.services.domains.payment.webhook.PaymentSucceededSideEffectHandler;
import com.se100.bds.utils.Constants.PaymentTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Side effects for DEPOSIT payments.
 * When a deposit payment succeeds, check if the contract should transition to ACTIVE.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepositPaymentSucceededHandler implements PaymentSucceededSideEffectHandler {

    private final DepositContractService depositContractService;

    @Override
    public boolean supports(Payment payment) {
        return payment != null && payment.getPaymentType() == PaymentTypeEnum.DEPOSIT;
    }

    @Override
    public void handle(Payment payment, PaymentGatewayWebhookEvent event) {
        if (payment.getContract() == null) {
            log.warn("DEPOSIT payment {} has no contract associated", payment.getId());
            return;
        }

        var contractId = payment.getContract().getId();

        log.info("DEPOSIT payment succeeded for paymentId={}, contractId={}, gatewayEventId={}",
                payment.getId(),
                contractId,
                event != null ? event.getExternalEventId() : null);

        // Trigger contract service to check if contract should transition to ACTIVE
        depositContractService.onDepositPaymentCompleted(contractId);
    }
}

