package com.se361.iam_service.controller;

import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.dto.PagedData;
import com.se361.iam_service.controller.base.AbstractBaseController;
import com.se361.iam_service.dto.request.UpdateAccountDto;
import com.se361.iam_service.dto.response.UserResponse;
import com.se361.iam_service.dto.response.listitem.CustomerListItem;
import com.se361.iam_service.dto.response.listitem.PropertyOwnerListItem;
import com.se361.iam_service.dto.response.listitem.SaleAgentListItem;
import com.se361.iam_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController extends AbstractBaseController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return responseFactory.successSingle(userService.getMe(), "OK");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        return responseFactory.successSingle(userService.getUserById(userId), "OK");
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @ModelAttribute UpdateAccountDto dto
    ) {
        return responseFactory.successSingle(userService.updateMe(dto), "Updated");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateUserById(
            @PathVariable UUID userId,
            @ModelAttribute UpdateAccountDto dto
    ) {
        return responseFactory.successSingle(userService.updateUserById(userId, dto), "Updated");
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
        userService.deleteMyAccount();
        return responseFactory.successSingle(null, "Deleted");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable UUID userId) {
        userService.deleteAccountById(userId);
        return responseFactory.successSingle(null, "Deleted");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{propOwnerId}/{approve}/approve")
    public ResponseEntity<ApiResponse<Void>> approveAccount(
            @PathVariable UUID propOwnerId,
            @PathVariable Boolean approve
    ) {
        userService.approveAccount(propOwnerId, approve);
        return responseFactory.successSingle(null, approve ? "Approved" : "Rejected");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedData<UserResponse>>> getUsersByRole(
            @RequestParam String role,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int limit,
            @RequestParam(defaultValue = "desc") String sortType,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<UserResponse> result = userService.getAllByRole(role, name, pageable);
        return responseFactory.successPage(result, "OK");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/sale-agents")
    public ResponseEntity<ApiResponse<PagedData<SaleAgentListItem>>> getAllSaleAgents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int limit,
            @RequestParam(defaultValue = "desc") String sortType,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<com.se.bds.common.enums.PerformanceTierEnum> agentTiers,
            @RequestParam(required = false) Integer maxProperties,
            @RequestParam(required = false) Integer minPerformancePoint,
            @RequestParam(required = false) Integer maxPerformancePoint,
            @RequestParam(required = false) Integer minRanking,
            @RequestParam(required = false) Integer maxRanking,
            @RequestParam(required = false) Integer minAssignments,
            @RequestParam(required = false) Integer maxAssignments,
            @RequestParam(required = false) Integer minAssignedProperties,
            @RequestParam(required = false) Integer maxAssignedProperties,
            @RequestParam(required = false) Integer minAssignedAppointments,
            @RequestParam(required = false) Integer maxAssignedAppointments,
            @RequestParam(required = false) Integer minContracts,
            @RequestParam(required = false) Integer maxContracts,
            @RequestParam(required = false) Double minAvgRating,
            @RequestParam(required = false) Double maxAvgRating,
            @RequestParam(required = false) LocalDateTime hiredDateFrom,
            @RequestParam(required = false) LocalDateTime hiredDateTo,
            @RequestParam(required = false) List<UUID> cityIds,
            @RequestParam(required = false) List<UUID> districtIds,
            @RequestParam(required = false) List<UUID> wardIds
    ) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        List<UUID> filteredCityIds = (cityIds != null && cityIds.isEmpty()) ? null : cityIds;
        List<UUID> filteredDistrictIds = (districtIds != null && districtIds.isEmpty()) ? null : districtIds;
        List<UUID> filteredWardIds = (wardIds != null && wardIds.isEmpty()) ? null : wardIds;

        Page<SaleAgentListItem> agentPage = userService.getAllSaleAgentItemsWithFilters(
                pageable, name, month, year, agentTiers, maxProperties,
                minPerformancePoint, maxPerformancePoint,
                minRanking, maxRanking,
                minAssignments, maxAssignments,
                minAssignedProperties, maxAssignedProperties,
                minAssignedAppointments, maxAssignedAppointments,
                minContracts, maxContracts,
                minAvgRating, maxAvgRating,
                hiredDateFrom, hiredDateTo,
                filteredCityIds, filteredDistrictIds, filteredWardIds
        );
        return responseFactory.successPage(agentPage, "Sale agents retrieved successfully");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SALESAGENT')")
    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<PagedData<CustomerListItem>>> getAllCustomers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int limit,
            @RequestParam(defaultValue = "desc") String sortType,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<com.se.bds.common.enums.CustomerTierEnum> customerTiers,
            @RequestParam(required = false) Integer minLeadingScore,
            @RequestParam(required = false) Integer maxLeadingScore,
            @RequestParam(required = false) Integer minViewings,
            @RequestParam(required = false) Integer maxViewings,
            @RequestParam(required = false) BigDecimal minSpending,
            @RequestParam(required = false) BigDecimal maxSpending,
            @RequestParam(required = false) Integer minContracts,
            @RequestParam(required = false) Integer maxContracts,
            @RequestParam(required = false) Integer minPropertiesBought,
            @RequestParam(required = false) Integer maxPropertiesBought,
            @RequestParam(required = false) Integer minPropertiesRented,
            @RequestParam(required = false) Integer maxPropertiesRented,
            @RequestParam(required = false) Integer minRanking,
            @RequestParam(required = false) Integer maxRanking,
            @RequestParam(required = false) LocalDateTime joinedDateFrom,
            @RequestParam(required = false) LocalDateTime joinedDateTo,
            @RequestParam(required = false) List<UUID> cityIds,
            @RequestParam(required = false) List<UUID> districtIds,
            @RequestParam(required = false) List<UUID> wardIds
    ) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        List<UUID> filteredCityIds = (cityIds != null && cityIds.isEmpty()) ? null : cityIds;
        List<UUID> filteredDistrictIds = (districtIds != null && districtIds.isEmpty()) ? null : districtIds;
        List<UUID> filteredWardIds = (wardIds != null && wardIds.isEmpty()) ? null : wardIds;

        Page<CustomerListItem> customerPage = userService.getAllCustomerItemsWithFilters(
                pageable, name, month, year, customerTiers,
                minLeadingScore, maxLeadingScore,
                minViewings, maxViewings,
                minSpending, maxSpending,
                minContracts, maxContracts,
                minPropertiesBought, maxPropertiesBought,
                minPropertiesRented, maxPropertiesRented,
                minRanking, maxRanking,
                joinedDateFrom, joinedDateTo,
                filteredCityIds, filteredDistrictIds, filteredWardIds
        );
        return responseFactory.successPage(customerPage, "Customers retrieved successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/property-owners")
    public ResponseEntity<ApiResponse<PagedData<PropertyOwnerListItem>>> getAllPropertyOwners(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int limit,
            @RequestParam(defaultValue = "desc") String sortType,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<com.se.bds.common.enums.ContributionTierEnum> ownerTiers,
            @RequestParam(required = false) Integer minContributionPoint,
            @RequestParam(required = false) Integer maxContributionPoint,
            @RequestParam(required = false) Integer minProperties,
            @RequestParam(required = false) Integer maxProperties,
            @RequestParam(required = false) Integer minPropertiesForSale,
            @RequestParam(required = false) Integer maxPropertiesForSale,
            @RequestParam(required = false) Integer minPropertiesForRents,
            @RequestParam(required = false) Integer maxPropertiesForRents,
            @RequestParam(required = false) Integer minRanking,
            @RequestParam(required = false) Integer maxRanking,
            @RequestParam(required = false) LocalDateTime joinedDateFrom,
            @RequestParam(required = false) LocalDateTime joinedDateTo,
            @RequestParam(required = false) List<UUID> cityIds,
            @RequestParam(required = false) List<UUID> districtIds,
            @RequestParam(required = false) List<UUID> wardIds
    ) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        List<UUID> filteredCityIds = (cityIds != null && cityIds.isEmpty()) ? null : cityIds;
        List<UUID> filteredDistrictIds = (districtIds != null && districtIds.isEmpty()) ? null : districtIds;
        List<UUID> filteredWardIds = (wardIds != null && wardIds.isEmpty()) ? null : wardIds;

        Page<PropertyOwnerListItem> ownerPage = userService.getAllPropertyOwnerItemsWithFilters(
                pageable, name, month, year, ownerTiers,
                minContributionPoint, maxContributionPoint,
                minProperties, maxProperties,
                minPropertiesForSale, maxPropertiesForSale,
                minPropertiesForRents, maxPropertiesForRents,
                minRanking, maxRanking,
                joinedDateFrom, joinedDateTo,
                filteredCityIds, filteredDistrictIds, filteredWardIds
        );
        return responseFactory.successPage(ownerPage, "Property owners retrieved successfully");
    }
}
