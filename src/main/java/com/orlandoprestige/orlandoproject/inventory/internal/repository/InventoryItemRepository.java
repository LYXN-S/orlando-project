package com.orlandoprestige.orlandoproject.inventory.internal.repository;

import com.orlandoprestige.orlandoproject.inventory.internal.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByProductId(Long productId);
    boolean existsByProductId(Long productId);
}
