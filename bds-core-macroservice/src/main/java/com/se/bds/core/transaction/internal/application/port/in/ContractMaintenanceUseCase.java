package com.se.bds.core.transaction.internal.application.port.in;

import com.se.bds.core.property.api.event.PropertyStatusChangedEvent;
import java.util.UUID;

public interface ContractMaintenanceUseCase {
    void cancelContractsForProperty(UUID propertyId, String reason);
    void handlePropertyStatusChanged(PropertyStatusChangedEvent event);
    void updateContractDraft(UUID contractId, java.time.LocalDate startDate, java.time.LocalDate endDate, String specialTerms);
    void rateContract(UUID contractId, UUID raterUserId, Short rating, String comment);
}
