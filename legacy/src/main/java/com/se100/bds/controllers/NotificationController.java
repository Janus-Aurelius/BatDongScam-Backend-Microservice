package com.se100.bds.controllers;

import com.se100.bds.controllers.base.AbstractBaseController;
import com.se100.bds.dtos.responses.PageResponse;
import com.se100.bds.dtos.responses.SingleResponse;
import com.se100.bds.dtos.responses.notification.NotificationDetails;
import com.se100.bds.dtos.responses.notification.NotificationItem;
import com.se100.bds.services.domains.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.se100.bds.utils.Constants.SECURITY_SCHEME_NAME;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
@Tag(name = "015. Notification Controller", description = "User notification management API")
@Slf4j
public class NotificationController extends AbstractBaseController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER', 'SALESAGENT', 'ADMIN')")
    @Operation(
            summary = "Get my notifications",
            description = "Retrieve paginated list of notifications for the current user, ordered by creation date (newest first).",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<NotificationItem>> getMyNotifications(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,

            @Parameter(description = "Field to sort by")
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        Page<NotificationItem> notifications = notificationService.getMyNotifications(pageable);
        return responseFactory.successPage(notifications, "Notifications retrieved successfully");
    }

    @GetMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER', 'SALESAGENT', 'ADMIN')")
    @Operation(
            summary = "Get notification details",
            description = "Retrieve detailed information of a specific notification. Users can only view their own notifications.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Notification not found or does not belong to current user")
    })
    public ResponseEntity<SingleResponse<NotificationDetails>> getNotificationDetails(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId
    ) {
        NotificationDetails notificationDetails = notificationService.getNotificationDetailsById(notificationId);
        return responseFactory.successSingle(notificationDetails, "Notification details retrieved successfully");
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'PROPERTY_OWNER', 'SALESAGENT', 'ADMIN')")
    @Operation(
            summary = "Mark notification as read",
            description = "Mark a specific notification as read. Users can only mark their own notifications.",
            security = @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification marked as read successfully",
                    content = @Content(schema = @Schema(implementation = SingleResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Notification not found or does not belong to current user")
    })
    public ResponseEntity<SingleResponse<NotificationDetails>> markAsRead(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId
    ) {
        NotificationDetails notificationDetails = notificationService.markAsRead(notificationId);
        return responseFactory.successSingle(notificationDetails, "Notification marked as read successfully");
    }
}
