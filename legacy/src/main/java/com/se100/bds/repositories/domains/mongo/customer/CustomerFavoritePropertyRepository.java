package com.se100.bds.repositories.domains.mongo.customer;

import com.se100.bds.models.schemas.customer.CustomerFavoriteProperty;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerFavoritePropertyRepository extends BaseCustomerPreferenceRepository<CustomerFavoriteProperty> {
}
