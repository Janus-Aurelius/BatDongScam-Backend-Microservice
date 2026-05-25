package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaywayPaymentId(String paywayPaymentId);

    List<Payment> findByContractId(UUID contractId);

    @Query("SELECT p FROM Payment p WHERE EXTRACT(MONTH FROM p.paidTime) = :month AND EXTRACT(YEAR FROM p.paidTime) = :year AND p.status = 'SUCCESS'")
    List<Payment> findRevenuePaymentsInMonth(@Param("month") int month, @Param("year") int year);
}
