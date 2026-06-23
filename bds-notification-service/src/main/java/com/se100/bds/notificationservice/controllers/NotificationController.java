package com.se100.bds.notificationservice.controllers;

import com.se100.bds.notificationservice.dtos.requests.CreateNotificationRequest;
import com.se.bds.common.dto.ApiResponse;
import com.se.bds.common.dto.PagedData;
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
    public ResponseEntity<ApiResponse<PagedData<NotificationItem>>> getMyNotifications(
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

        PagedData<NotificationItem> pagedData = PagedData.<NotificationItem>builder()
                .content(notifications.getContent())
                .pageNumber(notifications.getNumber())
                .pageSize(notifications.getSize())
                .totalElements(notifications.getTotalElements())
                .totalPages(notifications.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(pagedData));
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification details")
    public ResponseEntity<ApiResponse<NotificationDetails>> getNotificationDetails(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        NotificationDetails details = notificationService.getNotificationDetailsById(currentUserId, notificationId);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<NotificationDetails>> markAsRead(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId
    ) {
        UUID currentUserId = parseUserId(userIdHeader);
        NotificationDetails details = notificationService.markAsRead(currentUserId, notificationId);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @PostMapping
    @Operation(summary = "Create a notification (internal API)")
    public ResponseEntity<ApiResponse<Void>> createNotification(
            @RequestBody CreateNotificationRequest request
    ) {
        notificationService.createNotificationAsync(
                request.getRecipientId(),
                request.getFcmToken(),
                request.getType(),
                request.getTitle(),
                request.getMessage(),
                request.getRelatedEntityType(),
                request.getRelatedEntityId(),
                request.getImgUrl()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
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
