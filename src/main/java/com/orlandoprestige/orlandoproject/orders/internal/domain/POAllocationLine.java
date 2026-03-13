package com.orlandoprestige.orlandoproject.orders.internal.domain;

import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "po_allocation_lines")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class POAllocationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_review_id", nullable = false)
    private PurchaseOrderReview poReview;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_code", nullable = false)
    private WarehouseCode warehouseCode;

    @Column(name = "allocated_quantity", nullable = false)
    private int allocatedQuantity;
}
