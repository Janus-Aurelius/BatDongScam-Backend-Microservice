package com.se100.bds.repositories.domains.location;

import com.se100.bds.models.entities.location.District;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DistrictRepository extends JpaRepository<District, UUID>, JpaSpecificationExecutor<District> {
    List<District> findAllByCity_Id(UUID cityId);

    @Query("SELECT d FROM District d WHERE " +
            "(:keyWord IS NULL OR :keyWord = '' OR LOWER(CAST(d.districtName AS string)) LIKE LOWER(CONCAT('%', :keyWord, '%'))) AND " +
            "(:cityIds IS NULL OR d.city.id IN :cityIds) AND " +
            "(:districtIds IS NULL OR d.id IN :districtIds) AND " +
            "(:isActive IS NULL OR d.isActive = :isActive) AND " +
            "(:minAvgLandPrice IS NULL OR d.avgLandPrice >= :minAvgLandPrice) AND " +
            "(:maxAvgLandPrice IS NULL OR d.avgLandPrice <= :maxAvgLandPrice) AND " +
            "(:minArea IS NULL OR d.totalArea >= :minArea) AND " +
            "(:maxArea IS NULL OR d.totalArea <= :maxArea) AND " +
            "(:minPopulation IS NULL OR d.population >= :minPopulation) AND " +
            "(:maxPopulation IS NULL OR d.population <= :maxPopulation)")
    Page<District> findAllWithFilters(
            Pageable pageable,
            @Param("keyWord") String keyWord,
            @Param("cityIds") List<UUID> cityIds,
            @Param("districtIds") List<UUID> districtIds,
            @Param("isActive") Boolean isActive,
            @Param("minAvgLandPrice") BigDecimal minAvgLandPrice,
            @Param("maxAvgLandPrice") BigDecimal maxAvgLandPrice,
            @Param("minArea") BigDecimal minArea,
            @Param("maxArea") BigDecimal maxArea,
            @Param("minPopulation") Integer minPopulation,
            @Param("maxPopulation") Integer maxPopulation
    );

    @Query("SELECT COUNT(w) FROM Ward w WHERE w.district.id = :districtId")
    int countWardsByDistrictId(@Param("districtId") UUID districtId);

    @EntityGraph(attributePaths = {"wards", "wards.properties"})
    Optional<District> findById(UUID id);

    @Query("""
        SELECT d.id
        FROM District d
    """)
    List<UUID> getAllIds();

    @Query("""
        SELECT d.districtName
        FROM District d
        WHERE d.id = :districtId
    """)
    String getDistrictName(@Param("districtId") UUID districtId);

    @Query("""
        SELECT d.districtName
        FROM District d
        JOIN Ward w ON w.district.id = d.id
        WHERE w.id = :wardId
    """)
    String getDistrictNameByWardId(@Param("wardId") UUID wardId);
}
