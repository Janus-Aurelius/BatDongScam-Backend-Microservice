package com.se100.bds.services.domains.payment.webhook.impl;

import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.repositories.domains.property.PropertyRepository;
import com.se100.bds.services.domains.payment.webhook.PaymentGatewayWebhookEvent;
import com.se100.bds.services.domains.payment.webhook.PaymentSucceededSideEffectHandler;
import com.se100.bds.services.domains.report.FinancialUpdateService;
import com.se100.bds.utils.Constants;
import com.se100.bds.utils.Constants.PaymentTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Side effects for SERVICE_FEE payments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceFeePaymentSucceededHandler implements PaymentSucceededSideEffectHandler {

    private final FinancialUpdateService financialUpdateService;
    private final PropertyRepository propertyRepository;

    @Override
    public boolean supports(Payment payment) {
        return payment != null && payment.getPaymentType() == PaymentTypeEnum.SERVICE_FEE;
    }

    @Override
    public void handle(Payment payment, PaymentGatewayWebhookEvent event) {
        // Update financial reports
        var currentTime = LocalDateTime.now();
        financialUpdateService.transaction(
                payment.getProperty().getId(),
                payment.getAmount(),
                currentTime.getMonthValue(), currentTime.getYear());

        var property = payment.getProperty();
        var totalFee = property.getServiceFeeAmount();
        var collected = property.getServiceFeeCollectedAmount();
        var newCollected = collected.add(payment.getAmount());
        if (newCollected.compareTo(collected) > 0) {
            property.setServiceFeeCollectedAmount(newCollected);
        }
        if (newCollected.compareTo(totalFee) >= 0) {
            property.setStatus(Constants.PropertyStatusEnum.AVAILABLE);
        }
        propertyRepository.save(property);

        log.info("SERVICE_FEE succeeded for paymentId={}, contractId={}, propertyId={}, gatewayEventId={}",
                payment.getId(),
                payment.getContract() != null ? payment.getContract().getId() : null,
                payment.getProperty() != null ? payment.getProperty().getId() : null,
                event != null ? event.getExternalEventId() : null);
    }
}
