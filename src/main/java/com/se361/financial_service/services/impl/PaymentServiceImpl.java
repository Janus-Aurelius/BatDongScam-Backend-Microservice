package com.se361.financial_service.services.impl;

import com.se361.financial_service.dtos.requests.CreatePaymentRequest;
import com.se361.financial_service.dtos.requests.UpdatePaymentStatusRequest;
import com.se361.financial_service.dtos.responses.PaymentResponse;
import com.se361.financial_service.entities.Payment;
import com.se361.financial_service.exceptions.BadRequestException;
import com.se361.financial_service.exceptions.NotFoundException;
import com.se361.financial_service.repositories.PaymentRepository;
import com.se361.financial_service.services.PaymentService;
import com.se361.financial_service.utils.Constants;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;


    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        if (request.getPaymentType() != Constants.PaymentType.MONTHLY) {
            boolean exists = paymentRepository.existsByContractIdAndPaymentType(
                    request.getContractId(), request.getPaymentType()
            );
            if (exists) {
                throw new BadRequestException(
                        "Payment of type " + request.getPaymentType() + " already exists for this contract"
                );
            }
        }

        Payment payment = Payment.builder()
                .contractId(request.getContractId())
                .propertyId(request.getPropertyId())
                .payerId(request.getPayerId())
                .payerName(request.getPayerName())
                .propertyTitle(request.getPropertyTitle())
                .contractNumber(request.getContractNumber())
                .paymentType(request.getPaymentType())
                .status(Constants.PaymentStatus.PENDING)
                .amount(request.getAmount())
                .dueDate(request.getDueDate())
                .installmentNumber(request.getInstallmentNumber())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Created payment {} for contract {}", saved.getId(), request.getContractId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(
            Pageable pageable,
            List<Constants.PaymentType> paymentTypes,
            List<Constants.PaymentStatus> statuses,
            UUID payerId,
            UUID contractId,
            UUID propertyId,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            Boolean overdue)
    {
        Specification<Payment> spec = buildSpec(
                paymentTypes, statuses, payerId, contractId,
                propertyId, dueDateFrom, dueDateTo, overdue
        );
        return paymentRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByPayer(UUID payerId, List<Constants.PaymentStatus> statuses, Pageable pageable) {
        if (statuses != null && !statuses.isEmpty()) {
            return paymentRepository.findByPayerIdAndStatusIn(payerId, statuses, pageable)
                    .map(this::mapToResponse);
        }
        return paymentRepository.findByPayerId(payerId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByProperty(UUID propertyId, Pageable pageable) {
        return paymentRepository.findByPropertyId(propertyId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(UUID paymentId, UpdatePaymentStatusRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() == Constants.PaymentStatus.SUCCESS) {
            throw new BadRequestException("Payment already completed");
        }

        payment.setStatus(request.getStatus());

        if (request.getStatus() == Constants.PaymentStatus.SUCCESS) {
            payment.setPaidTime(LocalDateTime.now());
        }
        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes());
        }
        if (request.getTransactionReference() != null) {
            payment.setTransactionReference(request.getTransactionReference());
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Updated payment {} status to {}", paymentId, request.getStatus());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void handlePayOSWebhook(String rawBody) {
        // TODO: parse PayOS webhook payload
        // 1. Parse rawBody to extract payosPaymentId and status
        // 2. Find payment by payosPaymentId
        // 3. Update status accordingly
        log.info("Received PayOS webhook: {}", rawBody);
    }

    private Specification<Payment> buildSpec(
            List<Constants.PaymentType> paymentTypes,
            List<Constants.PaymentStatus> statuses,
            UUID payerId,
            UUID contractId,
            UUID propertyId,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            Boolean overdue
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (paymentTypes != null && !paymentTypes.isEmpty()) {
                predicates.add(root.get("paymentType").in(paymentTypes));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (payerId != null) {
                predicates.add(cb.equal(root.get("payerId"), payerId));
            }
            if (contractId != null) {
                predicates.add(cb.equal(root.get("contractId"), contractId));
            }
            if (propertyId != null) {
                predicates.add(cb.equal(root.get("propertyId"), propertyId));
            }
            if (dueDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueDateFrom));
            }
            if (dueDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueDateTo));
            }
            if (Boolean.TRUE.equals(overdue)) {
                predicates.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
                predicates.add(cb.notEqual(root.get("status"), Constants.PaymentStatus.SUCCESS));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .contractId(payment.getContractId())
                .propertyId(payment.getPropertyId())
                .payerId(payment.getPayerId())
                .payerName(payment.getPayerName())
                .propertyTitle(payment.getPropertyTitle())
                .contractNumber(payment.getContractNumber())
                .paymentType(payment.getPaymentType())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .penaltyAmount(payment.getPenaltyAmount())
                .dueDate(payment.getDueDate())
                .paidTime(payment.getPaidTime())
                .installmentNumber(payment.getInstallmentNumber())
                .paymentMethod(payment.getPaymentMethod())
                .transactionReference(payment.getTransactionReference())
                .notes(payment.getNotes())
                .checkoutUrl(payment.getCheckoutUrl())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
