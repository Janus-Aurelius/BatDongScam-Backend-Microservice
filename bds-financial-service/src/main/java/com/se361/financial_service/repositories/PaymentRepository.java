package com.se361.financial_service.repositories;

import com.se361.financial_service.entities.Payment;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    Page<Payment> findByPayerId(UUID payerId, Pageable pageable);

    Page<Payment> findByPayerIdAndStatusIn(UUID payerId, List<PaymentStatus> statuses, Pageable pageable);

    Page<Payment> findByPropertyId(UUID propertyId, Pageable pageable);

    Page<Payment> findByContractId(UUID contractId, Pageable pageable);

    List<Payment> findByContractIdAndPaymentType(UUID contractId, PaymentType paymentType);

    Optional<Payment> findByPayosPaymentId(String payosPaymentId);

    boolean existsByContractIdAndPaymentType(UUID contractId, PaymentType paymentType);

}
