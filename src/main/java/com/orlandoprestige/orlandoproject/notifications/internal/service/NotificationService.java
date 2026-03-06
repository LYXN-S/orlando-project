package com.orlandoprestige.orlandoproject.notifications.internal.service;

import com.orlandoprestige.orlandoproject.notifications.internal.domain.Notification;
import com.orlandoprestige.orlandoproject.notifications.internal.domain.NotificationType;
import com.orlandoprestige.orlandoproject.notifications.internal.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(Long userId, String targetRole,
                                           NotificationType type, String title,
                                           String message, Long referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTargetRole(targetRole);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        return notificationRepository.save(notification);
    }

    /**
     * Create a notification targeting ALL staff members (userId = 0 as broadcast marker).
     */
    @Transactional
    public Notification createStaffBroadcast(NotificationType type, String title,
                                              String message, Long referenceId) {
        return createStaffBroadcast(type, title, message, referenceId, null);
    }

    /**
     * Create a permission-scoped staff broadcast (only visible to staff with the given permission + super admins).
     */
    @Transactional
    public Notification createStaffBroadcast(NotificationType type, String title,
                                              String message, Long referenceId,
                                              String requiredPermission) {
        Notification notification = new Notification();
        notification.setUserId(0L);
        notification.setTargetRole("ROLE_STAFF");
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setRequiredPermission(requiredPermission);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId, String role) {
        return getNotificationsForUser(userId, role, List.of());
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId, String role, List<String> permissions) {
        // Determine the targetRole to filter personal notifications
        String targetRole = ("ROLE_STAFF".equals(role) || "ROLE_SUPER_ADMIN".equals(role))
                ? "ROLE_STAFF" : role;

        // Get personal notifications scoped to BOTH userId AND targetRole
        List<Notification> personal = new java.util.ArrayList<>(
                notificationRepository.findByUserIdAndTargetRoleOrderByCreatedAtDesc(userId, targetRole));
        if ("ROLE_STAFF".equals(role) || "ROLE_SUPER_ADMIN".equals(role)) {
            boolean isSuperAdmin = "ROLE_SUPER_ADMIN".equals(role);
            List<Notification> broadcasts = notificationRepository.findByTargetRoleOrderByCreatedAtDesc("ROLE_STAFF");
            personal.addAll(broadcasts.stream()
                    .filter(n -> n.getUserId() == 0L)
                    .filter(n -> {
                        // No permission requirement → visible to all staff
                        if (n.getRequiredPermission() == null || n.getRequiredPermission().isBlank()) {
                            return true;
                        }
                        // Super admin sees everything
                        if (isSuperAdmin) {
                            return true;
                        }
                        // Staff must have the required permission
                        return permissions.contains(n.getRequiredPermission());
                    })
                    .toList());
            personal.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }
        return personal;
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId, String role) {
        return getUnreadCount(userId, role, List.of());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId, String role, List<String> permissions) {
        // Determine the targetRole to scope personal unread count
        String targetRole = ("ROLE_STAFF".equals(role) || "ROLE_SUPER_ADMIN".equals(role))
                ? "ROLE_STAFF" : role;

        long personal = notificationRepository.countByUserIdAndTargetRoleAndReadFalse(userId, targetRole);
        if ("ROLE_STAFF".equals(role) || "ROLE_SUPER_ADMIN".equals(role)) {
            boolean isSuperAdmin = "ROLE_SUPER_ADMIN".equals(role);
            if (isSuperAdmin) {
                personal += notificationRepository.countByTargetRoleAndReadFalse("ROLE_STAFF");
            } else {
                // Count only broadcasts the user has permission to see
                List<Notification> broadcasts = notificationRepository.findByTargetRoleOrderByCreatedAtDesc("ROLE_STAFF");
                personal += broadcasts.stream()
                        .filter(n -> n.getUserId() == 0L && !n.isRead())
                        .filter(n -> {
                            if (n.getRequiredPermission() == null || n.getRequiredPermission().isBlank()) {
                                return true;
                            }
                            return permissions.contains(n.getRequiredPermission());
                        })
                        .count();
            }
        }
        return personal;
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId, String role) {
        // Determine the targetRole to scope which notifications to mark
        String targetRole = ("ROLE_STAFF".equals(role) || "ROLE_SUPER_ADMIN".equals(role))
                ? "ROLE_STAFF" : role;

        // Mark personal notifications as read
        List<Notification> unread = notificationRepository.findByUserIdAndTargetRoleAndReadFalseOrderByCreatedAtDesc(userId, targetRole);
        unread.forEach(n -> n.setRead(true));

        // For staff, also mark broadcast notifications as read
        if ("ROLE_STAFF".equals(role) || "ROLE_SUPER_ADMIN".equals(role)) {
            List<Notification> broadcastUnread = notificationRepository.findByTargetRoleAndReadFalseOrderByCreatedAtDesc("ROLE_STAFF");
            broadcastUnread.stream()
                    .filter(n -> n.getUserId() == 0L)
                    .forEach(n -> n.setRead(true));
            unread.addAll(broadcastUnread);
        }

        notificationRepository.saveAll(unread);
    }
}
