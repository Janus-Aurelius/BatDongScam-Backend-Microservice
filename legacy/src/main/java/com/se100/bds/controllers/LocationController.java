package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.requests.location.CreateLocationRequest;
import com.se100.bds.dtos.requests.location.UpdateLocationRequest;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.SuccessResponse;
import com.se100.bds.dtos.responses.error.DetailedErrorResponse;
import com.se100.bds.dtos.responses.error.ErrorResponse;
import com.se100.bds.dtos.responses.location.LocationDetailsResponse;
import com.se100.bds.services.domains.location.LocationService;
import com.se100.bds.utils.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

import static com.se100.bds.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("locations")
@Tag(name = "007. Locations", description = "Location API")
@Slf4j
public class LocationController extends AbstractBaseController {
    private final LocationService locationService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a new location (City, District, or Ward)",
            description = "Create a new location with optional image upload. Specify the location type (CITY, DISTRICT, or WARD) and provide parent ID for District and Ward.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Location created successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "422",
                            description = "Validation failed",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DetailedErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Parent location not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<LocationDetailsResponse>> createLocation(
            @Parameter(description = "Request body to create location (multipart/form-data)", required = true)
            @Valid @ModelAttribute CreateLocationRequest request,
            BindingResult bindingResult
    ) throws BindException, IOException {
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }

        log.info("Creating new location - type: {}, name: {}", request.getLocationTypeEnum(), request.getName());
        LocationDetailsResponse response = locationService.create(request);
        return responseFactory.successSingle(response, "Location created successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update an existing location",
            description = "Update location details including name, description, image, area, population, etc. Provide location ID and type.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Location updated successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "422",
                            description = "Validation failed",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DetailedErrorResponse.class)
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
    public ResponseEntity<SingleResponse<LocationDetailsResponse>> updateLocation(
            @Parameter(description = "Request body to update location (multipart/form-data)", required = true)
            @Valid @ModelAttribute UpdateLocationRequest request,
            BindingResult bindingResult
    ) throws BindException, IOException {
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }

        log.info("Updating location - id: {}, type: {}", request.getId(), request.getLocationTypeEnum());
        LocationDetailsResponse response = locationService.update(request);
        return responseFactory.successSingle(response, "Location updated successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{locationId}")
    @Operation(
            summary = "Delete a location by ID and type",
            description = "Delete a location (City, District, or Ward) by ID. This will also delete the associated image from Cloudinary.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Location deleted successfully",
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
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Failed to delete location",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<SuccessResponse>> deleteLocation(
            @Parameter(description = "Location ID to delete", required = true)
            @PathVariable UUID locationId,

            @Parameter(description = "Location type: CITY, DISTRICT, or WARD", required = true)
            @RequestParam Constants.LocationEnum locationTypeEnum
    ) {
        log.info("Deleting location - id: {}, type: {}", locationId, locationTypeEnum);
        boolean deleted = locationService.delete(locationId, locationTypeEnum);

        if (deleted) {
            return responseFactory.successSingle(null, "Location deleted successfully");
        } else {
            return responseFactory.failedSingle(null, "Failed to delete location");
        }
    }
}
