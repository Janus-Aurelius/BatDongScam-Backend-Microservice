package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.application.port.out.PaymentRepository;
import com.se.bds.core.transaction.internal.domain.model.Payment;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final JpaPaymentRepository jpaPaymentRepository;

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaPaymentRepository.findById(id);
    }

    @Override
    public Optional<Payment> findByStripeSessionId(String stripeSessionId) {
        return jpaPaymentRepository.findByStripeSessionId(stripeSessionId);
    }

    @Override
    public Payment save(Payment payment) {
        return jpaPaymentRepository.save(payment);
    }

    @Override
    public void delete(Payment payment) {
        jpaPaymentRepository.delete(payment);
    }

    @Override
    public List<Payment> findByContractId(UUID contractId) {
        return jpaPaymentRepository.findByContractId(contractId);
    }

    @Override
    public List<Payment> findRevenuePaymentsInMonth(int month, int year) {
        return jpaPaymentRepository.findRevenuePaymentsInMonth(month, year);
    }

    @Override
    public org.springframework.data.domain.Page<Payment> searchPayments(
            List<PaymentType> paymentTypes,
            List<PaymentStatus> statuses,
            UUID payerId,
            UUID contractId,
            UUID propertyId,
            org.springframework.data.domain.Pageable pageable
    ) {
        return jpaPaymentRepository.searchPayments(paymentTypes, statuses, payerId, contractId, propertyId, pageable);
    }

    @Override
    public org.springframework.data.domain.Page<Payment> searchPaymentsByPayer(
            UUID payerId,
            List<PaymentStatus> statuses,
            org.springframework.data.domain.Pageable pageable
    ) {
        return jpaPaymentRepository.searchPaymentsByPayer(payerId, statuses, pageable);
    }
}
