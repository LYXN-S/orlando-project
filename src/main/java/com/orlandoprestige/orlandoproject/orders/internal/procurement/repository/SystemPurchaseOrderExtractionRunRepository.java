package com.orlandoprestige.orlandoproject.orders.internal.procurement.repository;

import com.orlandoprestige.orlandoproject.orders.internal.procurement.domain.SystemPurchaseOrderExtractionRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemPurchaseOrderExtractionRunRepository extends JpaRepository<SystemPurchaseOrderExtractionRun, Long> {
    List<SystemPurchaseOrderExtractionRun> findAllByPurchaseOrderIdOrderByCreatedAtDesc(Long purchaseOrderId);
}
