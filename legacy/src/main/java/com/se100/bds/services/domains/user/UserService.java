package com.se100.bds.services.domains.user;

import com.se100.bds.dtos.requests.auth.RegisterRequest;
import com.se100.bds.dtos.requests.account.UpdateAccountDto;
import com.se100.bds.dtos.responses.user.listitem.CustomerListItem;
import com.se100.bds.dtos.responses.user.listitem.FreeAgentListItem;
import com.se100.bds.dtos.responses.user.listitem.PropertyOwnerListItem;
import com.se100.bds.dtos.responses.user.listitem.SaleAgentListItem;
import com.se100.bds.dtos.responses.user.meprofile.MeResponse;
import com.se100.bds.dtos.responses.user.otherprofile.UserProfileResponse;
import com.se100.bds.models.entities.user.SaleAgent;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.securities.JwtUserDetails;
import com.se100.bds.utils.Constants;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserService {
    User getUser();

    void approveAccount(UUID propOwnerId, boolean approve);

    List<User> getAllByName(String name);

    MeResponse<?> getAccount();

    MeResponse<?> getUserById(UUID userId);

    MeResponse<?> updateUserById(UUID userId, UpdateAccountDto updateAccountDto);

    MeResponse<?> updateMe(UpdateAccountDto updateAccountDto);

    void deleteAccountById(UUID userId);

    void deleteMyAccount();

    UUID getUserId();

    UserDetails loadUserById(String id);

    JwtUserDetails getPrincipal(Authentication authentication);

    Page<User> findAll(Pageable pageable);

    @NotNull User findById(UUID id);

    SaleAgent findSaleAgentById(UUID agentId);

    User updateStatus(UUID id, String status);

    User findByEmail(String email);

    UserDetails loadUserByEmail(String email);

    UserProfileResponse<?> getUserProfileById(UUID id);

    User register(RegisterRequest request, Constants.RoleEnum roleEnum) throws BindException, IOException;

    void delete(String id);

    void activeteUser(String id);

    /// ADMIN
    Page<SaleAgentListItem> getAllSaleAgentItemsWithFilters(
            Pageable pageable,
            String name, Integer month, Integer year,
            List<Constants.PerformanceTierEnum> agentTiers, Integer maxProperties,
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
            List<Constants.CustomerTierEnum> customerTierEnums,
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
            List<Constants.ContributionTierEnum> ownerTiers,
            Integer minContributionPoint, Integer maxContributionPoint,
            Integer minProperties, Integer maxProperties,
            Integer minPropertiesForSale, Integer maxPropertiesForSale,
            Integer minPropertiesForRents, Integer maxPropertiesForRents,
            Integer minRanking, Integer maxRanking,
            LocalDateTime joinedDateFrom, LocalDateTime joinedDateTo,
            List<UUID> cityIds, List<UUID> districtIds, List<UUID> wardIds
    );
    Page<FreeAgentListItem> getAllFreeAgentItemsWithFilters(
            Pageable pageable,
            String agentNameOrCode,
            List<Constants.PerformanceTierEnum> agentTiers,
            Integer minAssignedAppointments, Integer maxAssignedAppointments,
            Integer minAssignedProperties, Integer maxAssignedProperties,
            Integer minCurrentlyHandle, Integer maxCurrentlyHandle
    );

    /// Internal
    List<User> findAllByNameAndRole(String name, Constants.RoleEnum roleEnum);
    List<User> findAllByRoleAndStillAvailable(Constants.RoleEnum roleEnum);
    Integer countNewUsersByRoleAndMonthAndYear(Constants.RoleEnum roleEnum, int month, int year);
    List<UUID> getAllCurrentAgentIds();
}