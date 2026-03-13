package com.orlandoprestige.orlandoproject.orders.internal.procurement.repository;

import com.orlandoprestige.orlandoproject.orders.internal.procurement.domain.SystemPurchaseOrderAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemPurchaseOrderAttachmentRepository extends JpaRepository<SystemPurchaseOrderAttachment, Long> {
    List<SystemPurchaseOrderAttachment> findAllByPurchaseOrderIdOrderByUploadedAtDesc(Long purchaseOrderId);
}
