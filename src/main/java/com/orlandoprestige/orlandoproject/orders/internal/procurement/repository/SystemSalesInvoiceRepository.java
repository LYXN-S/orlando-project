package com.orlandoprestige.orlandoproject.orders.internal.procurement.repository;

import com.orlandoprestige.orlandoproject.orders.internal.procurement.domain.SystemSalesInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSalesInvoiceRepository extends JpaRepository<SystemSalesInvoice, Long> {
    Optional<SystemSalesInvoice> findByPurchaseOrderId(Long purchaseOrderId);
}
