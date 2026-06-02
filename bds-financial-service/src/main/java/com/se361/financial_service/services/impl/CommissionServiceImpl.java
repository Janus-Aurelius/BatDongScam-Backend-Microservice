package com.se361.financial_service.services.impl;

import com.se361.financial_service.dtos.requests.CreateCommissionRequest;
import com.se361.financial_service.dtos.responses.CommissionResponse;
import com.se361.financial_service.entities.Commission;
import com.se361.financial_service.exceptions.NotFoundException;
import com.se361.financial_service.repositories.CommissionRepository;
import com.se361.financial_service.services.CommissionService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionServiceImpl implements CommissionService {

    private final CommissionRepository commissionRepository;

    @Override
    @Transactional
    public CommissionResponse createCommission(CreateCommissionRequest request) {
        Commission commission = Commission.builder()
                .paymentId(request.getPaymentId())
                .contractId(request.getContractId())
                .propertyId(request.getPropertyId())
                .agentId(request.getAgentId())
                .agentName(request.getAgentName())
                .propertyTitle(request.getPropertyTitle())
                .commissionAmount(request.getCommissionAmount())
                .transactionAmount(request.getTransactionAmount())
                .commissionDate(request.getCommissionDate() != null
                        ? request.getCommissionDate()
                        : LocalDate.now())
                .status(Constants.CommissionStatus.PENDING)
                .notes(request.getNotes())
                .build();

        Commission saved = commissionRepository.save(commission);
        log.info("Created commission {} for agent {}", saved.getId(), request.getAgentId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionResponse getCommissionById(UUID commissionId) {
        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new NotFoundException("Commission not found: " + commissionId));
        return mapToResponse(commission);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionResponse> getCommissions(
            Pageable pageable,
            UUID agentId,
            UUID propertyId,
            UUID contractId,
            List<Constants.CommissionStatus> statuses
    ) {
        Specification<Commission> spec = buildSpec(agentId, propertyId, contractId, statuses);
        return commissionRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionResponse> getCommissionsByAgent(
            UUID agentId,
            List<Constants.CommissionStatus> statuses,
            Pageable pageable
    ) {
        if (statuses != null && !statuses.isEmpty()) {
            return commissionRepository.findByAgentIdAndStatusIn(agentId, statuses, pageable)
                    .map(this::mapToResponse);
        }
        return commissionRepository.findByAgentId(agentId, pageable).map(this::mapToResponse);
    }

    @Override
    public CommissionResponse updateCommissionStatus(
            UUID commissionId,
            Constants.CommissionStatus status,
            String notes
    ) {
        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new NotFoundException("Commission not found: " + commissionId));

        commission.setStatus(status);
        if (notes != null) {
            commission.setNotes(notes);
        }

        Commission saved = commissionRepository.save(commission);
        log.info("Updated commission {} status to {}", commissionId, status);

        return mapToResponse(saved);
    }

    private Specification<Commission> buildSpec(
            UUID agentId,
            UUID propertyId,
            UUID contractId,
            List<Constants.CommissionStatus> statuses
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (agentId != null) {
                predicates.add(cb.equal(root.get("agentId"), agentId));
            }
            if (propertyId != null) {
                predicates.add(cb.equal(root.get("propertyId"), propertyId));
            }
            if (contractId != null) {
                predicates.add(cb.equal(root.get("contractId"), contractId));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private CommissionResponse mapToResponse(Commission commission) {
        return CommissionResponse.builder()
                .id(commission.getId())
                .paymentId(commission.getPaymentId())
                .contractId(commission.getContractId())
                .propertyId(commission.getPropertyId())
                .agentId(commission.getAgentId())
                .agentName(commission.getAgentName())
                .propertyTitle(commission.getPropertyTitle())
                .commissionAmount(commission.getCommissionAmount())
                .transactionAmount(commission.getTransactionAmount())
                .commissionDate(commission.getCommissionDate())
                .status(commission.getStatus())
                .notes(commission.getNotes())
                .createdAt(commission.getCreatedAt())
                .updatedAt(commission.getUpdatedAt())
                .build();
    }
}
