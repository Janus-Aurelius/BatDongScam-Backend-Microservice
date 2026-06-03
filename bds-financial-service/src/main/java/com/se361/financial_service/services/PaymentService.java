package com.se361.financial_service.services;

import com.se361.financial_service.dtos.requests.CreatePaymentRequest;
import com.se361.financial_service.dtos.requests.UpdatePaymentStatusRequest;
import com.se361.financial_service.dtos.responses.PaymentResponse;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse getPaymentById(UUID paymentId);

    Page<PaymentResponse> getPayments(
            Pageable pageable,
            List<PaymentType> paymentTypes,
            List<PaymentStatus> statuses,
            UUID payerId,
            UUID contractId,
            UUID propertyId,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            Boolean overdue
    );

    Page<PaymentResponse> getPaymentsByPayer(UUID payerId, List<PaymentStatus> statuses, Pageable pageable);

    Page<PaymentResponse> getPaymentsByProperty(UUID propertyId, Pageable pageable);

    PaymentResponse updatePaymentStatus(UUID paymentId, UpdatePaymentStatusRequest request);

    void handlePayOSWebhook(String rawBody);
}
