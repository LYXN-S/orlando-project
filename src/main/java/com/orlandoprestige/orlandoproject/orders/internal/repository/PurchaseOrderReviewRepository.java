package com.orlandoprestige.orlandoproject.orders.internal.repository;

import com.orlandoprestige.orlandoproject.orders.internal.domain.PurchaseOrderReview;
import com.orlandoprestige.orlandoproject.orders.internal.domain.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderReviewRepository extends JpaRepository<PurchaseOrderReview, Long> {
    Optional<PurchaseOrderReview> findByOrderId(Long orderId);
    List<PurchaseOrderReview> findAllByOrderByCreatedAtDesc();
    List<PurchaseOrderReview> findAllByStatusOrderByCreatedAtDesc(PurchaseOrderStatus status);
    List<PurchaseOrderReview> findAllByStatusAndReviewedAtBetween(PurchaseOrderStatus status, LocalDateTime from, LocalDateTime to);
}
