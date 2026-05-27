package com.se100.bds.services.domains.payment.webhook.impl;

import com.se100.bds.models.entities.contract.Payment;

import com.se100.bds.services.domains.contract.PurchaseContractService;
import com.se100.bds.services.domains.payment.webhook.PaymentGatewayWebhookEvent;
import com.se100.bds.services.domains.payment.webhook.PaymentSucceededSideEffectHandler;
import com.se100.bds.utils.Constants;
import com.se100.bds.utils.Constants.PaymentTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Side effects for ADVANCE and FULL_PAY payments on purchase contracts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchasePaymentSucceededHandler implements PaymentSucceededSideEffectHandler {

    private final PurchaseContractService purchaseContractService;

    @Override
    public boolean supports(Payment payment) {
        if (payment == null || !(payment.getContract().getContractType() == Constants.ContractTypeEnum.PURCHASE)) {
            return false;
        }
        PaymentTypeEnum type = payment.getPaymentType();
        return type == PaymentTypeEnum.ADVANCE || type == PaymentTypeEnum.FULL_PAY;
    }

    @Override
    public void handle(Payment payment, PaymentGatewayWebhookEvent event) {
        if (payment.getContract() == null) {
            log.warn("Purchase payment {} has no contract associated", payment.getId());
            return;
        }

        var contractId = payment.getContract().getId();

        log.info("Purchase payment succeeded: type={}, paymentId={}, contractId={}, gatewayEventId={}",
                payment.getPaymentType(),
                payment.getId(),
                contractId,
                event != null ? event.getExternalEventId() : null);

        if (payment.getPaymentType() == PaymentTypeEnum.ADVANCE) {
            // Advance payment completed - notify agent
            purchaseContractService.onAdvancePaymentCompleted(contractId);
        } else if (payment.getPaymentType() == PaymentTypeEnum.FULL_PAY) {
            // Final payment completed - complete the contract
            purchaseContractService.onFinalPaymentCompleted(contractId);
        }
    }
}
