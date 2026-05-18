# In-Memory Event Bus: The Complete "Rich Domain" Implementation Guide

This guide provides the **full-blown source code** for implementing cross-module side effects. We follow the **Dumb Listener** pattern: Listeners capture events, Use Cases orchestrate the workflow, and Entities handle business rules.

---

## 1. Domain Layer: Rich Entities (`internal.domain.model`)

Before implementing listeners, ensure your entities have the "verbs" (methods) to handle their own state. Our entities use guarded transitions to ensure valid state changes.

### 1.1 — Property.java
**File:** `bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/domain/model/Property.java`

```java
public PropertyStatus markAsSold() {
    return transitionStatus(PropertyStatus.SOLD);
}

public PropertyStatus markAsRented() {
    return transitionStatus(PropertyStatus.RENTED);
}

public PropertyStatus markAsAvailable() {
    if (this.status != PropertyStatus.RENTED) {
        throw new IllegalStateException("Cannot transition to AVAILABLE from " + this.status);
    }
    return transitionStatus(PropertyStatus.AVAILABLE);
}

public boolean recordServiceFeePayment(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Payment amount must be greater than zero");
    }
    this.serviceFeeCollectedAmount = this.serviceFeeCollectedAmount.add(amount);
    return isSeviceFullyPaid();
}
```

### 1.2 — Contract.java / DepositContract.java
**File:** `bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/domain/model/Contract.java`

```java
public ContractStatus cancel(String reason, Role initiator) {
    if (isTerminal()) {
        throw new IllegalStateException("Cannot cancel contract " + this.id + " - already " + this.status);
    }
    ContractStatus oldStatus = this.status;
    this.status = ContractStatus.CANCELLED;
    this.cancellationReason = reason;
    this.cancelledBy = initiator;
    this.cancelledAt = LocalDateTime.now();
    return oldStatus;
}

// In DepositContract.java (Override for specific logic)
@Override
public ContractStatus cancel(String reason, Role initiator) {
    ContractStatus old = super.cancel(reason, initiator);
    if (getCancellationPenalty() == null) {
        setCancellationPenalty(this.getDepositAmount());
    }
    return old;
}
```

---

## 2. Application Layer: Maintenance Use Cases (`internal.application`)

This layer handles the "What happens next?" logic, keeping the listeners "dumb".

### 2.1 — Property Maintenance (Handles Transaction Events)
**File:** `bds-core-macroservice/src/main/java/com/se/bds/core/property/internal/application/service/PropertyMaintenanceService.java`

```java
@Service
@RequiredArgsConstructor
public class PropertyMaintenanceService implements PropertyMaintenanceUseCase {
    private final PropertyRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncWithContractStatus(ContractStatusChangedEvent event) {
        Property property = repository.findById(event.propertyId())
                .orElseThrow(() -> new IllegalStateException("Property not found: " + event.propertyId()));

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

        Property property = repository.findById(event.propertyId()).orElseThrow();
        boolean fullyPaid = property.recordServiceFeePayment(event.amount());
        repository.save(property);

        eventPublisher.publishEvent(new PropertyServiceFeeCollectedEvent(
                new PropertyId(event.propertyId()),
                event.amount(), property.getServiceFeeCollectedAmount(), 
                fullyPaid, Instant.now()));
    }
}
```

### 2.2 — Transaction Maintenance (Handles Property Events)
**File:** `bds-core-macroservice/src/main/java/com/se/bds/core/transaction/internal/application/service/ContractMaintenanceService.java`

```java
@Service
@RequiredArgsConstructor
public class ContractMaintenanceService implements ContractMaintenanceUseCase {
    private final DepositContractRepository depositRepo;
    private final RentalContractRepository rentalRepo;
    private final PurchaseContractRepository purchaseRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelContractsForProperty(UUID propertyId, String reason) {
        depositRepo.findActiveByPropertyId(propertyId).forEach(c -> {
            c.cancel(reason, Role.SYSTEM);
            depositRepo.save(c);
            publishCancellation(c.getId(), "DEPOSIT", propertyId, reason);
        });
        // ... repeat for Rental and Purchase
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePropertyStatusChanged(PropertyStatusChangedEvent event) {
        String status = event.newStatus().name();
        if ("DELETED".equals(status) || "REMOVED".equals(status)) {
            cancelContractsForProperty(event.propertyId().value(), "Property " + status);
        }
    }
}
```

---

## 3. Adapter Layer: Dumb Listeners (`internal.adapter.in.messaging`)

These classes are strictly responsible for catching events and delegating to Use Cases.

### 3.1 — PropertyEventListener.java
```java
@Component
@RequiredArgsConstructor
public class PropertyEventListener {
    private final PropertyMaintenanceUseCase maintenanceUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContractStatusChanged(ContractStatusChangedEvent event) {
        maintenanceUseCase.syncWithContractStatus(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        maintenanceUseCase.recordPayment(event);
    }
}
```

---

## 4. Persistence Layer: Repository Ports (`internal.application.port.out`)

### 4.1 — Interface Additions
Add to `DepositContractRepository`, `RentalContractRepository`, and `PurchaseContractRepository`:
```java
List<T> findActiveByPropertyId(UUID propertyId); 
```

### 4.2 — JPA Implementation
```java
@Query("SELECT c FROM RentalContract c WHERE c.propertyId = :propertyId "
     + "AND c.status NOT IN ('COMPLETED', 'CANCELLED')")
List<RentalContract> findActiveByPropertyId(@Param("propertyId") UUID propertyId);
```

---

## 5. Implementation Roadmap

1.  **[DONE] Modify Entities:** Validated guarded transitions in `Property` and `Contract`.
2.  **[DONE] Add Ports:** Updated repository interfaces and JPA adapters with `findActiveByPropertyId`.
3.  **[DONE] Define Use Case Ports:** Created `PropertyMaintenanceUseCase` and `ContractMaintenanceUseCase`.
4.  **[DONE] Implement Services:** Completed `PropertyMaintenanceService` and `ContractMaintenanceService`.
5.  **[DONE] Create Listeners:** Implemented `PropertyEventListener` and `TransactionEventListener`.
6.  **Verify:** Fire a `ContractStatusChangedEvent` (e.g., via `ContractService`) and verify Property status sync.
