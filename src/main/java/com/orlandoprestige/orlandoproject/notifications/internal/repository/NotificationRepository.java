package com.orlandoprestige.orlandoproject.notifications.internal.repository;

import com.orlandoprestige.orlandoproject.notifications.internal.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndTargetRoleOrderByCreatedAtDesc(Long userId, String targetRole);

    List<Notification> findByTargetRoleOrderByCreatedAtDesc(String targetRole);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndTargetRoleAndReadFalseOrderByCreatedAtDesc(Long userId, String targetRole);

    List<Notification> findByTargetRoleAndReadFalseOrderByCreatedAtDesc(String targetRole);

    long countByUserIdAndReadFalse(Long userId);

    long countByUserIdAndTargetRoleAndReadFalse(Long userId, String targetRole);

    long countByTargetRoleAndReadFalse(String targetRole);
}
