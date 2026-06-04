package com.se.bds.core.transaction.internal.adapter.out.persistence;

import com.se.bds.core.transaction.internal.domain.model.Payment;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    List<Payment> findByContractId(UUID contractId);

    @Query("SELECT p FROM Payment p WHERE EXTRACT(MONTH FROM p.paidTime) = :month AND EXTRACT(YEAR FROM p.paidTime) = :year AND p.status = 'SUCCESS'")
    List<Payment> findRevenuePaymentsInMonth(@Param("month") int month, @Param("year") int year);

    @Query("""
    SELECT p FROM Payment p
    WHERE (COALESCE(:paymentTypes, NULL) IS NULL OR p.paymentType IN :paymentTypes)
    AND (COALESCE(:statuses, NULL) IS NULL OR p.status IN :statuses)
    AND (:payerId IS NULL OR p.payerUserId = :payerId)
    AND (:contractId IS NULL OR p.contract.id = :contractId)
    AND (:propertyId IS NULL OR p.propertyId = :propertyId)
    """)
    org.springframework.data.domain.Page<Payment> searchPayments(
            @Param("paymentTypes") List<PaymentType> paymentTypes,
            @Param("statuses") List<PaymentStatus> statuses,
            @Param("payerId") UUID payerId,
            @Param("contractId") UUID contractId,
            @Param("propertyId") UUID propertyId,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
    SELECT p FROM Payment p
    WHERE p.payerUserId = :payerId
    AND (COALESCE(:statuses, NULL) IS NULL OR p.status IN :statuses)
    """)
    org.springframework.data.domain.Page<Payment> searchPaymentsByPayer(
            @Param("payerId") UUID payerId,
            @Param("statuses") List<PaymentStatus> statuses,
            org.springframework.data.domain.Pageable pageable
    );
}
