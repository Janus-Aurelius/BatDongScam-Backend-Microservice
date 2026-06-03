package com.se361.financial_service.controllers;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.dto.PagedData;
import com.se361.financial_service.dtos.requests.CreateCommissionRequest;
import com.se361.financial_service.dtos.requests.UpdateCommissionStatusRequest;
import com.se361.financial_service.dtos.responses.CommissionResponse;
import com.se361.financial_service.securities.SecurityUtils;
import com.se361.financial_service.services.CommissionService;
import com.se361.financial_service.utils.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommissionResponse>> createCommission(
            @Valid @RequestBody CreateCommissionRequest request
    ) {
        CommissionResponse response = commissionService.createCommission(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{commissionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    public ResponseEntity<ApiResponse<CommissionResponse>> getCommissionById(
            @PathVariable UUID commissionId
    ) {
        CommissionResponse response = commissionService.getCommissionById(commissionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedData<CommissionResponse>>> getCommissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) UUID contractId,
            @RequestParam(required = false) List<Constants.CommissionStatus> statuses
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<CommissionResponse> commissions = commissionService.getCommissions(
                pageable, agentId, propertyId, contractId, statuses
        );
        return ResponseEntity.ok(ApiResponse.success(toPagedData(commissions)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SALESAGENT')")
    public ResponseEntity<ApiResponse<PagedData<CommissionResponse>>> getMyCommissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<Constants.CommissionStatus> statuses
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommissionResponse> commissions = commissionService.getCommissionsByAgent(
                currentUserId, statuses, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(toPagedData(commissions)));
    }

    @PatchMapping("/{commissionId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommissionResponse>> updateCommissionStatus(
            @PathVariable UUID commissionId,
            @Valid @RequestBody UpdateCommissionStatusRequest request
    ) {
        CommissionResponse response = commissionService.updateCommissionStatus(
                commissionId, request.getStatus(), request.getNotes()
        );
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
