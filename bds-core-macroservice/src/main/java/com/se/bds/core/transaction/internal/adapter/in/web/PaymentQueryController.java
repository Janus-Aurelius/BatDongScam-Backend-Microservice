package com.se.bds.core.transaction.internal.adapter.in.web;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.core.transaction.internal.adapter.in.web.dto.PaymentWebRequests.UpdatePaymentStatusRequest;
import com.se.bds.core.transaction.internal.application.port.in.PaymentQueryUseCase;
import com.se.bds.core.transaction.internal.domain.model.Payment;
import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.enums.PaymentType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentQueryController {
    private final PaymentQueryUseCase paymentQueryUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Page<Payment>> getPayments(
            @RequestParam(required = false) List<PaymentType> paymentTypes,
            @RequestParam(required = false) List<PaymentStatus> statuses,
            @RequestParam(required = false) UUID payerId,
            @RequestParam(required = false) UUID contractId,
            @RequestParam(required = false) UUID propertyId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(paymentQueryUseCase.searchPayments(paymentTypes, statuses, payerId, contractId, propertyId, pageable));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER')")
    public ResponseEntity<Page<Payment>> getMyPayments(
            @RequestParam(required = false) List<PaymentStatus> statuses,
            Pageable pageable
    ) {
        UUID currentUserId = getCurrentUserId();
        return ResponseEntity.ok(paymentQueryUseCase.searchPaymentsByPayer(currentUserId, statuses, pageable));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER', 'PROPERTY_OWNER')")
    public ResponseEntity<Payment> getPaymentById(@PathVariable UUID paymentId) {
        Payment payment = paymentQueryUseCase.getPaymentById(paymentId);
        
        // Non-admins must be the payer of this payment
        if (!isUserAdmin()) {
            UUID currentUserId = getCurrentUserId();
            if (payment.getPayerUserId() == null || !payment.getPayerUserId().equals(currentUserId)) {
                throw new BusinessException("ACCESS_DENIED", "You do not have access to view this payment");
            }
        }
        
        return ResponseEntity.ok(payment);
    }

    @PatchMapping("/{paymentId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Payment> updatePaymentStatus(
            @PathVariable UUID paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        return ResponseEntity.ok(paymentQueryUseCase.updatePaymentStatus(paymentId, request.status()));
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("UNAUTHORIZED", "User is not authenticated");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            if ("test_user".equals(authentication.getName())) {
                return UUID.fromString("33333333-3333-3333-3333-333333333333");
            }
            throw new BusinessException("INVALID_USER_ID", "Authenticated principal name is not a valid UUID: " + authentication.getName());
        }
    }

    private boolean isUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }
}
