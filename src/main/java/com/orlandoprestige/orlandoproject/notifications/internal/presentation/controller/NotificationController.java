package com.orlandoprestige.orlandoproject.notifications.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.notifications.internal.domain.Notification;
import com.orlandoprestige.orlandoproject.notifications.internal.presentation.dto.NotificationDto;
import com.orlandoprestige.orlandoproject.notifications.internal.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications for the authenticated user")
    public ResponseEntity<List<NotificationDto>> getMyNotifications(
            @AuthenticationPrincipal AuthenticatedUser user) {
        List<Notification> notifications = notificationService.getNotificationsForUser(
                user.userId(), user.role(), user.permissions());
        return ResponseEntity.ok(notifications.stream().map(this::toDto).toList());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal AuthenticatedUser user) {
        long count = notificationService.getUnreadCount(user.userId(), user.role(), user.permissions());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(toDto(notification));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read for current user")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedUser user) {
        notificationService.markAllAsRead(user.userId(), user.role());
        return ResponseEntity.noContent().build();
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
