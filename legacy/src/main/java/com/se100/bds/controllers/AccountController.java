package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.requests.account.UpdateAccountDto;
import com.se100.bds.dtos.responses.PageResponse;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.user.listitem.CustomerListItem;
import com.se100.bds.dtos.responses.user.listitem.PropertyOwnerListItem;
import com.se100.bds.dtos.responses.user.listitem.SaleAgentListItem;
import com.se100.bds.dtos.responses.error.ErrorResponse;
import com.se100.bds.dtos.responses.SuccessResponse;
import com.se100.bds.dtos.responses.user.meprofile.MeResponse;
import com.se100.bds.models.entities.user.User;
import com.se100.bds.mappers.UserMapper;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.utils.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.se100.bds.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("/account")
@Tag(name = "002. Account", description = "Account API")
@Slf4j
public class AccountController extends AbstractBaseController {
    private final UserService userService;
    private final UserMapper userMapper;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(MultipartFile.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                // Convert empty string to null for MultipartFile fields
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                }
            }
        });
    }

    @GetMapping("/me")
    @Operation(
            summary = "Me endpoint",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = MeResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Bad credentials",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<MeResponse>> me() {
        MeResponse meResponse = userService.getAccount();
        return responseFactory.successSingle(meResponse, "Successful operation");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user by ID with statistics",
            description = "Get detailed user information including statistics for a specific user ID",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MeResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<MeResponse<?>>> getUserById(@PathVariable UUID userId) {
        MeResponse<?> meResponse = userService.getUserById(userId);
        return responseFactory.successSingle(meResponse, "Successful operation");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update user by ID",
            description = "Update user information for a specific user ID (Admin only)",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User updated successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MeResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<MeResponse<?>>> updateUserById(
            @PathVariable UUID userId,
            @Parameter(description = "Update account data (multipart/form-data)", required = true)
            @Valid @ModelAttribute UpdateAccountDto updateAccountDto,
            BindingResult bindingResult
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }
        MeResponse<?> meResponse = userService.updateUserById(userId, updateAccountDto);
        return responseFactory.successSingle(meResponse, "User updated successfully");
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update my account",
            description = "Update current authenticated user's account information",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Account updated successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MeResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<MeResponse<?>>> updateMe(
            @Parameter(description = "Update account data (multipart/form-data)", required = true)
            @Valid @ModelAttribute UpdateAccountDto updateAccountDto,
            BindingResult bindingResult
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }
        MeResponse<?> meResponse = userService.updateMe(updateAccountDto);
        return responseFactory.successSingle(meResponse, "Account updated successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    @Operation(
            summary = "Delete user account by ID",
            description = "Delete user account. If user is ADMIN, performs hard delete (removes from database). Otherwise, performs soft delete (sets status to DELETED).",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Account deleted successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SuccessResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<Void>> deleteAccountById(@PathVariable UUID userId) {
        userService.deleteAccountById(userId);
        return responseFactory.successSingle(null, "Account deleted successfully");
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "Delete my account",
            description = "Delete current authenticated user's account. If user is ADMIN, performs hard delete. Otherwise, performs soft delete (sets status to DELETED).",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Account deleted successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SuccessResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<Void>> deleteMyAccount() {
        userService.deleteMyAccount();
        return responseFactory.successSingle(null, "Account deleted successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{propOwnerId}/{approve}/approve")
    @Operation(
            summary = "Approve a property owner account",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Account approved successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<Void>> approveAccount(
            @Parameter(description = "Property owner ID", required = true)
            @PathVariable UUID propOwnerId,
            @Parameter(description = "approve(true) or reject", deprecated = true)
            @PathVariable Boolean approve
    ) {
        userService.approveAccount(propOwnerId, approve);
        return responseFactory.successSingle(null, "Account approved successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/sale-agents")
    @Operation(
            summary = "Get all sale agents with filters",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PageResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<SaleAgentListItem>> getAllSaleAgents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int limit,
            @RequestParam(defaultValue = "desc") String sortType,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<Constants.PerformanceTierEnum> agentTiers,
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
        // Convert empty lists to null to avoid JPQL issues
        List<UUID> filteredCityIds = (cityIds != null && cityIds.isEmpty()) ? null : cityIds;
        List<UUID> filteredDistrictIds = (districtIds != null && districtIds.isEmpty()) ? null : districtIds;
        List<UUID> filteredWardIds = (wardIds != null && wardIds.isEmpty()) ? null : wardIds;

        // TODO: Replaced this shit
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
    @Operation(
            summary = "Get all customers with filters",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PageResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<CustomerListItem>> getAllCustomers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int limit,
            @RequestParam(defaultValue = "desc") String sortType,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<Constants.CustomerTierEnum> customerTiers,
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
        // Convert empty lists to null to avoid JPQL issues
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
    @Operation(
            summary = "Get all property owners with filters",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PageResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<PropertyOwnerListItem>> getAllPropertyOwners(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int limit,
            @RequestParam(defaultValue = "desc") String sortType,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<Constants.ContributionTierEnum> ownerTiers,
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
        // Convert empty lists to null to avoid JPQL issues
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
