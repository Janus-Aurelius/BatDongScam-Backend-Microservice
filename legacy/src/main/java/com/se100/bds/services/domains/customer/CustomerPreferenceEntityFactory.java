package com.se100.bds.services.domains.customer;

import com.se100.bds.models.schemas.customer.AbstractCustomerPreferenceMongoSchema;
import com.se100.bds.models.schemas.customer.*;
import com.se100.bds.utils.Constants;

import java.util.UUID;
import java.util.function.BiFunction;

public class CustomerPreferenceEntityFactory {

    public static AbstractCustomerPreferenceMongoSchema createEntity(
            Constants.LikeTypeEnum likeType,
            UUID customerId,
            UUID refId) {

        BiFunction<UUID, UUID, AbstractCustomerPreferenceMongoSchema> constructor = switch (likeType) {
            case PROPERTY -> CustomerFavoriteProperty::new;
            case CITY -> CustomerPreferredCity::new;
            case DISTRICT -> CustomerPreferredDistrict::new;
            case WARD -> CustomerPreferredWard::new;
            case PROPERTY_TYPE -> CustomerPreferredPropertyType::new;
        };

        return constructor.apply(customerId, refId);
    }
}

