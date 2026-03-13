package com.orlandoprestige.orlandoproject.orders.internal.procurement.repository;

import com.orlandoprestige.orlandoproject.orders.internal.procurement.domain.SystemPurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemPurchaseOrderItemRepository extends JpaRepository<SystemPurchaseOrderItem, Long> {
    List<SystemPurchaseOrderItem> findAllByPurchaseOrderId(Long purchaseOrderId);
    void deleteAllByPurchaseOrderId(Long purchaseOrderId);
}
