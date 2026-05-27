package com.se.bds.core.property.internal.adapter.in.messaging;

import com.se.bds.core.property.internal.application.port.in.PropertyMaintenanceUseCase;
import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.shared.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * Inbound messaging adapter for the Property module.
 * Listens for events from the Transaction module and delegates to Use Cases.
 */
public class PropertyEventListener {
    private final PropertyMaintenanceUseCase maintenanceUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContractStatusChanged(ContractStatusChangedEvent event) {
        log.info("[PropertyEventListener] ContractStatusChanged received for property {}", event.propertyId());
        maintenanceUseCase.syncWithContractStatus(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[PropertyEventListener] PaymentCompleted received for property {}", event.propertyId());
        maintenanceUseCase.recordPayment(event);
    }
}
