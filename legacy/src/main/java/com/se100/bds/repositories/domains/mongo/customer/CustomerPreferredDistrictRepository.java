package com.se100.bds.repositories.domains.mongo.customer;

import com.se100.bds.models.schemas.customer.CustomerPreferredDistrict;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPreferredDistrictRepository extends BaseCustomerPreferenceRepository<CustomerPreferredDistrict> {
}
