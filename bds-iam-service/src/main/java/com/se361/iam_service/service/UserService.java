package com.se361.iam_service.service;

import com.se361.iam_service.dto.request.RegisterRequest;
import com.se361.iam_service.dto.request.UpdateAccountDto;
import com.se361.iam_service.dto.response.UserResponse;
import com.se361.iam_service.dto.response.listitem.CustomerListItem;
import com.se361.iam_service.dto.response.listitem.PropertyOwnerListItem;
import com.se361.iam_service.dto.response.listitem.SaleAgentListItem;
import com.se361.iam_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserService {
    User getCurrentUser();
    User findById(UUID id);
    User findByEmail(String email);

    // Me / profile
    UserResponse getMe();
    UserResponse getUserById(UUID id);
    UserResponse updateMe(UpdateAccountDto dto);
    UserResponse updateUserById(UUID id, UpdateAccountDto dto);

    // Delete
    void deleteMyAccount();
    void deleteAccountById(UUID id);

    // Register
    User register(RegisterRequest request);

    // Admin ops
    void approveAccount(UUID propOwnerId, boolean approve);
    User updateStatus(UUID id, String status);

    // List (phần ranking/tier sẽ là stub trả null — bổ sung sau khi có Ranking Service)
    Page<UserResponse> getAllByRole(String role, String name, Pageable pageable);

    // Internal helpers (dùng bởi các service khác trong IAM)
    List<UUID> getAllCurrentAgentIds();
    void activateUser(UUID id);

    // Advanced filtering
    Page<SaleAgentListItem> getAllSaleAgentItemsWithFilters(
            Pageable pageable,
            String name, Integer month, Integer year,
            List<com.se.bds.common.enums.PerformanceTierEnum> agentTiers, Integer maxProperties,
            Integer minPerformancePoint, Integer maxPerformancePoint,
            Integer minRanking, Integer maxRanking,
            Integer minAssignments, Integer maxAssignments,
            Integer minAssignedProperties, Integer maxAssignedProperties,
            Integer minAssignedAppointments, Integer maxAssignedAppointments,
            Integer minContracts, Integer maxContracts,
            Double minAvgRating, Double maxAvgRating,
            LocalDateTime hiredDateFrom, LocalDateTime hiredDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds
    );

    Page<CustomerListItem> getAllCustomerItemsWithFilters(
            Pageable pageable,
            String name, Integer month, Integer year,
            List<com.se.bds.common.enums.CustomerTierEnum> customerTiers,
            Integer minLeadingScore, Integer maxLeadingScore,
            Integer minViewings, Integer maxViewings,
            BigDecimal minSpending, BigDecimal maxSpending,
            Integer minContracts, Integer maxContracts,
            Integer minPropertiesBought, Integer maxPropertiesBought,
            Integer minPropertiesRented, Integer maxPropertiesRented,
            Integer minRanking, Integer maxRanking,
            LocalDateTime joinedDateFrom, LocalDateTime joinedDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds
    );

    Page<PropertyOwnerListItem> getAllPropertyOwnerItemsWithFilters(
            Pageable pageable,
            String name, Integer month, Integer year,
            List<com.se.bds.common.enums.ContributionTierEnum> ownerTiers,
            Integer minContributionPoint, Integer maxContributionPoint,
            Integer minProperties, Integer maxProperties,
            Integer minPropertiesForSale, Integer maxPropertiesForSale,
            Integer minPropertiesForRents, Integer maxPropertiesForRents,
            Integer minRanking, Integer maxRanking,
            LocalDateTime joinedDateFrom, LocalDateTime joinedDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds
    );
}
