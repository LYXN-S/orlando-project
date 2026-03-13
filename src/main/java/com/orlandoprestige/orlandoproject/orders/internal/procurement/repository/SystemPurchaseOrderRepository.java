package com.orlandoprestige.orlandoproject.orders.internal.procurement.repository;

import com.orlandoprestige.orlandoproject.orders.internal.procurement.domain.SystemPoStatus;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.domain.SystemPurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemPurchaseOrderRepository extends JpaRepository<SystemPurchaseOrder, Long> {
    List<SystemPurchaseOrder> findAllByOrderByCreatedAtDesc();
    List<SystemPurchaseOrder> findAllByStatusOrderByCreatedAtDesc(SystemPoStatus status);
}
