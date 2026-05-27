package com.se.bds.core.property.internal.adapter.out.persistence;

import com.se.bds.core.property.internal.domain.model.Property;
import com.se.bds.core.property.internal.domain.model.PropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface JpaPropertyRepository extends JpaRepository<Property, UUID> {
    @Query("SELECT COUNT (p) FROM Property p where p.assignedAgentId =:agentId")
    int countByAssignedAgentId(@Param("agentId") UUID agentId);

    @Query("select count(p) from Property p where p.propertyType.id = :propertyTypeId")
    int countByPropertyTypeId(@Param("propertyTypeId") UUID propertyTypeId);

    @EntityGraph(attributePaths = {"propertyType", "mediaList"})
    List<Property> findAllByOwnerId(UUID onwnerId);

    @EntityGraph(attributePaths = {"propertyType", "mediaList"})
    List<Property> findAllByAssignedAgentId(UUID agentId);

    @Query("""
    SELECT p from Property p
    where (coalesce(:cityIds, NULL) IS NULL or p.ward.district.city.id in :cityIds )
    and (coalesce(:districtIds, NULL) IS Null or p.ward.district.id in :districtIds)
    and (coalesce(:wardIds,null) is null or p.ward.id in :wardIds )
    and (coalesce(:propertyTypeIds, null) is null or p.propertyType.id in :propertyTypeIds )
    and (coalesce(:ownerId, null) is null or p.ownerId = :ownerId )
    and (coalesce(:agentId, null) is null or p.assignedAgentId = :agentId )
    and (:minPrice  is null or p.priceAmount >= :minPrice)
    and (:maxPrice is null or p.priceAmount <= :maxPrice)
    and (:minArea is null or p.area >= :minArea)
    and (:maxArea is null or p.area <= :maxArea)
""")
    Page<Property> searchWithFilters(
            @Param("cityIds") List<UUID> cityIds, @Param("districtIds") List<UUID> districtIds,
            @Param("wardIds") List<UUID> wardIds, @Param("propertyTypeIds") List<UUID> propertyTypeIds,
            @Param("ownerId") UUID ownerId, @Param("agentId") UUID agentId,
            @Param("minPrice")BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice,
            @Param("minArea") BigDecimal minArea, @Param("maxArea") BigDecimal maxArea,
            Pageable pageable
            );
}

    interface  JpaPropertyTypeRepository extends JpaRepository<PropertyType, UUID> {
    @Query("Select pt.id from PropertyType pt where pt.isActive = true")
        List<UUID> findAllActiveIds();
    }
