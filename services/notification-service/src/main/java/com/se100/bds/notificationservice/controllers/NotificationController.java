package com.se100.bds.notificationservice.controllers;

import com.se100.bds.notificationservice.dtos.requests.CreateNotificationRequest;
import com.se100.bds.notificationservice.dtos.responses.PageResponse;
import com.se100.bds.notificationservice.dtos.responses.SingleResponse;
import com.se100.bds.notificationservice.dtos.responses.notification.NotificationDetails;
import com.se100.bds.notificationservice.dtos.responses.notification.NotificationItem;
import com.se100.bds.notificationservice.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification management API")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get my notifications")
    public ResponseEntity<PageResponse<NotificationItem>> getMyNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortType,
            @Parameter(description = "Field to sort by")
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        Sort.Direction direction = sortType.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit, Sort.by(direction, sortBy));

        Page<NotificationItem> notifications = notificationService.getMyNotifications(currentUserId, pageable);

        PageResponse<NotificationItem> response = PageResponse.<NotificationItem>builder()
                .statusCode(200)
                .message("Notifications retrieved successfully")
                .data(notifications.getContent())
                .paging(new PageResponse.PagingResponse(
                        notifications.getNumber(),
                        notifications.getSize(),
                        notifications.getTotalElements(),
                        notifications.getTotalPages()
                ))
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification details")
    public ResponseEntity<SingleResponse<NotificationDetails>> getNotificationDetails(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        NotificationDetails details = notificationService.getNotificationDetailsById(currentUserId, notificationId);
        return ResponseEntity.ok(
                SingleResponse.<NotificationDetails>builder()
                        .statusCode(200)
                        .message("Notification details retrieved successfully")
                        .data(details)
                        .build()
        );
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<SingleResponse<NotificationDetails>> markAsRead(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        NotificationDetails details = notificationService.markAsRead(currentUserId, notificationId);
        return ResponseEntity.ok(
                SingleResponse.<NotificationDetails>builder()
                        .statusCode(200)
                        .message("Notification marked as read successfully")
                        .data(details)
                        .build()
        );
    }

    @PostMapping
    @Operation(summary = "Create a notification (internal API)")
    public ResponseEntity<SingleResponse<String>> createNotification(
            @RequestBody CreateNotificationRequest request
    ) {
        notificationService.createNotification(
                request.getRecipientId(),
                request.getFcmToken(),
                request.getType(),
                request.getTitle(),
                request.getMessage(),
                request.getRelatedEntityType(),
                request.getRelatedEntityId(),
                request.getImgUrl()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
                SingleResponse.<String>builder()
                        .statusCode(201)
                        .message("Notification created successfully")
                        .data(null)
                        .build()
        );
    }

    private UUID parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid X-User-Id header: {}", userIdHeader);
            return null;
        }
    }
}
