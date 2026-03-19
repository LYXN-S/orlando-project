package com.orlandoprestige.orlandoproject.orders.internal.procurement.repository;

import com.orlandoprestige.orlandoproject.orders.internal.procurement.domain.SystemSalesInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemSalesInvoiceItemRepository extends JpaRepository<SystemSalesInvoiceItem, Long> {
    List<SystemSalesInvoiceItem> findAllBySalesInvoiceIdOrderByLineNumberAsc(Long salesInvoiceId);
    void deleteAllBySalesInvoiceId(Long salesInvoiceId);
}
