package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.application.port.out.PaymentRepository;
import com.se.bds.core.transaction.internal.domain.model.Payment;
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
    public Optional<Payment> findByPaywayPaymentId(String paywayPaymentId) {
        return jpaPaymentRepository.findByPaywayPaymentId(paywayPaymentId);
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
}
