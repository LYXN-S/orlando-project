package com.orlandoprestige.orlandoproject.notifications.internal.repository;

import com.orlandoprestige.orlandoproject.notifications.internal.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByTargetRoleOrderByCreatedAtDesc(String targetRole);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findByTargetRoleAndReadFalseOrderByCreatedAtDesc(String targetRole);

    long countByUserIdAndReadFalse(Long userId);

    long countByTargetRoleAndReadFalse(String targetRole);
}
