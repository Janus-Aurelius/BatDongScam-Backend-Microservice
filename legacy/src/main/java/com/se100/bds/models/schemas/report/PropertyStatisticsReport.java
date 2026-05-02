package com.se100.bds.models.schemas.report;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;
import java.util.UUID;

@Document(collection = "property_statistic_reports")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyStatisticsReport extends AbstractBaseMongoReport {
    @Field("total_active_properties")
    private Integer totalActiveProperties;

    @Field("total_sold_properties")
    private Integer totalSoldProperties;

    @Field("total_rented_properties")
    private Integer totalRentedProperties;

    @Field("searched_cities")
    private Map<UUID, Integer> searchedCities;

    @Field("favorite_cities")
    private Map<UUID, Integer> favoriteCities;

    @Field("searched_districts")
    private Map<UUID, Integer> searchedDistricts;

    @Field("favorite_districts")
    private Map<UUID, Integer> favoriteDistricts;

    @Field("searched_wards")
    private Map<UUID, Integer> searchedWards;

    @Field("favorite_wards")
    private Map<UUID, Integer> favoriteWards;

    @Field("searched_property_types")
    private Map<UUID, Integer> searchedPropertyTypes;

    @Field("favorite_property_types")
    private Map<UUID, Integer> favoritePropertyTypes;

    @Field("searched_properties")
    private Map<UUID, Integer> searchedProperties;
}
