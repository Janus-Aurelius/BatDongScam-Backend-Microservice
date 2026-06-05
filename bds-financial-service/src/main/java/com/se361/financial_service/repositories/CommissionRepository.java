package com.se361.financial_service.repositories;

import com.se361.financial_service.entities.Commission;
import com.se361.financial_service.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, UUID>,
        JpaSpecificationExecutor<Commission> {

    Page<Commission> findByAgentId(UUID agentId, Pageable pageable);

    Page<Commission> findByAgentIdAndStatusIn(UUID agentId, List<Constants.CommissionStatus> statuses,
                                              Pageable pageable);

    Page<Commission> findByPropertyId(UUID propertyId, Pageable pageable);

    Page<Commission> findByContractId(UUID contractId, Pageable pageable);

    List<Commission> findByPaymentId(UUID paymentId);
}
