package com.se100.bds.services.domains.payment.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se100.bds.dtos.requests.payment.UpdatePaymentStatusRequest;
import com.se100.bds.dtos.responses.payment.PaymentDetailResponse;
import com.se100.bds.dtos.responses.payment.PaymentListItem;
import com.se100.bds.exceptions.NotFoundException;
import com.se100.bds.models.entities.contract.Payment;
import com.se100.bds.models.entities.property.Property;
import com.se100.bds.repositories.domains.contract.PaymentRepository;
import com.se100.bds.services.payment.PaymentGatewayService;
import com.se100.bds.services.payment.dto.CreatePaymentSessionRequest;
import com.se100.bds.services.payment.payway.PaywayWebhookHandler;
import com.se100.bds.services.payment.payway.dto.PaywayWebhookEvent;
import com.se100.bds.services.domains.payment.PaymentService;
import com.se100.bds.utils.Constants;
import com.se100.bds.utils.Constants.PaymentStatusEnum;
import com.se100.bds.utils.Constants.PaymentTypeEnum;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaywayWebhookHandler paywayWebhookHandler;
    private final ObjectMapper objectMapper;

    private static final String PAYOS_METHOD = "PAYOS";
    private final PaymentGatewayService paymentGatewayService;
    private static final String CURRENCY_VND = "VND";

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentListItem> getPayments(
            Pageable pageable,
            List<PaymentTypeEnum> paymentTypes,
            List<PaymentStatusEnum> statuses,
            UUID payerId,
            UUID payeeId,
            UUID contractId,
            UUID propertyId,
            UUID agentId,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            LocalDate paidDateFrom,
            LocalDate paidDateTo,
            Boolean overdue
    ) {
        Specification<Payment> spec = buildPaymentSpecification(
                paymentTypes, statuses, payerId, payeeId, contractId, propertyId, agentId,
                dueDateFrom, dueDateTo, paidDateFrom, paidDateTo, overdue
        );

        Page<Payment> payments = paymentRepository.findAll(spec, pageable);
        return payments.map(this::mapToListItem);
    }

    // get payments of property
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentListItem> getPaymentsOfProperty(
            Pageable pageable,
            @NotNull UUID propertyId
    ) {
        Page<Payment> payments = paymentRepository.findAllByProperty_Id(propertyId, pageable);
        return payments.map(this::mapToListItem);
    }

    // get payments where user is the payer
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentListItem> getPaymentsByPayer(
            Pageable pageable,
            @NotNull UUID payerId,
            List<PaymentStatusEnum> statuses) {
        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by payer - check multiple paths:
            // 1. Direct payer field
            // 2. Contract customer's user
            // 3. Property owner's user (for service fees)
            Predicate directPayer = cb.equal(root.get("payer").get("id"), payerId);
            Predicate contractCustomer = cb.equal(root.get("contract").get("customer").get("user").get("id"), payerId);
            Predicate propertyOwner = cb.equal(root.get("property").get("owner").get("user").get("id"), payerId);

            predicates.add(cb.or(directPayer, contractCustomer, propertyOwner));

            // Filter by status if provided
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Payment> payments = paymentRepository.findAll(spec, pageable);
        return payments.map(this::mapToListItem);
    }

    // get payments where user is the payee (agent payouts)
    // NOTE: Agent payouts (salary, bonus) not yet implemented in database schema
    // The Payment entity doesn't have a payee/saleAgent field for this purpose
    // TODO: Add payee_user_id field to Payment entity for agent payouts
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentListItem> getPaymentsByPayee(
            Pageable pageable,
            @NotNull UUID payeeId,
            List<PaymentStatusEnum> statuses) {
        // Return empty page until database schema is extended to support agent payouts
        log.warn("Agent payouts feature not yet implemented - returning empty result");
        return Page.empty(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPaymentAccessibleByPropertyOwner(UUID paymentId, UUID ownerUserId) {
        return paymentRepository.findById(paymentId)
                .map(payment -> {
                    // Check if payment's property belongs to the owner
                    if (payment.getProperty() != null
                            && payment.getProperty().getOwner() != null
                            && payment.getProperty().getOwner().getUser() != null) {
                        return payment.getProperty().getOwner().getUser().getId().equals(ownerUserId);
                    }
                    return false;
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        return mapToDetailResponse(payment);
    }

    @Override
    @Transactional
    public PaymentDetailResponse createServiceFeePayment(Property property) {
        var feeToPay = property.getServiceFeeAmount().subtract(property.getServiceFeeCollectedAmount());
        Payment payment = Payment.builder()
            .property(property)
            .payer(property.getOwner().getUser())
            .paymentType(PaymentTypeEnum.SERVICE_FEE)
            .amount(feeToPay)
            .dueDate(LocalDate.now().plusDays(7)) // is due in 7 days
            .status(PaymentStatusEnum.PENDING)
            .paymentMethod(PAYOS_METHOD)
            .build();

        var savedPayment = paymentRepository.save(payment);

        var rq = CreatePaymentSessionRequest.builder()
            .metadata(Map.of(
                "paymentType", Constants.PaymentTypeEnum.SERVICE_FEE,
                "propertyId", savedPayment.getProperty().getId().toString(),
                "paymentId", savedPayment.getId().toString()
            ))
            .amount(savedPayment.getAmount())
            .currency(CURRENCY_VND)
            .description("Service fee payment for property: " + property.getTitle())
            .build();

        var paymentSession = paymentGatewayService.createPaymentSession(rq);

        savedPayment.setPaywayPaymentId(paymentSession.getId());

        var finalPayment = paymentRepository.save(savedPayment);

        log.info("Created service fee payment {} for property {}", finalPayment.getId(), property.getId());

        return mapToDetailResponse(finalPayment);
    }

    @Override
    @Transactional
    public PaymentDetailResponse updatePaymentStatus(UUID paymentId, UpdatePaymentStatusRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));

        PaymentStatusEnum oldStatus = payment.getStatus();
        payment.setStatus(request.getStatus());

        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes());
        }
        if (request.getTransactionReference() != null) {
            payment.setTransactionReference(request.getTransactionReference());
        }

        // Set paid date if marking as success
        if (request.getStatus() == PaymentStatusEnum.SUCCESS || request.getStatus() == PaymentStatusEnum.SYSTEM_SUCCESS) {
            if (payment.getPaidTime() == null) {
                payment.setPaidTime(LocalDateTime.now());
            }
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Updated payment {} status from {} to {}", paymentId, oldStatus, request.getStatus());

        return mapToDetailResponse(saved);
    }

    @Override
    public void handlePaywayWebhook(String rawBody) {
        // Route by event.type, since Payway sends multiple event shapes.
        try {
            PaywayWebhookEvent<?> envelope = objectMapper.readValue(
                    rawBody,
                    objectMapper.getTypeFactory().constructType(PaywayWebhookEvent.class)
            );

            String type = envelope != null ? envelope.getType() : null;
            if (type != null && type.startsWith("payout.")) {
                paywayWebhookHandler.handlePayoutEvent(rawBody);
                return;
            }
        } catch (Exception e) {
            // Best-effort: if we can't parse the envelope, fall back to payment handler.
            log.warn("Payway webhook: unable to parse envelope, falling back to payment handler", e);
        }

        paywayWebhookHandler.handlePaymentEvent(rawBody);
    }

    private Specification<Payment> buildPaymentSpecification(
            List<PaymentTypeEnum> paymentTypes,
            List<PaymentStatusEnum> statuses,
            UUID payerId,
            UUID payeeId,
            UUID contractId,
            UUID propertyId,
            UUID agentId,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            LocalDate paidDateFrom,
            LocalDate paidDateTo,
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
            if (contractId != null) {
                predicates.add(cb.equal(root.get("contract").get("id"), contractId));
            }
            if (propertyId != null) {
                predicates.add(root.get("property").get("id").in(propertyId));
            }
            if (agentId != null) {
                predicates.add(cb.equal(root.get("saleAgent").get("id"), agentId));
            }
            if (dueDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueDateFrom));
            }
            if (dueDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueDateTo));
            }
            if (paidDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paidDate"), paidDateFrom));
            }
            if (paidDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paidDate"), paidDateTo));
            }
            if (Boolean.TRUE.equals(overdue)) {
                predicates.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
                predicates.add(cb.isNull(root.get("paidDate")));
                predicates.add(root.get("status").in(PaymentStatusEnum.PENDING, PaymentStatusEnum.SYSTEM_PENDING));
            }

            // Payer filter - derived from contract customer or property owner
            if (payerId != null) {
                Predicate contractCustomer = cb.equal(root.get("contract").get("customer").get("id"), payerId);
                Predicate propertyOwner = cb.equal(root.get("property").get("owner").get("id"), payerId);
                predicates.add(cb.or(contractCustomer, propertyOwner));
            }

            // Payee filter - typically agent for salary/bonus, or could be owner for refunds
            if (payeeId != null) {
                Predicate agentPayee = cb.equal(root.get("saleAgent").get("id"), payeeId);
                Predicate ownerPayee = cb.equal(root.get("property").get("owner").get("id"), payeeId);
                predicates.add(cb.or(agentPayee, ownerPayee));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PaymentListItem mapToListItem(Payment payment) {
        PaymentListItem.PaymentListItemBuilder builder = PaymentListItem.builder()
                .id(payment.getId())
                .createdAt(payment.getCreatedAt())
                .paymentType(payment.getPaymentType() != null ? payment.getPaymentType().name() : null)
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .amount(payment.getAmount())
                .dueDate(payment.getDueDate())
                .paidTime(payment.getPaidTime());

        return builder.build();
    }

    // TODO: reuse mapping logic with PaymentMapper
    private PaymentDetailResponse mapToDetailResponse(Payment payment) {
        var payerResponse = PaymentDetailResponse.PaymentPayerResponse.builder()
                .id(payment.getPayer() != null ? payment.getPayer().getId() : null)
                .name(payment.getPayer() != null ? payment.getPayer().getFullName() : null)
                .email(payment.getPayer() != null ? payment.getPayer().getEmail() : null)
                .phoneNumber(payment.getPayer() != null ? payment.getPayer().getPhoneNumber() : null)
                .build();

        PaymentDetailResponse.PaymentDetailResponseBuilder builder = PaymentDetailResponse.builder()
                .id(payment.getId())
                .payer(payerResponse)
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .paywayPaymentId(payment.getPaywayPaymentId())
                .paymentType(payment.getPaymentType() != null ? payment.getPaymentType().name() : null)
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .amount(payment.getAmount())
                .penaltyAmount(payment.getPenaltyAmount())
                .dueDate(payment.getDueDate())
                .paidTime(payment.getPaidTime())
                .installmentNumber(payment.getInstallmentNumber())
                .paymentMethod(payment.getPaymentMethod())
                .transactionReference(payment.getTransactionReference())
                .notes(payment.getNotes());

        return builder.build();
    }
}
