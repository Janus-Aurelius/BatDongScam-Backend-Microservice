package com.se100.bds.models.schemas.customer;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "customer_preferred_districts")
@Getter
@Setter
@NoArgsConstructor
public class CustomerPreferredDistrict extends AbstractCustomerPreferenceMongoSchema {
    public CustomerPreferredDistrict(UUID customerId, UUID refId) {
        super(customerId, refId);
    }
}
