package com.se100.bds.repositories.domains.contract;

import com.se100.bds.models.entities.contract.Contract;
import com.se100.bds.utils.Constants;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID>, JpaSpecificationExecutor<Contract> {
    List<Contract> findAllByAgent_Id(UUID agentId);
    List<Contract> findAllByCustomer_Id(UUID customerId);

    @Query("SELECT COUNT(c) FROM Contract c WHERE YEAR(c.signedAt) = :year AND MONTH(c.signedAt) = :month AND c.signedAt IS NOT NULL")
    int countSignedInMonth(@Param("month") int month, @Param("year") int year);

    @Query("SELECT c FROM Contract c")
    @EntityGraph(attributePaths = {"customer", "customer.user"})
    List<Contract> findAllWithCustomerAndUser();

    List<Contract> findAllByProperty_IdAndStartDateAfterAndEndDateBeforeAndContractTypeNot(UUID propertyId, LocalDate startDateAfter, LocalDate endDateBefore, Constants.ContractTypeEnum contractType);
}
