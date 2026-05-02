package com.se100.bds.repositories.domains.contract;

import com.se100.bds.models.entities.contract.DepositContract;
import com.se100.bds.utils.Constants.ContractStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepositContractRepository extends JpaRepository<DepositContract, UUID>, JpaSpecificationExecutor<DepositContract> {

    /**
     * Check if there's an existing non-draft deposit contract for a property
     */
    @Query("SELECT COUNT(d) > 0 FROM DepositContract d WHERE d.property.id = :propertyId AND d.status <> :draftStatus")
    boolean existsNonDraftDepositContractForProperty(@Param("propertyId") UUID propertyId, @Param("draftStatus") ContractStatusEnum draftStatus);

    /**
     * Find all deposit contracts for a property
     */
    List<DepositContract> findAllByProperty_Id(UUID propertyId);

    /**
     * Find deposit contracts by agent
     */
    List<DepositContract> findAllByAgent_Id(UUID agentId);

    /**
     * Find deposit contracts by customer
     */
    List<DepositContract> findAllByCustomer_Id(UUID customerId);

    /**
     * Find deposit contract with all related entities eagerly loaded
     */
    @Query("SELECT d FROM DepositContract d " +
            "LEFT JOIN FETCH d.property p " +
            "LEFT JOIN FETCH p.owner o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH d.customer c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH d.agent a " +
            "LEFT JOIN FETCH a.user " +
            "LEFT JOIN FETCH d.payments " +
            "WHERE d.id = :contractId")
    Optional<DepositContract> findByIdWithDetails(@Param("contractId") UUID contractId);
}
