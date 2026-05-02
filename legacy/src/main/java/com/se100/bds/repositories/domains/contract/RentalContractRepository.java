package com.se100.bds.repositories.domains.contract;

import com.se100.bds.models.entities.contract.RentalContract;
import com.se100.bds.utils.Constants.ContractStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalContractRepository extends JpaRepository<RentalContract, UUID>, JpaSpecificationExecutor<RentalContract> {

    /**
     * Check if there's an existing non-draft rental contract for a property
     */
    @Query("SELECT COUNT(r) > 0 FROM RentalContract r WHERE r.property.id = :propertyId AND r.status <> :draftStatus")
    boolean existsNonDraftRentalContractForProperty(@Param("propertyId") UUID propertyId, @Param("draftStatus") ContractStatusEnum draftStatus);

    /**
     * Find rental contract with all related entities eagerly loaded
     */
    @Query("SELECT r FROM RentalContract r " +
            "LEFT JOIN FETCH r.property prop " +
            "LEFT JOIN FETCH prop.owner o " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH r.customer c " +
            "LEFT JOIN FETCH c.user " +
            "LEFT JOIN FETCH r.agent a " +
            "LEFT JOIN FETCH a.user " +
            "LEFT JOIN FETCH r.depositContract " +
            "LEFT JOIN FETCH r.payments " +
            "WHERE r.id = :contractId")
    Optional<RentalContract> findByIdWithDetails(@Param("contractId") UUID contractId);
}

