package com.orlandoprestige.orlandoproject.inventory.internal.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_stock", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_code"}))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_code", nullable = false)
    private WarehouseCode warehouseCode;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}
