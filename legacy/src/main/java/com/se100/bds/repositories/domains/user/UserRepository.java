package com.se100.bds.repositories.domains.user;

import com.se100.bds.models.entities.user.User;
import com.se100.bds.repositories.dtos.SaleAgentCardProjection;
import com.se100.bds.utils.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phone);

    boolean existsByEmail(String email);

    List<User> findAllByRole(Constants.RoleEnum role);

    @EntityGraph(attributePaths = {"ward", "ward.district", "ward.district.city"})
    @Query("SELECT u FROM User u")
    Page<User> findAllWithLocation(Pageable pageable);

    @EntityGraph(attributePaths = {"ward", "ward.district", "ward.district.city"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLocation(@Param("id") UUID id);

    @Query("SELECT u FROM User u WHERE LOWER(CAST(CONCAT(u.lastName, ' ', u.firstName) AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))")
    List<User> findAllByFullNameIsLikeIgnoreCase(String name);

    @Query("SELECT u FROM User u WHERE LOWER(CAST(CONCAT(u.lastName, ' ', u.firstName) AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')) AND u.role = :role")
    List<User> findAllByFullNameIsLikeIgnoreCaseAndRole(@Param("name") String name, @Param("role") Constants.RoleEnum role);

    @EntityGraph(attributePaths = {"ward", "ward.district", "ward.district.city"})
    @Query("""
        SELECT u
        FROM User u
        JOIN SaleAgent sa ON sa.user.id = u.id
        JOIN Ward w ON u.ward.id = w.id
        JOIN District d ON w.district.id = d.id
        JOIN City c ON d.city.id = c.id
        WHERE u.role = com.se100.bds.utils.Constants.RoleEnum.SALESAGENT
            AND (COALESCE(:name, '') = '' OR LOWER(CAST(CONCAT(u.lastName, ' ', u.firstName) AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
            AND (:maxProperties IS NULL OR sa.maxProperties <= :maxProperties)
            AND (COALESCE(:cityIds, NULL) IS NULL OR c.id IN :cityIds)
            AND (COALESCE(:districtIds, NULL) IS NULL OR d.id IN :districtIds)
            AND (COALESCE(:wardIds, NULL) IS NULL OR w.id IN :wardIds)
        """)
    List<User> findAllSaleAgentWithFiltersTestHiredDate(
            @Param("name") String name,
            @Param("maxProperties") Integer maxProperties,
            @Param("cityIds") List<UUID> cityIds,
            @Param("districtIds") List<UUID> districtIds,
            @Param("wardIds") List<UUID> wardIds
    );

    @EntityGraph(attributePaths = {"ward", "ward.district", "ward.district.city"})
    @Query("""
        SELECT u
        FROM User u
        JOIN Customer c ON c.user.id = u.id
        JOIN Ward w ON u.ward.id = w.id
        JOIN District d ON w.district.id = d.id
        JOIN City ci ON d.city.id = ci.id
        WHERE u.role = com.se100.bds.utils.Constants.RoleEnum.CUSTOMER
            AND (COALESCE(:name, '') = '' OR LOWER(CAST(CONCAT(u.lastName, ' ', u.firstName) AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
            AND (COALESCE(:cityIds, NULL) IS NULL OR ci.id IN :cityIds)
            AND (COALESCE(:districtIds, NULL) IS NULL OR d.id IN :districtIds)
            AND (COALESCE(:wardIds, NULL) IS NULL OR w.id IN :wardIds)
        """)
    List<User> findAllByCustomerFullNameIsLikeIgnoreCaseAndRangeJoinedDateAndLocation(
            @Param("name") String name,
            @Param("cityIds") List<UUID> cityIds,
            @Param("districtIds") List<UUID> districtIds,
            @Param("wardIds") List<UUID> wardIds
    );

    @EntityGraph(attributePaths = {"ward", "ward.district", "ward.district.city"})
    @Query("""
        SELECT u
        FROM User u
        JOIN PropertyOwner po ON po.user.id = u.id
        JOIN Ward w ON u.ward.id = w.id
        JOIN District d ON w.district.id = d.id
        JOIN City ci ON d.city.id = ci.id
        WHERE u.role = com.se100.bds.utils.Constants.RoleEnum.PROPERTY_OWNER
            AND (COALESCE(:name, '') = '' OR LOWER(CAST(CONCAT(u.lastName, ' ', u.firstName) AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
            AND (COALESCE(:cityIds, NULL) IS NULL OR ci.id IN :cityIds)
            AND (COALESCE(:districtIds, NULL) IS NULL OR d.id IN :districtIds)
            AND (COALESCE(:wardIds, NULL) IS NULL OR w.id IN :wardIds)
        """)
    List<User> findAllByPropertyOwnerFullNameIsLikeIgnoreCaseAndRangeJoinedDateAndLocation(
            @Param("name") String name,
            @Param("cityIds") List<UUID> cityIds,
            @Param("districtIds") List<UUID> districtIds,
            @Param("wardIds") List<UUID> wardIds
    );

    List<User> findAllByRoleAndStatusIn(Constants.RoleEnum role, Collection<Constants.StatusProfileEnum> statuses);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND YEAR(u.createdAt) = :year AND MONTH(u.createdAt) = :month")
    Integer countByRoleAndCreatedAtYearAndMonth(@Param("role") Constants.RoleEnum role, @Param("year") int year, @Param("month") int month);

    Integer countByRole(Constants.RoleEnum role);

    @Query("""
    SELECT u.id
    FROM User u
    WHERE u.saleAgent IS NOT NULL
      AND u.role = com.se100.bds.utils.Constants.RoleEnum.SALESAGENT
      AND u.status NOT IN (com.se100.bds.utils.Constants.StatusProfileEnum.DELETED, com.se100.bds.utils.Constants.StatusProfileEnum.SUSPENDED)
""")
    List<UUID> findAllCurrentAgent();
}