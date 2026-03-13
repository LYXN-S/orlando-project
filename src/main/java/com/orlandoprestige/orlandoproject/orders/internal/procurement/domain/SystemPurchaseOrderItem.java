package com.orlandoprestige.orlandoproject.orders.internal.procurement.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "system_purchase_order_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemPurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private SystemPurchaseOrder purchaseOrder;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sku")
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 14, scale = 2)
    private BigDecimal lineTotal;
}
