package com.se.bds.core.property.internal.application.port.in;

import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.shared.event.PaymentCompletedEvent;

public interface PropertyMaintenanceUseCase {
    void syncWithContractStatus(ContractStatusChangedEvent event);
    void recordPayment(PaymentCompletedEvent event);
}
