package com.se100.bds.repositories.domains.mongo.ranking;

import com.se100.bds.models.schemas.ranking.IndividualCustomerPotentialMonth;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IndividualCustomerPotentialMonthRepository extends MongoRepository<IndividualCustomerPotentialMonth, String> {
    /**
     * Find customer potential records by customer ID
     */
    List<IndividualCustomerPotentialMonth> findByCustomerId(UUID customerId);

    /**
     * Find customer potential record by customer ID, month and year
     */
    IndividualCustomerPotentialMonth findByCustomerIdAndMonthAndYear(UUID customerId, Integer month, Integer year);

    /**
     * Find all customer potential records by month and year
     */
    List<IndividualCustomerPotentialMonth> findAllByMonthAndYear(Integer month, Integer year);
}
