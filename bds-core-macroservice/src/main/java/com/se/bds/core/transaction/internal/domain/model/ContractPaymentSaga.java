package com.se.bds.core.transaction.internal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persists the state of the distributed Contract Payment Saga for recovery and auditing.
 */
@Entity
@Table(name = "contract_payment_sagas", schema = "transaction_workflow")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractPaymentSaga {

    @Id
    @Column(name = "saga_id", nullable = false)
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
