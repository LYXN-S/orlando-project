package com.orlandoprestige.orlandoproject.inventory.internal.domain;

import com.orlandoprestige.orlandoproject.shared.domain.entities.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items", uniqueConstraints = @UniqueConstraint(columnNames = "product_id"))
@SQLDelete(sql = "UPDATE inventory_items SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryItem extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "current_stock", nullable = false)
    private int currentStock;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 5;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}
