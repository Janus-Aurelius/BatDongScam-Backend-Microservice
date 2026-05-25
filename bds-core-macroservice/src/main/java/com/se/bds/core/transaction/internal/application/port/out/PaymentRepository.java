package com.se.bds.core.transaction.internal.application.port.out;

import com.se.bds.core.transaction.internal.domain.model.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for Payment entities (US-010, US-011, US-028).
 */
public interface PaymentRepository {

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByPaywayPaymentId(String paywayPaymentId);

    Payment save(Payment payment);

    void delete(Payment payment);

    List<Payment> findByContractId(UUID contractId);

    List<Payment> findRevenuePaymentsInMonth(int month, int year);
}
