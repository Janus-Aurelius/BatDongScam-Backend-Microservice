package com.se100.bds.models.schemas.search;

import com.se100.bds.models.schemas.AbstractBaseMongoSchema;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.UUID;

@Document(collection = "search_logs")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchLog extends AbstractBaseMongoSchema {
    @Field("user_id")
    private UUID userId;

    @Field("city_id")
    private UUID cityId;

    @Field("district_id")
    private UUID districtId;

    @Field("ward_id")
    private UUID wardId;

    @Field("property_id")
    private UUID propertyId;

    @Field("property_type_id")
    private UUID propertyTypeId;
}
