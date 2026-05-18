package com.se.bds.core.transaction.internal.application.port.in;

import com.se.bds.core.property.api.event.PropertyStatusChangedEvent;
import java.util.UUID;

public interface ContractMaintenanceUseCase {
    void cancelContractsForProperty(UUID propertyId, String reason);
    void handlePropertyStatusChanged(PropertyStatusChangedEvent event);
}
