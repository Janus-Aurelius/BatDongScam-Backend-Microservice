package com.se100.bds.repositories.domains.location;

import com.se100.bds.models.entities.location.Ward;
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
public interface WardRepository extends JpaRepository<Ward, UUID>, JpaSpecificationExecutor<Ward> {
    List<Ward> findAllByDistrict_Id(UUID districtId);

    @Query("SELECT w FROM Ward w WHERE " +
            "(:keyWord IS NULL OR :keyWord = '' OR LOWER(CAST(w.wardName AS string)) LIKE LOWER(CONCAT('%', :keyWord, '%'))) AND " +
            "(:cityIds IS NULL OR w.district.city.id IN :cityIds) AND " +
            "(:districtIds IS NULL OR w.district.id IN :districtIds) AND " +
            "(:isActive IS NULL OR w.isActive = :isActive) AND " +
            "(:minAvgLandPrice IS NULL OR w.avgLandPrice >= :minAvgLandPrice) AND " +
            "(:maxAvgLandPrice IS NULL OR w.avgLandPrice <= :maxAvgLandPrice) AND " +
            "(:minArea IS NULL OR w.totalArea >= :minArea) AND " +
            "(:maxArea IS NULL OR w.totalArea <= :maxArea) AND " +
            "(:minPopulation IS NULL OR w.population >= :minPopulation) AND " +
            "(:maxPopulation IS NULL OR w.population <= :maxPopulation)")
    Page<Ward> findAllWithFilters(
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

    @EntityGraph(attributePaths = {"properties"})
    Optional<Ward> findById(UUID id);

    @Query("""
        SELECT w.id
        FROM Ward w
    """)
    List<UUID> getAllIds();

    @Query("""
        SELECT w.wardName
        FROM Ward w
        WHERE w.id = :wardId
    """)
    String getWardName(@Param("wardId") UUID wardId);
}
