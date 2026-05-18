package com.se.bds.core.transaction.internal.adapter.in.messaging;

import com.se.bds.core.property.api.event.PropertyStatusChangedEvent;
import com.se.bds.core.transaction.internal.application.port.in.ContractMaintenanceUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * Inbound messaging adapter for the Transaction module.
 * Listens for events from the Property module and delegates to Use Cases.
 */
public class TransactionEventListener {
    private final ContractMaintenanceUseCase maintenanceUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPropertyStatusChanged(PropertyStatusChangedEvent event) {
        log.info("[TransactionEventListener] PropertyStatusChanged received for property {}", event.propertyId());
        maintenanceUseCase.handlePropertyStatusChanged(event);
    }
}
