package com.se.bds.core.transaction.internal.application.service;

import com.se.bds.core.property.api.event.PropertyStatusChangedEvent;
import com.se.bds.core.shared.enums.Role;
import com.se.bds.core.shared.ids.ContractId;
import com.se.bds.core.transaction.api.event.ContractCancelledEvent;
import com.se.bds.core.transaction.internal.application.port.in.ContractMaintenanceUseCase;
import com.se.bds.core.transaction.internal.application.port.out.DepositContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.PurchaseContractRepository;
import com.se.bds.core.transaction.internal.application.port.out.RentalContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractMaintenanceService implements ContractMaintenanceUseCase {
    private final DepositContractRepository depositRepo;
    private final RentalContractRepository rentalRepo;
    private final PurchaseContractRepository purchaseRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelContractsForProperty(UUID propertyId, String reason) {
        log.info("[ContractMaintenanceService] Cancelling all active contracts for property {} due to: {}", 
                propertyId, reason);

        // 1. Deposits
        depositRepo.findActiveByPropertyId(propertyId).forEach(c -> {
            c.cancel(reason, Role.ADMIN);
            depositRepo.save(c);
            publishCancellation(c.getId(), "DEPOSIT", propertyId, reason);
        });

        // 2. Rentals
        rentalRepo.findActiveByPropertyId(propertyId).forEach(c -> {
            c.cancel(reason, Role.ADMIN);
            rentalRepo.save(c);
            publishCancellation(c.getId(), "RENTAL", propertyId, reason);
        });

        // 3. Purchases
        purchaseRepo.findActiveByPropertyId(propertyId).forEach(c -> {
            c.cancel(reason, Role.ADMIN);
            purchaseRepo.save(c);
            publishCancellation(c.getId(), "PURCHASE", propertyId, reason);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePropertyStatusChanged(PropertyStatusChangedEvent event) {
        String status = event.newStatus();
        if ("DELETED".equals(status) || "REMOVED".equals(status)) {
            cancelContractsForProperty(
                event.propertyId().value(), 
                "System: Property transitioned to " + status.toLowerCase());
        }
    }

    private void publishCancellation(UUID id, String type, UUID propId, String reason) {
        eventPublisher.publishEvent(new ContractCancelledEvent(
                new ContractId(id), type, propId, Role.ADMIN, reason, Instant.now()));
    }
}
