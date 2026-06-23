package com.se361.financial_service.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.event.PaymentCompletedEvent;
import com.se361.financial_service.dtos.requests.CreatePaymentRequest;
import com.se361.financial_service.dtos.requests.UpdatePaymentStatusRequest;
import com.se361.financial_service.dtos.responses.PaymentResponse;
import com.se361.financial_service.entities.OutboxEvent;
import com.se361.financial_service.entities.Payment;
import com.se361.financial_service.exceptions.BadRequestException;
import com.se361.financial_service.exceptions.NotFoundException;
import com.se361.financial_service.repositories.OutboxEventRepository;
import com.se361.financial_service.repositories.PaymentRepository;
import com.se361.financial_service.repositories.PayoutRepository;
import com.se361.financial_service.services.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REFACTORED: Outbox Pattern.
 *
 * Trước: publishPaymentCompleted() gọi kafkaTemplate.send() trực tiếp.
 *   Rủi ro: DB commit thành công nhưng Kafka sập → event mất vĩnh viễn.
 *           DB rollback nhưng Kafka đã nhận → event sai dữ liệu.
 *
 * Sau: saveToOutbox() lưu event vào bảng outbox_events TRONG CÙNG transaction.
 *   Đảm bảo: DB commit ↔ event tồn tại trong outbox (atomic).
 *   OutboxPublisher relay lên Kafka sau đó (at-least-once delivery).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PayoutRepository payoutRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        if (request.getPaymentType() != PaymentType.MONTHLY) {
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
                .status(PaymentStatus.PENDING)
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
            List<PaymentType> paymentTypes,
            List<PaymentStatus> statuses,
            UUID payerId,
            UUID contractId,
            UUID propertyId,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            Boolean overdue) {
        Specification<Payment> spec = buildSpec(
                paymentTypes, statuses, payerId, contractId,
                propertyId, dueDateFrom, dueDateTo, overdue
        );
        return paymentRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByPayer(UUID payerId, List<PaymentStatus> statuses, Pageable pageable) {
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

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Payment already completed");
        }

        payment.setStatus(request.getStatus());

        if (request.getStatus() == PaymentStatus.SUCCESS) {
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

        if (saved.getStatus() == PaymentStatus.SUCCESS) {
            // lưu vào outbox thay vì gọi Kafka trực tiếp
            savePaymentCompletedToOutbox(saved);
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        log.info("handleStripeWebhook START: payload_length={}, sig={}", 
            (payload != null ? payload.length() : "null"), sigHeader);
        try {
            log.info("Constructing Stripe Event...");
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Stripe Event constructed: id={}, type={}", event.getId(), event.getType());

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session != null) {
                    paymentRepository.findByStripeSessionId(session.getId()).ifPresentOrElse(
                            payment -> {
                                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                                    payment.setStatus(PaymentStatus.SUCCESS);
                                    payment.setPaidTime(LocalDateTime.now());
                                    payment.setTransactionReference(session.getPaymentIntent());
                                    Payment saved = paymentRepository.save(payment);
                                    log.info("Stripe Webhook: Payment {} marked SUCCESS", payment.getId());
                                    // lưu vào outbox thay vì gọi Kafka trực tiếp
                                    savePaymentCompletedToOutbox(saved);
                                }
                            },
                            () -> log.warn("Payment not found for Stripe session: {}", session.getId())
                    );
                } else {
                    log.error("Failed to deserialize Session object for checkout.session.completed event. Raw payload: {}",
                            event.getDataObjectDeserializer().getRawJson());
                }
            } else if ("payout.paid".equals(event.getType())) {
                com.stripe.model.Payout payout =
                        (com.stripe.model.Payout) event.getDataObjectDeserializer().getObject().orElse(null);
                if (payout != null) {
                    payoutRepository.findByStripePayoutId(payout.getId()).ifPresentOrElse(p -> {
                        p.setStatus("PAID");
                        payoutRepository.save(p);
                        log.info("Stripe Webhook: Payout {} marked PAID", p.getId());
                    }, () -> log.warn("Payout not found for Stripe payout ID: {}", payout.getId()));
                } else {
                    log.error("Failed to deserialize Payout object for payout.paid event. Raw payload: {}",
                            event.getDataObjectDeserializer().getRawJson());
                }
            } else if ("payout.failed".equals(event.getType())) {
                com.stripe.model.Payout payout =
                        (com.stripe.model.Payout) event.getDataObjectDeserializer().getObject().orElse(null);
                if (payout != null) {
                    payoutRepository.findByStripePayoutId(payout.getId()).ifPresentOrElse(p -> {
                        p.setStatus("FAILED");
                        p.setErrorMessage(payout.getFailureMessage());
                        payoutRepository.save(p);
                        log.info("Stripe Webhook: Payout {} marked FAILED", p.getId());
                    }, () -> log.warn("Payout not found for Stripe payout ID: {}", payout.getId()));
                } else {
                    log.error("Failed to deserialize Payout object for payout.failed event. Raw payload: {}",
                            event.getDataObjectDeserializer().getRawJson());
                }
            }
        } catch (SignatureVerificationException e) {
            log.error("Failed to verify Stripe webhook signature", e);
            throw new BadRequestException("Invalid webhook signature");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            throw new RuntimeException(e);
        }
    }

    // ======================== HELPERS ========================

    /**
     * THAY publishPaymentCompleted cũ.
     *
     * Lưu PaymentCompletedEvent vào bảng outbox_events TRONG CÙNG @Transactional.
     * OutboxPublisher sẽ relay lên Kafka sau khi transaction commit.
     *
     * Atomic guarantee: payment.status = SUCCESS và outbox record
     * được commit trong 1 transaction duy nhất.
     */
    private void savePaymentCompletedToOutbox(Payment payment) {
        try {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    payment.getId(),
                    payment.getContractId(),
                    payment.getPropertyId(),
                    payment.getPaymentType().name(),
                    payment.getAmount(),
                    payment.getPayerId(),
                    Instant.now()
            );
            String eventPayload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .topic("payment-succeeded")
                    .aggregateId(payment.getId().toString())
                    .payload(eventPayload)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("[Outbox] Saved PaymentCompletedEvent to outbox for paymentId={}", payment.getId());
        } catch (Exception e) {
            // Ném lỗi để rollback cả transaction — không để payment SUCCESS mà không có outbox record
            throw new RuntimeException("Failed to save payment event to outbox for paymentId=" + payment.getId(), e);
        }
    }

    private Specification<Payment> buildSpec(
            List<PaymentType> paymentTypes,
            List<PaymentStatus> statuses,
            UUID payerId,
            UUID contractId,
            UUID propertyId,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            Boolean overdue
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (paymentTypes != null && !paymentTypes.isEmpty()) predicates.add(root.get("paymentType").in(paymentTypes));
            if (statuses != null && !statuses.isEmpty()) predicates.add(root.get("status").in(statuses));
            if (payerId != null) predicates.add(cb.equal(root.get("payerId"), payerId));
            if (contractId != null) predicates.add(cb.equal(root.get("contractId"), contractId));
            if (propertyId != null) predicates.add(cb.equal(root.get("propertyId"), propertyId));
            if (dueDateFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueDateFrom));
            if (dueDateTo != null) predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueDateTo));
            if (Boolean.TRUE.equals(overdue)) {
                predicates.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
                predicates.add(cb.notEqual(root.get("status"), PaymentStatus.SUCCESS));
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