package microservices.appointmentservice.repositories;

import microservices.appointmentservice.entities.property.Property;
import microservices.appointmentservice.entities.user.PropertyOwner;
import microservices.appointmentservice.repositories.dtos.DocumentProjection;
import microservices.appointmentservice.repositories.dtos.MediaProjection;
import microservices.appointmentservice.repositories.dtos.PropertyCardProtection;
import microservices.appointmentservice.repositories.dtos.PropertyDetailsProjection;
import microservices.appointmentservice.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    @Query("""
    SELECT new microservices.appointmentservice.repositories.dtos.PropertyCardProtection (
        p.id,
        p.createdAt,
        p.updatedAt,
        p.transactionType,
        p.title,
        MIN(m.filePath),
        true,
        COALESCE(CAST(COUNT(DISTINCT m.id) AS int), 0),
        p.fullAddress,
        d.districtName,
        c.cityName,
        CAST(p.status AS string),
        p.priceAmount,
        p.area,
        po.id,
        u.firstName,
        u.lastName,
        sa.id,
        u2.firstName,
        u2.lastName
    )
    FROM Property p
    JOIN Ward w ON p.wardId = w.id
    JOIN District d ON w.district.id = d.id
    JOIN City c ON d.city.id = c.id
    LEFT JOIN PropertyOwner po ON p.ownerId = po.id
    LEFT JOIN SaleAgent sa ON p.assignedAgentId = sa.id
    LEFT JOIN User u ON po.id = u.id
    LEFT JOIN User u2 ON sa.id = u2.id
    LEFT JOIN Media m ON m.property.id = p.id
    WHERE p.id IN :propertyIds
    GROUP BY
        p.id,
        p.createdAt,
        p.updatedAt,
        p.transactionType,
        p.title,
        p.fullAddress,
        d.districtName,
        c.cityName,
        p.status,
        p.priceAmount,
        p.area,
        po.id,
        u.firstName,
        u.lastName,
        sa.id,
        u2.firstName,
        u2.lastName
    """)
    Page<PropertyCardProtection> findFavoritePropertyCards(
            Pageable pageable,
            @Param("propertyIds") List<UUID> propertyIds);

    @Query("""
    SELECT new microservices.appointmentservice.repositories.dtos.PropertyCardProtection (
        p.id,
        p.createdAt,
        p.updatedAt,
        p.transactionType,
        p.title,
        MIN(m.filePath),
        false,
        COALESCE(CAST(COUNT(DISTINCT m.id) AS int), 0),
        p.fullAddress,
        d.districtName,
        c.cityName,
        CAST(p.status AS string),
        p.priceAmount,
        p.area,
        po.id,
        u.firstName,
        u.lastName,
        sa.id,
        u2.firstName,
        u2.lastName
    )
    FROM Property p
    JOIN Ward w ON p.wardId = w.id
    JOIN District d ON w.district.id = d.id
    JOIN City c ON d.city.id = c.id
    LEFT JOIN PropertyOwner po ON p.ownerId = po.id
    LEFT JOIN SaleAgent sa ON p.assignedAgentId = sa.id
    LEFT JOIN User u ON po.id = u.id
    LEFT JOIN User u2 ON sa.id = u2.id
    LEFT JOIN Media m ON m.property.id = p.id
    WHERE
        (COALESCE(:propertyIds, NULL) IS NULL OR p.id IN :propertyIds)
        AND (COALESCE(:cityIds, NULL) IS NULL OR c.id IN :cityIds)
        AND (COALESCE(:districtIds, NULL) IS NULL OR d.id IN :districtIds)
        AND (COALESCE(:wardIds, NULL) IS NULL OR w.id IN :wardIds)
        AND (COALESCE(:propertyTypeIds, NULL) IS NULL OR p.propertyType.id IN :propertyTypeIds)
        AND (COALESCE(:ownerIds, NULL) IS NULL OR po.id IN :ownerIds)
        AND (COALESCE(:agentIds, NULL) IS NULL OR sa.id IN :agentIds)
        AND (:minPrice IS NULL OR p.priceAmount >= :minPrice)
        AND (:maxPrice IS NULL OR p.priceAmount <= :maxPrice)
        AND (:minArea IS NULL OR p.area >= :minArea)
        AND (:maxArea IS NULL OR p.area <= :maxArea)
        AND (:rooms IS NULL OR p.rooms = :rooms)
        AND (:bathrooms IS NULL OR p.bathrooms = :bathrooms)
        AND (:bedrooms IS NULL OR p.bedrooms = :bedrooms)
        AND (:floors IS NULL OR p.floors = :floors)
        AND (:houseOrientation IS NULL OR CAST(p.houseOrientation AS string) = :houseOrientation)
        AND (:balconyOrientation IS NULL OR CAST(p.balconyOrientation AS string) = :balconyOrientation)
        AND (COALESCE(:transactionType, NULL) IS NULL OR CAST(p.transactionType AS string) IN :transactionType)
        AND (:statuses IS NULL OR CAST(p.status AS string) IN :statuses)
    GROUP BY 
        p.id, 
        p.createdAt, 
        p.updatedAt, 
        p.transactionType, 
        p.title, 
        p.fullAddress, 
        d.districtName, 
        c.cityName, 
        p.status, 
        p.priceAmount, 
        p.area, 
        po.id,
        u.firstName,
        u.lastName,
        sa.id,
        u2.firstName,
        u2.lastName
    """)
    Page<PropertyCardProtection> findAllPropertyCardsWithFilter(
            Pageable pageable,
            @Param("propertyIds") List<UUID> propertyIds,
            @Param("cityIds") List<UUID> cityIds,
            @Param("districtIds") List<UUID> districtIds,
            @Param("wardIds") List<UUID> wardIds,
            @Param("propertyTypeIds") List<UUID> propertyTypeIds,
            @Param("ownerIds") List<UUID> ownerIds,
            @Param("agentIds") List<UUID> agentIds,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minArea") BigDecimal minArea,
            @Param("maxArea") BigDecimal maxArea,
            @Param("rooms") Integer rooms,
            @Param("bathrooms") Integer bathrooms,
            @Param("bedrooms") Integer bedrooms,
            @Param("floors") Integer floors,
            @Param("houseOrientation") String houseOrientation,
            @Param("balconyOrientation") String balconyOrientation,
            @Param("transactionType") List<String> transactionType,
            @Param("statuses") List<String> statuses
    );

    @Query("""
        SELECT
            p.id AS id,
            p.createdAt AS createdAt,
            p.updatedAt AS updatedAt,
            po.id AS ownerId,
            u1.firstName AS ownerFirstName,
            u1.lastName AS ownerLastName,
            u1.phoneNumber AS ownerPhoneNumber,
            u1.zaloContact AS ownerZaloContact,
            u1.createdAt AS ownerCreatedAt,
            u1.updatedAt AS ownerUpdatedAt,
            sa.id AS agentId,
            u2.firstName AS agentFirstName,
            u2.lastName AS agentLastName,
            u2.phoneNumber AS agentPhoneNumber,
            u2.zaloContact AS agentZaloContact,
            u2.createdAt AS agentCreatedAt,
            u2.updatedAt AS agentUpdatedAt,
            p.serviceFeeAmount AS serviceFeeAmount,
            p.serviceFeeCollectedAmount AS serviceFeeCollectedAmount,
            pt.id AS propertyTypeId,
            pt.typeName AS propertyTypeName,
            w.id AS wardId,
            w.wardName AS wardName,
            d.id AS districtId,
            d.districtName AS districtName,
            c.id AS cityId,
            c.cityName AS cityName,
            p.title AS title,
            p.description AS description,
            CAST(p.transactionType AS string) AS transactionType,
            p.fullAddress AS fullAddress,
            p.area AS area,
            p.rooms AS rooms,
            p.bathrooms AS bathrooms,
            p.floors AS floors,
            p.bedrooms AS bedrooms,
            CAST(p.houseOrientation AS string) AS houseOrientation,
            CAST(p.balconyOrientation AS string) AS balconyOrientation,
            p.yearBuilt AS yearBuilt,
            p.priceAmount AS priceAmount,
            p.pricePerSquareMeter AS pricePerSquareMeter,
            p.commissionRate AS commissionRate,
            p.amenities AS amenities,
            CAST(p.status AS string) AS status,
            p.viewCount AS viewCount,
            p.approvedAt AS approvedAt
        FROM Property p
        JOIN PropertyOwner po ON p.ownerId = po.id
        JOIN User u1 ON po.id = u1.id
        LEFT JOIN SaleAgent sa ON p.assignedAgentId = sa.id
        LEFT JOIN User u2 ON sa.id = u2.id
        JOIN PropertyType pt ON p.propertyType.id = pt.id
        JOIN Ward w ON p.wardId = w.id
        JOIN District d ON w.district.id = d.id
        JOIN City c ON d.city.id = c.id
        WHERE p.id = :propertyId
    """)
    PropertyDetailsProjection findPropertyDetailsById(@Param("propertyId") UUID propertyId);

    @Query("""
        SELECT
            m.id AS id,
            m.createdAt AS createdAt,
            m.updatedAt AS updatedAt,
            CAST(m.mediaType AS string) AS mediaType,
            m.fileName AS fileName,
            m.filePath AS filePath,
            m.mimeType AS mimeType
        FROM Media m
        WHERE m.property.id = :propertyId
        ORDER BY m.createdAt ASC
    """)
    List<MediaProjection> findMediaByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("""
        SELECT
            d.id AS id,
            d.createdAt AS createdAt,
            d.updatedAt AS updatedAt,
            dt.id AS documentTypeId,
            dt.name AS documentTypeName,
            d.documentNumber AS documentNumber,
            d.documentName AS documentName,
            d.filePath AS filePath,
            d.issueDate AS issueDate,
            d.expiryDate AS expiryDate,
            d.issuingAuthority AS issuingAuthority,
            CAST(d.verificationStatus AS string) AS verificationStatus,
            d.verifiedAt AS verifiedAt,
            d.rejectionReason AS rejectionReason
        FROM IdentificationDocument d
        JOIN DocumentType dt ON d.documentType.id = dt.id
        WHERE d.property.id = :propertyId
        ORDER BY d.createdAt ASC
    """)
    List<DocumentProjection> findDocumentsByPropertyId(@Param("propertyId") UUID propertyId);

    List<Property> findAllByOwnerId(UUID ownerId);

    List<Property> findAllByOwnerIdAndAssignedAgentId(UUID ownerId, UUID assignedAgentId);

    @Query("""
    SELECT DISTINCT p
    FROM Property p
    JOIN Contract c ON c.property.id = p.id
    WHERE c.customer.id = :customerId
    """)
    List<Property> findAllByCustomer_Id(@Param("customerId") UUID customerId);

    @Query("""
    SELECT DISTINCT p
    FROM Property p
    JOIN Contract c ON c.property.id = p.id
    WHERE c.customer.id = :customerId
        AND (c.status = microservices.appointmentservice.utils.Constants.ContractStatusEnum.ACTIVE
            OR c.status = microservices.appointmentservice.utils.Constants.ContractStatusEnum.COMPLETED)
    """)
    List<Property> findAllByCustomer_IdAndStatusIn(@Param("customerId") UUID customerId);

    @EntityGraph(attributePaths = {"mediaList", "propertyType"})
    List<Property> findAllByAssignedAgentId(UUID assignedAgentId);

    @EntityGraph(attributePaths = {"mediaList", "propertyType"})
    Page<Property> findAllByAssignedAgentId(UUID assignedAgentId, Pageable pageable);

    @Query("""
    SELECT p
    FROM Property p
    WHERE p.ownerId = :ownerId
        AND (COALESCE(:statuses, NULL) IS NULL OR p.status IN :statuses)
    """)
    List<Property> findAllByOwnerIdAndStatusIn(@Param("ownerId") UUID ownerId, @Param("statuses") Collection<Constants.PropertyStatusEnum> statuses);

    @Query("""
    SELECT p
    FROM Property p
    WHERE p.assignedAgentId = :assignedAgentId
        AND (COALESCE(:statuses, NULL) IS NULL OR p.status IN :statuses)
    """)
    List<Property> findAllByAssignedAgentIdAndStatusIn(@Param("assignedAgentId") UUID assignedAgentId, @Param("statuses") Collection<Constants.PropertyStatusEnum> statuses);

    @Query("""
    SELECT p
    FROM Property p
    WHERE p.ownerId = :ownerId
        AND p.assignedAgentId = :assignedAgentId
        AND (COALESCE(:statuses, NULL) IS NULL OR p.status IN :statuses)
    """)
    List<Property> findAllByOwnerIdAndAssignedAgentIdAndStatusIn(@Param("ownerId") UUID ownerId, @Param("assignedAgentId") UUID assignedAgentId, @Param("statuses") Collection<Constants.PropertyStatusEnum> statuses);

    @Query("SELECT COUNT(p) FROM Property p WHERE p.wardId = :wardId AND p.status = microservices.appointmentservice.utils.Constants.PropertyStatusEnum.AVAILABLE")
    int countActivePropertiesByWardId(@Param("wardId") UUID wardId);

    @Query("SELECT COUNT(p) FROM Property p JOIN Ward w ON p.wardId = w.id WHERE w.district.id = :districtId AND p.status = microservices.appointmentservice.utils.Constants.PropertyStatusEnum.AVAILABLE")
    int countActivePropertiesByDistrictId(@Param("districtId") UUID districtId);

    @Query("SELECT COUNT(p) FROM Property p JOIN Ward w ON p.wardId = w.id JOIN District d ON w.district.id = d.id WHERE d.city.id = :cityId AND p.status = microservices.appointmentservice.utils.Constants.PropertyStatusEnum.AVAILABLE")
    int countActivePropertiesByCityId(@Param("cityId") UUID cityId);

    @Query("SELECT COUNT(p) FROM Property p WHERE p.propertyType.id = :propertyTypeId")
    int countByPropertyType_Id(@Param("propertyTypeId") UUID propertyTypeId);

    Long countByAssignedAgentId(UUID assignedAgentId);

    @EntityGraph(attributePaths = {"mediaList", "propertyType"})
    Page<Property> findAllByOwnerIdInAndAssignedAgentId(Collection<UUID> ownerIds, UUID assignedAgentId, Pageable pageable);

    @EntityGraph(attributePaths = {"propertyType"})
    Optional<Property> findById(UUID propertyId);

    List<Property> findAllByCreatedAtBefore(LocalDateTime createdAtBefore);
}
