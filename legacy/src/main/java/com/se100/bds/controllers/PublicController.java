package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.requests.auth.LoginRequest;
import com.se100.bds.dtos.requests.auth.RegisterRequest;
import com.se100.bds.dtos.responses.PageResponse;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.SuccessResponse;
import com.se100.bds.dtos.responses.auth.TokenResponse;
import com.se100.bds.dtos.responses.document.DocumentTypeDetailsResponse;
import com.se100.bds.dtos.responses.document.DocumentTypeListItemResponse;
import com.se100.bds.dtos.responses.error.DetailedErrorResponse;
import com.se100.bds.dtos.responses.error.ErrorResponse;
import com.se100.bds.dtos.responses.location.LocationCardResponse;
import com.se100.bds.dtos.responses.location.LocationDetailsResponse;
import com.se100.bds.dtos.responses.property.PropertyDetails;
import com.se100.bds.dtos.responses.property.PropertyTypeResponse;
import com.se100.bds.dtos.responses.property.SimplePropertyCard;
import com.se100.bds.dtos.responses.user.otherprofile.UserProfileResponse;
import com.se100.bds.mappers.LocationMapper;
import com.se100.bds.mappers.PropertyMapper;
import com.se100.bds.models.entities.location.City;
import com.se100.bds.models.entities.property.PropertyType;
import com.se100.bds.services.domains.auth.AuthService;
import com.se100.bds.services.domains.document.DocumentService;
import com.se100.bds.services.domains.location.LocationService;
import com.se100.bds.services.domains.property.PropertyService;
import com.se100.bds.services.domains.user.UserService;
import com.se100.bds.services.dtos.results.PropertyCard;
import com.se100.bds.utils.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "000. Public", description = "Public API - All endpoints that don't require authentication")
@Slf4j
public class PublicController extends AbstractBaseController {
    private final AuthService authService;
    private final UserService userService;
    private final PropertyMapper propertyMapper;
    private final PropertyService propertyService;
    private final LocationService locationService;
    private final LocationMapper locationMapper;
    private final DocumentService documentService;

    // ==================== AUTH ENDPOINTS ====================

    @PostMapping("/auth/login")
    @Operation(
            summary = "Login endpoint",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TokenResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Bad credentials",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "422",
                            description = "Validation failed",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DetailedErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<TokenResponse>> login(
            @Parameter(description = "Request body to login", required = true)
            @RequestBody @Validated final LoginRequest request
    ) {
        TokenResponse tokenResponse = authService.login(request.getEmail(), request.getPassword(), false);
        return responseFactory.successSingle(tokenResponse, "Login successful");
    }

    @PostMapping(path = "/auth/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Register endpoint",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SuccessResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "422",
                            description = "Validation failed",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DetailedErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<SuccessResponse>> register(
            @Parameter(description = "Request body to register (multipart/form-data)", required = true)
            @Valid @ModelAttribute RegisterRequest request,
            BindingResult bindingResult,

            @Parameter(description = "Role to create")
            @RequestParam Constants.RoleEnum roleEnum
    ) throws BindException, IOException {
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }

        if (roleEnum.equals(Constants.RoleEnum.ADMIN) || roleEnum.equals(Constants.RoleEnum.SALESAGENT))
            return responseFactory.failedSingle(null, "Hell nah");

        userService.register(request, roleEnum);
        return responseFactory.successSingle(null, "Register successful");
    }

    @GetMapping("/auth/refresh")
    @Operation(
            summary = "Refresh endpoint",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TokenResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
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
    public ResponseEntity<SingleResponse<TokenResponse>> refresh(
            @Parameter(description = "Refresh token", required = true)
            @RequestHeader("Authorization") @Validated final String refreshToken
    ) {
        TokenResponse tokenResponse = authService.refreshFromBearerString(refreshToken);
        return responseFactory.successSingle(tokenResponse, "Refresh successful");
    }

    // ==================== PROPERTY ENDPOINTS ====================

    @GetMapping("/properties/cards")
    @Operation(
            summary = "Get all property cards with filters and pagination",
            description = "Retrieve a paginated list of property cards with optional filters for location, price, area, and property features",
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
                            responseCode = "400",
                            description = "Invalid parameters",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<SimplePropertyCard>> getAllCardsWithFilters(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "15") int limit,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Field to sort by")
            @RequestParam(required = false) String sortBy,

            @Parameter(description = "List of city IDs to filter by")
            @RequestParam(required = false) List<UUID> cityIds,

            @Parameter(description = "List of district IDs to filter by")
            @RequestParam(required = false) List<UUID> districtIds,

            @Parameter(description = "List of ward IDs to filter by")
            @RequestParam(required = false) List<UUID> wardIds,

            @Parameter(description = "List of property type IDs to filter by")
            @RequestParam(required = false) List<UUID> propertyTypeIds,

            @Parameter(description = "Property owner ID; Note: Use this when get all properties by owner id, if search in the landing page using owner's name and tier, keep this null")
            @RequestParam(required = false) UUID ownerId,

            @Parameter(description = "Property owner's name")
            @RequestParam(required = false) String ownerName,

            @Parameter(description = "Property owner's tier")
            @RequestParam(required = false) List<Constants.ContributionTierEnum> ownerTier,

            @Parameter(description = "The same shit as owner")
            @RequestParam(required = false) UUID agentId,

            @Parameter(description = "Agent's name")
            @RequestParam(required = false) String agentName,

            @Parameter(description = "Agent's tier")
            @RequestParam(required = false) List<Constants.PerformanceTierEnum> agentTier,

            @Parameter(description = "Is the property has agent assigned?")
            @RequestParam(required = false) Boolean hasAgent,

            @Parameter(description = "Get top K property? This sorted by most popular property. If admin, don't touch it")
            @RequestParam(required = true, defaultValue = "false") Boolean topK,

            @Parameter(description = "Minimum price")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Maximum price")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Minimum area (square meters)")
            @RequestParam(required = false) BigDecimal minArea,

            @Parameter(description = "Maximum area (square meters)")
            @RequestParam(required = false) BigDecimal maxArea,

            @Parameter(description = "Number of rooms")
            @RequestParam(required = false) Integer rooms,

            @Parameter(description = "Number of bathrooms")
            @RequestParam(required = false) Integer bathrooms,

            @Parameter(description = "Number of bedrooms")
            @RequestParam(required = false) Integer bedrooms,

            @Parameter(description = "Number of floors")
            @RequestParam(required = false) Integer floors,

            @Parameter(description = "House orientation (e.g., EAST, WEST, NORTH, SOUTH)")
            @RequestParam(required = false) Constants.OrientationEnum houseOrientation,

            @Parameter(description = "Balcony orientation (e.g., EAST, WEST, NORTH, SOUTH)")
            @RequestParam(required = false) Constants.OrientationEnum balconyOrientation,

            @Parameter(description = "List of desire Transaction type (e.g., SALE, RENT)")
            @RequestParam(required = false) List<Constants.TransactionTypeEnum> transactionType,

            @Parameter(description = "Property statuses (e.g., AVAILABLE, SOLD, RENTED, PENDING, APPROVED)")
            @RequestParam(required = false) List<Constants.PropertyStatusEnum> statuses
    ) {
        if (!topK)
            sortBy = null;
        Pageable pageable = createPageable(page, limit, sortType, sortBy);

        Page<PropertyCard> propertyCards = propertyService.getAllCardsWithFilters(
                cityIds,
                districtIds,
                wardIds,
                propertyTypeIds,
                ownerId,
                ownerName,
                ownerTier,
                agentId,
                agentName,
                agentTier,
                hasAgent,
                minPrice,
                maxPrice,
                minArea,
                maxArea,
                rooms,
                bathrooms,
                bedrooms,
                floors,
                houseOrientation,
                balconyOrientation,
                transactionType,
                statuses,
                topK,
                pageable
        );

        Page<SimplePropertyCard> simplePropertyCards = propertyMapper.mapToPage(propertyCards, SimplePropertyCard.class);

        return responseFactory.successPage(simplePropertyCards, "Property cards retrieved successfully");
    }

    @GetMapping("/properties/{propertyId}")
    @Operation(
            summary = "Get property details by ID",
            description = "Retrieve detailed information about a specific property including owner, agent, location, and media",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Property not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<PropertyDetails>> getPropertyDetails(
            @Parameter(description = "Property ID", required = true)
            @PathVariable UUID propertyId
    ) {
        log.info("Getting property details for ID: {}", propertyId);

        PropertyDetails propertyDetails = propertyService.getPropertyDetailsById(propertyId);

        return responseFactory.successSingle(propertyDetails, "Property details retrieved successfully");
    }

    // ==================== LOCATION ENDPOINTS ====================

    @GetMapping("/locations/cities/top")
    @Operation(
            summary = "Get top K most searched/popular cities",
            description = "Retrieve a paginated list of the most popular cities based on search frequency",
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
                            responseCode = "400",
                            description = "Invalid parameters",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<LocationCardResponse>> getTopCities(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Getting top most searched cities - page: {}, limit: {}", page, limit);

        Pageable pageable = createPageable(page, limit, null, null);
        Page<City> topCities = locationService.topMostSearchedCities(pageable);
        Page<LocationCardResponse> cityResponses = locationMapper.mapToPage(topCities, LocationCardResponse.class);

        return responseFactory.successPage(cityResponses, "Top most searched cities retrieved successfully");
    }

    @GetMapping("/locations/children")
    @Operation(
            summary = "Get child locations by parent ID",
            description = "Retrieve all districts of a city, wards of a district, or cities if no parent. Specify the search type to determine what child locations to return.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation - returns a map of UUID to name",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid search type",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<Map<UUID, String>>> getChildLocations(
            @Parameter(
                    description = "Parent location ID (null to get all cities)",
                    required = false
            )
            @RequestParam(required = false) UUID parentId,

            @Parameter(
                    description = "Type of child locations to retrieve: CITY (gets all cities), DISTRICT (gets districts of a city), WARD (gets wards of a district)",
                    required = true,
                    example = "DISTRICT"
            )
            @RequestParam Constants.SearchTypeEnum searchType
    ) {
        log.info("Getting child locations - parentId: {}, searchType: {}", parentId, searchType);

        Map<UUID, String> childLocations = locationService.findAllByParents(parentId, searchType);

        String message = parentId == null
                ? String.format("All %ss retrieved successfully", searchType.name().toLowerCase())
                : String.format("Child %ss retrieved successfully", searchType.name().toLowerCase());

        return responseFactory.successSingle(childLocations, message);
    }

    @GetMapping("/locations/cards")
    @Operation(
            summary = "Get all location cards with filters and pagination",
            description = "Retrieve a paginated list of location cards (cities, districts, or wards) with optional filters",
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
                            responseCode = "400",
                            description = "Invalid parameters",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<LocationCardResponse>> getAllLocationCardsWithFilters(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "15") int limit,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Field to sort by")
            @RequestParam(required = false) String sortBy,

            @Parameter(description = "Keyword to search in location name")
            @RequestParam(required = false) String keyWord,

            @Parameter(description = "List of city IDs to filter by")
            @RequestParam(required = false) List<UUID> cityIds,

            @Parameter(description = "List of district IDs to filter by")
            @RequestParam(required = false) List<UUID> districtIds,

            @Parameter(description = "Location type: CITY, DISTRICT, or WARD")
            @RequestParam(required = true) Constants.LocationEnum locationTypeEnum,

            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "Minimum average land price")
            @RequestParam(required = false) BigDecimal minAvgLandPrice,

            @Parameter(description = "Maximum average land price")
            @RequestParam(required = false) BigDecimal maxAvgLandPrice,

            @Parameter(description = "Minimum area")
            @RequestParam(required = false) BigDecimal minArea,

            @Parameter(description = "Maximum area")
            @RequestParam(required = false) BigDecimal maxArea,

            @Parameter(description = "Minimum population")
            @RequestParam(required = false) Integer minPopulation,

            @Parameter(description = "Maximum population")
            @RequestParam(required = false) Integer maxPopulation
    ) {
        log.info("Getting all location cards with filters - page: {}, limit: {}, locationTypeEnum: {}", page, limit, locationTypeEnum);

        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<LocationCardResponse> locationCards = locationService.findAllLocationCardsWithFilter(
                pageable,
                keyWord,
                cityIds,
                districtIds,
                locationTypeEnum,
                isActive,
                minAvgLandPrice,
                maxAvgLandPrice,
                minArea,
                maxArea,
                minPopulation,
                maxPopulation
        );

        return responseFactory.successPage(locationCards, "Location cards retrieved successfully");
    }

    @GetMapping("/locations/{locationId}/details")
    @Operation(
            summary = "Get location details by ID and type",
            description = "Retrieve detailed information about a specific location (city, district, or ward)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Location not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<LocationDetailsResponse>> getLocationDetails(
            @Parameter(description = "Location ID", required = true)
            @PathVariable UUID locationId,

            @Parameter(description = "Location type: CITY, DISTRICT, or WARD", required = true)
            @RequestParam Constants.LocationEnum locationTypeEnum
    ) {
        log.info("Getting location details - locationId: {}, locationTypeEnum: {}", locationId, locationTypeEnum);

        LocationDetailsResponse locationDetails = 
                locationService.getLocationDetails(locationId, locationTypeEnum);

        return responseFactory.successSingle(locationDetails, "Location details retrieved successfully");
    }

    @GetMapping("/locations/property-types")
    @Operation(
            summary = "Get all property types",
            description = "Retrieve a paginated list of all property types",
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
                            responseCode = "400",
                            description = "Invalid parameters",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<PropertyTypeResponse>> getAllPropertyTypes(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Field to sort by")
            @RequestParam(required = false) String sortBy
    ) {
        log.info("Getting all property types - page: {}, limit: {}", page, limit);

        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<PropertyType> propertyTypes = propertyService.getAllTypes(pageable);
        Page<PropertyTypeResponse> propertyTypeResponses = propertyMapper.mapToPage(propertyTypes, PropertyTypeResponse.class);

        return responseFactory.successPage(propertyTypeResponses, "Property types retrieved successfully");
    }

    // ==================== DOCUMENT TYPE ENDPOINTS ====================

    @GetMapping("/document-types")
    @Operation(
            summary = "Get all document types with optional filter",
            description = "Retrieve a paginated list of document types with optional compulsory filter",
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
                            responseCode = "400",
                            description = "Invalid parameters",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<DocumentTypeListItemResponse>> getAllDocumentTypes(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Field to sort by")
            @RequestParam(required = false) String sortBy,

            @Parameter(description = "Filter by compulsory status")
            @RequestParam(required = false) Boolean isCompulsory
    ) {
        log.info("Getting all document types - page: {}, limit: {}, isCompulsory: {}", page, limit, isCompulsory);

        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<DocumentTypeListItemResponse> documentTypes =
                documentService.getAllWithFilter(pageable, isCompulsory);

        return responseFactory.successPage(documentTypes, "Document types retrieved successfully");
    }

    @GetMapping("/document-types/{id}")
    @Operation(
            summary = "Get document type details by ID",
            description = "Retrieve detailed information about a specific document type",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Document type not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<DocumentTypeDetailsResponse>> getDocumentTypeById(
            @Parameter(description = "Document type ID", required = true)
            @PathVariable UUID id
    ) {
        log.info("Getting document type details - id: {}", id);

        DocumentTypeDetailsResponse documentType = documentService.getById(id);

        return responseFactory.successSingle(documentType, "Document type details retrieved successfully");
    }

    // ==================== ACCOUNT ENDPOINTS ====================

    @GetMapping("/account/{id}/other-profile")
    @Operation(
            summary = "Get other user profile (not your self) by ID",
            description = "Get detailed user profile including role-specific information (properties, contracts, etc.)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserProfileResponse.class)
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
    public ResponseEntity<SingleResponse<UserProfileResponse<?>>> getUserProfileById(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID id
    ) {
        log.info("Getting user profile for ID: {}", id);
        UserProfileResponse<?> userProfileResponse = userService.getUserProfileById(id);
        return responseFactory.successSingle(userProfileResponse, "User profile retrieved successfully");
    }
}

