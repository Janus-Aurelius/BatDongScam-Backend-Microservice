package com.se100.bds.repositories.domains.contract;

import com.se100.bds.models.entities.contract.PurchaseContract;
import com.se100.bds.utils.Constants.ContractStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseContractRepository extends JpaRepository<PurchaseContract, UUID>, JpaSpecificationExecutor<PurchaseContract> {

    /**
     * Check if there's an existing non-draft purchase contract for a property
     */
    @Query("SELECT COUNT(p) > 0 FROM PurchaseContract p WHERE p.property.id = :propertyId AND p.status <> :draftStatus")
    boolean existsNonDraftPurchaseContractForProperty(@Param("propertyId") UUID propertyId, @Param("draftStatus") ContractStatusEnum draftStatus);

    /**
     * Find purchase contract with all related entities eagerly loaded
     */
    @Query("SELECT p FROM PurchaseContract p " +
            "LEFT JOIN FETCH p.property prop " +
            "LEFT JOIN FETCH prop.owner o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH p.customer c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH p.agent a " +
            "LEFT JOIN FETCH a.user " +
            "LEFT JOIN FETCH p.depositContract " +
            "LEFT JOIN FETCH p.payments " +
            "WHERE p.id = :contractId")
    Optional<PurchaseContract> findByIdWithDetails(@Param("contractId") UUID contractId);
}

