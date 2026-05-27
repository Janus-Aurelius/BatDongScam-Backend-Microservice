package com.se.bds.core.property.internal.application.service;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG18;
import com.se.bds.core.property.api.event.PropertyServiceFeeCollectedEvent;
import com.se.bds.core.property.internal.application.port.in.PropertyMaintenanceUseCase;
import com.se.bds.core.property.internal.application.port.out.PropertyRepository;
import com.se.bds.core.property.internal.domain.model.Property;
import com.se.bds.core.shared.ids.PropertyId;
import com.se.bds.core.shared.event.ContractStatusChangedEvent;
import com.se.bds.core.shared.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyMaintenanceService implements PropertyMaintenanceUseCase {
    private final PropertyRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncWithContractStatus(ContractStatusChangedEvent event) {
        log.info("[PropertyMaintenanceService] Syncing property {} with contract status {}", 
                event.propertyId(), event.newStatus());
                
        Property property = repository.findById(event.propertyId())
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));

        String type = event.contractType();
        String status = event.newStatus();

        if ("ACTIVE".equals(status)) {
            if ("PURCHASE".equals(type)) property.markAsSold();
            else if ("RENTAL".equals(type)) property.markAsRented();
        } 
        else if ("COMPLETED".equals(status) && "RENTAL".equals(type)) {
            property.markAsAvailable();
        }

        repository.save(property);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPayment(PaymentCompletedEvent event) {
        if (!"SERVICE_FEE".equals(event.paymentType())) return;

        log.info("[PropertyMaintenanceService] Recording service fee payment for property {}", event.propertyId());

        Property property = repository.findById(event.propertyId())
                .orElseThrow(() -> new BusinessException(MSG18.CODE, MSG18.MESSAGE));
                
        boolean fullyPaid = property.recordServiceFeePayment(event.amount());
        repository.save(property);

        // Notify downstream (e.g., for notifications or analytics)
        eventPublisher.publishEvent(new PropertyServiceFeeCollectedEvent(
                new PropertyId(event.propertyId()),
                event.amount(), 
                property.getServiceFeeCollectedAmount(), 
                fullyPaid, 
                Instant.now()));
    }
}
