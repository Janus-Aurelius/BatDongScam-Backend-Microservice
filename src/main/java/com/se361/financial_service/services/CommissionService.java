package com.se361.financial_service.services;

import com.se361.financial_service.dtos.requests.CreateCommissionRequest;
import com.se361.financial_service.dtos.responses.CommissionResponse;
import com.se361.financial_service.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;
public interface CommissionService {
    CommissionResponse createCommission(CreateCommissionRequest request);

    CommissionResponse getCommissionById(UUID commissionId);

    Page<CommissionResponse> getCommissions(
            Pageable pageable,
            UUID agentId,
            UUID propertyId,
            UUID contractId,
            List<Constants.CommissionStatus> statuses
    );

    Page<CommissionResponse> getCommissionsByAgent(UUID agentId, List<Constants.CommissionStatus> statuses, Pageable pageable);

    CommissionResponse updateCommissionStatus(UUID commissionId, Constants.CommissionStatus status, String notes);
}
