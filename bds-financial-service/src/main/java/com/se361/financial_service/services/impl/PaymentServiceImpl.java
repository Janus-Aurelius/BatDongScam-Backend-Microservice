package com.se361.financial_service.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.enums.PaymentType;
import com.se.bds.common.event.PaymentCompletedEvent;
import com.se361.financial_service.dtos.requests.CreatePaymentRequest;
import com.se361.financial_service.dtos.requests.UpdatePaymentStatusRequest;
import com.se361.financial_service.dtos.responses.PaymentResponse;
import com.se361.financial_service.entities.Payment;
import com.se361.financial_service.exceptions.BadRequestException;
import com.se361.financial_service.exceptions.NotFoundException;
import com.se361.financial_service.repositories.PaymentRepository;
import com.se361.financial_service.services.PaymentService;
import com.se361.financial_service.repositories.PayoutRepository;
import com.stripe.model.checkout.Session;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.stripe.exception.SignatureVerificationException;
import org.springframework.beans.factory.annotation.Value;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final PayoutRepository payoutRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
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
            publishPaymentCompleted(saved);
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        log.info("Received Stripe webhook event");
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Verified Stripe webhook event: id={}, type={}", event.getId(), event.getType());

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session != null) {
                    String sessionId = session.getId();
                    var paymentOpt = paymentRepository.findByStripeSessionId(sessionId);
                    if (paymentOpt.isPresent()) {
                        var payment = paymentOpt.get();
                        if (payment.getStatus() != PaymentStatus.SUCCESS) {
                            payment.setStatus(PaymentStatus.SUCCESS);
                            payment.setPaidTime(LocalDateTime.now());
                            payment.setTransactionReference(session.getPaymentIntent());
                            Payment saved = paymentRepository.save(payment);
                            log.info("Stripe Webhook: Payment {} marked SUCCESS", payment.getId());
                            publishPaymentCompleted(saved);
                        }
                    } else {
                        log.warn("Payment not found for Stripe session: {}", sessionId);
                    }
                }
            } else if ("payout.paid".equals(event.getType())) {
                com.stripe.model.Payout payout = (com.stripe.model.Payout) event.getDataObjectDeserializer().getObject().orElse(null);
                if (payout != null) {
                    payoutRepository.findByStripePayoutId(payout.getId()).ifPresentOrElse(p -> {
                        p.setStatus("PAID");
                        payoutRepository.save(p);
                        log.info("Stripe Webhook: Payout {} marked PAID", p.getId());
                    }, () -> log.warn("Payout not found for Stripe payout ID: {}", payout.getId()));
                }
            } else if ("payout.failed".equals(event.getType())) {
                com.stripe.model.Payout payout = (com.stripe.model.Payout) event.getDataObjectDeserializer().getObject().orElse(null);
                if (payout != null) {
                    payoutRepository.findByStripePayoutId(payout.getId()).ifPresentOrElse(p -> {
                        p.setStatus("FAILED");
                        p.setErrorMessage(payout.getFailureMessage());
                        payoutRepository.save(p);
                        log.info("Stripe Webhook: Payout {} marked FAILED", p.getId());
                    }, () -> log.warn("Payout not found for Stripe payout ID: {}", payout.getId()));
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

    private void publishPaymentCompleted(Payment payment) {
        try {
            PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                    payment.getId(),
                    payment.getContractId(),
                    payment.getPropertyId(),
                    payment.getPaymentType().name(),
                    payment.getAmount(),
                    payment.getPayerId(),
                    Instant.now()
            );
            String payload = objectMapper.writeValueAsString(completedEvent);
            log.info("[Kafka] Publishing PaymentCompletedEvent to topic=payment-succeeded: {}", payload);
            kafkaTemplate.send("payment-succeeded", payment.getId().toString(), payload);
        } catch (Exception e) {
            log.error("[Kafka] Failed to publish PaymentCompletedEvent for payment={}", payment.getId(), e);
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
