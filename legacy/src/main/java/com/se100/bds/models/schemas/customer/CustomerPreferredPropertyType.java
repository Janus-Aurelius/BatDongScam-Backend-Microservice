package com.se100.bds.models.schemas.customer;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "customer_preferred_property_types")
@Getter
@Setter
@NoArgsConstructor
public class CustomerPreferredPropertyType extends AbstractCustomerPreferenceMongoSchema {
    public CustomerPreferredPropertyType(UUID customerId, UUID refId) {
        super(customerId, refId);
    }
}
