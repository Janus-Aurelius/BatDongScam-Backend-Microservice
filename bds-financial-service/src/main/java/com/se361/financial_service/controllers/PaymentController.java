package com.se361.financial_service.controllers;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.dto.PagedData;
import com.se.bds.common.enums.PaymentStatus;
import com.se.bds.common.enums.PaymentType;
import com.se361.financial_service.dtos.requests.CreatePaymentRequest;
import com.se361.financial_service.dtos.requests.UpdatePaymentStatusRequest;
import com.se361.financial_service.dtos.responses.PaymentResponse;
import com.se361.financial_service.securities.SecurityUtils;
import com.se361.financial_service.services.PaymentService;
import com.se361.financial_service.gateway.CreatePaymentSessionRequest;
import com.se361.financial_service.gateway.CreatePaymentSessionResponse;
import com.se361.financial_service.gateway.CreatePayoutSessionRequest;
import com.se361.financial_service.gateway.CreatePayoutSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments", description = "Endpoints for creating and managing financial payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @Operation(summary = "Create a new payment", description = "Manually creates a payment record in the system. Typically used by Admins or Sale Agents to initialize deposits or installments.")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT', 'CUSTOMER', 'PROPERTY_OWNER')")
    @Operation(summary = "Get payment by ID", description = "Retrieves the detailed information of a specific payment record.")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable UUID paymentId
    ) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedData<PaymentResponse>>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(required = false) List<PaymentType> paymentTypes,
            @RequestParam(required = false) List<PaymentStatus> statuses,
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
        return ResponseEntity.ok(ApiResponse.success(toPagedData(payments)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER')")
    public ResponseEntity<ApiResponse<PagedData<PaymentResponse>>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<PaymentStatus> statuses
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentResponse> payments = paymentService.getPaymentsByPayer(currentUserId, statuses, pageable);
        return ResponseEntity.ok(ApiResponse.success(toPagedData(payments)));
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROPERTY_OWNER')")
    public ResponseEntity<ApiResponse<PagedData<PaymentResponse>>> getPaymentsByProperty(
            @PathVariable UUID propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentResponse> payments = paymentService.getPaymentsByProperty(propertyId, pageable);
        return ResponseEntity.ok(ApiResponse.success(toPagedData(payments)));
    }

    @PatchMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentStatus(
            @PathVariable UUID paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        PaymentResponse response = paymentService.updatePaymentStatus(paymentId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private <T> PagedData<T> toPagedData(Page<T> page) {
        return PagedData.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
