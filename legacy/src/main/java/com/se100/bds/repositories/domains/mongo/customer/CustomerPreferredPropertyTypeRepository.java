package com.se100.bds.repositories.domains.mongo.customer;

import com.se100.bds.models.schemas.customer.CustomerPreferredPropertyType;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPreferredPropertyTypeRepository extends BaseCustomerPreferenceRepository<CustomerPreferredPropertyType> {
}
