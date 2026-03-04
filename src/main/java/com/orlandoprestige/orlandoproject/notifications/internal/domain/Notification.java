package com.orlandoprestige.orlandoproject.notifications.internal.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user this notification targets (customerId or staffId). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Role of the target user (ROLE_STAFF or ROLE_CUSTOMER). */
    @Column(name = "target_role", nullable = false)
    private String targetRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Optional reference to related entity (e.g. orderId, productId). */
    @Column(name = "reference_id")
    private Long referenceId;

    /** Optional permission required to see this broadcast (e.g. MANAGE_ORDERS). */
    @Column(name = "required_permission")
    private String requiredPermission;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
