package com.se361.iam_service.repository;

import com.se361.iam_service.entity.User;
import com.se361.iam_service.util.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phone);
    boolean existsByEmail(String email);
    List<User> findAllByRole(Constants.RoleEnum role);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(CONCAT(u.lastName, ' ', u.firstName)) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findAllByFullNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role " +
            "AND YEAR(u.createdAt) = :year AND MONTH(u.createdAt) = :month")
    Integer countByRoleAndYearAndMonth(
            @Param("role") Constants.RoleEnum role,
            @Param("year") int year,
            @Param("month") int month
    );

    Integer countByRole(Constants.RoleEnum role);

    @Query("SELECT u.id FROM User u WHERE u.saleAgent IS NOT NULL " +
            "AND u.role = com.se361.iam_service.util.Constants.RoleEnum.SALESAGENT " +
            "AND u.status NOT IN (" +
            "  com.se361.iam_service.util.Constants.StatusProfileEnum.DELETED, " +
            "  com.se361.iam_service.util.Constants.StatusProfileEnum.SUSPENDED" +
            ")")
    List<UUID> findAllCurrentAgentIds();

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
            "LOWER(CONCAT(u.lastName, ' ', u.firstName)) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<User> findAllByRoleAndFullNameContaining(
            @Param("role") Constants.RoleEnum role,
            @Param("name") String name,
            Pageable pageable
    );

    Page<User> findAllByRole(Constants.RoleEnum role, Pageable pageable);
}
