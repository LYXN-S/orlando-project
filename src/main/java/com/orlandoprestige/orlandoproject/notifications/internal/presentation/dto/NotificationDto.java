package com.orlandoprestige.orlandoproject.notifications.internal.presentation.dto;

import com.orlandoprestige.orlandoproject.notifications.internal.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long referenceId,
        boolean read,
        LocalDateTime createdAt
) {
}
