package com.se100.bds.repositories.domains.mongo.customer;

import com.se100.bds.models.schemas.customer.CustomerPreferredCity;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPreferredCityRepository extends BaseCustomerPreferenceRepository<CustomerPreferredCity> {
}
