package com.se100.bds.repositories.domains.mongo.ranking;

import com.se100.bds.models.schemas.ranking.IndividualCustomerPotentialAll;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IndividualCustomerPotentialAllRepository extends MongoRepository<IndividualCustomerPotentialAll, String> {

    IndividualCustomerPotentialAll findByCustomerId(UUID customerId);
}

