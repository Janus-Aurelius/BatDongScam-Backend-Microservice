package com.se.bds.core.transaction.internal.application.port.out;

import com.se.bds.core.transaction.internal.domain.model.Payment;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound repository port for Payment entities (US-010, US-011, US-028).
 */
public interface PaymentRepository {

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    Payment save(Payment payment);

    void delete(Payment payment);

    List<Payment> findByContractId(UUID contractId);

    List<Payment> findRevenuePaymentsInMonth(int month, int year);

    org.springframework.data.domain.Page<Payment> searchPayments(
            List<PaymentType> paymentTypes,
            List<PaymentStatus> statuses,
            UUID payerId,
            UUID contractId,
            UUID propertyId,
            org.springframework.data.domain.Pageable pageable
    );

    org.springframework.data.domain.Page<Payment> searchPaymentsByPayer(
            UUID payerId,
            List<PaymentStatus> statuses,
            org.springframework.data.domain.Pageable pageable
    );
}
