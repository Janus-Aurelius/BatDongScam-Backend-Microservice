package com.se100.bds.models.schemas.customer;

import com.se100.bds.models.schemas.AbstractBaseMongoSchema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractCustomerPreferenceMongoSchema extends AbstractBaseMongoSchema {
    @Field("customer_id")
    private UUID customerId;

    @Field("ref_id")
    private UUID refId;

    // Constructor chung cho tất cả các entity
    public AbstractCustomerPreferenceMongoSchema(UUID customerId, UUID refId) {
        super();
        this.customerId = customerId;
        this.refId = refId;
    }
}

