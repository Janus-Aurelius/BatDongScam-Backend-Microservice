package com.se.bds.core.transaction.internal.application.port.in;

import com.se.bds.core.transaction.internal.domain.model.Payment;
import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.enums.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface PaymentQueryUseCase {
    Page<Payment> searchPayments(
            List<PaymentType> paymentTypes,
            List<PaymentStatus> statuses,
            UUID payerId,
            UUID contractId,
            UUID propertyId,
            Pageable pageable
    );

    Page<Payment> searchPaymentsByPayer(
            UUID payerId,
            List<PaymentStatus> statuses,
            Pageable pageable
    );

    Payment getPaymentById(UUID id);

    Payment updatePaymentStatus(UUID id, PaymentStatus status);
}
