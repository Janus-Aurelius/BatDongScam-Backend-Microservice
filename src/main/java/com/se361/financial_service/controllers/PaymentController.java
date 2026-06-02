package com.se361.financial_service.controllers;

import com.se361.financial_service.dtos.requests.CreatePaymentRequest;
import com.se361.financial_service.dtos.requests.UpdatePaymentStatusRequest;
import com.se361.financial_service.dtos.responses.PageResponse;
import com.se361.financial_service.dtos.responses.PaymentResponse;
import com.se361.financial_service.dtos.responses.SingleResponse;
import com.se361.financial_service.securities.SecurityUtils;
import com.se361.financial_service.services.PaymentService;
import com.se361.financial_service.utils.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController extends AbstractBaseController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    public ResponseEntity<SingleResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.createPayment(request);
        return responseFactory.successSingle(response, "Payment created successfully");
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT', 'CUSTOMER', 'PROPERTY_OWNER')")
    public ResponseEntity<SingleResponse<PaymentResponse>> getPaymentById(
            @PathVariable UUID paymentId
    ) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return responseFactory.successSingle(response, "Payment retrieved successfully");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PaymentResponse>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(required = false) List<Constants.PaymentType> paymentTypes,
            @RequestParam(required = false) List<Constants.PaymentStatus> statuses,
            @RequestParam(required = false) UUID payerId,
            @RequestParam(required = false) UUID contractId,
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(required = false) Boolean overdue
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<PaymentResponse> payments = paymentService.getPayments(
                pageable, paymentTypes, statuses, payerId,
                contractId, propertyId, dueDateFrom, dueDateTo, overdue
        );
        return responseFactory.successPage(payments, "Payments retrieved successfully");
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER')")
    public ResponseEntity<PageResponse<PaymentResponse>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<Constants.PaymentStatus> statuses
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentResponse> payments = paymentService.getPaymentsByPayer(currentUserId, statuses, pageable);
        return responseFactory.successPage(payments, "My payments retrieved successfully");
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_OWNER')")
    public ResponseEntity<PageResponse<PaymentResponse>> getPaymentsByProperty(
            @PathVariable UUID propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentResponse> payments = paymentService.getPaymentsByProperty(propertyId, pageable);
        return responseFactory.successPage(payments, "Property payments retrieved successfully");
    }

    @PatchMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SingleResponse<PaymentResponse>> updatePaymentStatus(
            @PathVariable UUID paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        PaymentResponse response = paymentService.updatePaymentStatus(paymentId, request);
        return responseFactory.successSingle(response, "Payment status updated successfully");
    }
}
