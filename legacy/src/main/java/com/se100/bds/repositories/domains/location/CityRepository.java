package com.se100.bds.repositories.domains.location;

import com.se100.bds.models.entities.location.City;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CityRepository extends JpaRepository<City, UUID>, JpaSpecificationExecutor<City> {
    Page<City> findAllByIdIn(Collection<UUID> ids, Pageable pageable);

    @Query("SELECT c FROM City c WHERE " +
            "(:keyWord IS NULL OR :keyWord = '' OR LOWER(CAST(c.cityName AS string)) LIKE LOWER(CONCAT('%', :keyWord, '%'))) AND " +
            "(:cityIds IS NULL OR c.id IN :cityIds) AND " +
            "(:isActive IS NULL OR c.isActive = :isActive) AND " +
            "(:minAvgLandPrice IS NULL OR c.avgLandPrice >= :minAvgLandPrice) AND " +
            "(:maxAvgLandPrice IS NULL OR c.avgLandPrice <= :maxAvgLandPrice) AND " +
            "(:minArea IS NULL OR c.totalArea >= :minArea) AND " +
            "(:maxArea IS NULL OR c.totalArea <= :maxArea) AND " +
            "(:minPopulation IS NULL OR c.population >= :minPopulation) AND " +
            "(:maxPopulation IS NULL OR c.population <= :maxPopulation)")
    Page<City> findAllWithFilters(
            Pageable pageable,
            @Param("keyWord") String keyWord,
            @Param("cityIds") List<UUID> cityIds,
            @Param("isActive") Boolean isActive,
            @Param("minAvgLandPrice") BigDecimal minAvgLandPrice,
            @Param("maxAvgLandPrice") BigDecimal maxAvgLandPrice,
            @Param("minArea") BigDecimal minArea,
            @Param("maxArea") BigDecimal maxArea,
            @Param("minPopulation") Integer minPopulation,
            @Param("maxPopulation") Integer maxPopulation
    );

    @Query("SELECT COUNT(d) FROM District d WHERE d.city.id = :cityId")
    int countDistrictsByCityId(@Param("cityId") UUID cityId);

    @Query("SELECT COUNT(w) FROM Ward w WHERE w.district.city.id = :cityId")
    int countWardsByCityId(@Param("cityId") UUID cityId);

    @EntityGraph(attributePaths = {"districts", "districts.wards", "districts.wards.properties"})
    Optional<City> findById(UUID id);

    @Query("""
        SELECT c.id
        FROM City c
    """)
    List<UUID> getAllIds();

    @Query("""
        SELECT c.cityName
        FROM City c
        WHERE c.id = :cityId
    """)
    String getCityName(@Param("cityId") UUID cityId);

    @Query("""
        SELECT c.cityName
        FROM City c
        JOIN District d ON d.city.id = c.id
        WHERE d.id = :districtId
    """)
    String getCityNameByDistrictId(@Param("districtId") UUID districtId);

    @Query("""
        SELECT c.cityName
        FROM City c
        JOIN District d ON d.city.id = c.id
        JOIN Ward w ON w.district.id = d.id
        WHERE w.id = :wardId
    """)
    String getCityNameByWardId(@Param("wardId") UUID wardId);
}
