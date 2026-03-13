package com.orlandoprestige.orlandoproject.inventory.internal.repository;

import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseCode;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {
    Optional<WarehouseStock> findByProductIdAndWarehouseCode(Long productId, WarehouseCode warehouseCode);
    List<WarehouseStock> findAllByProductId(Long productId);
    List<WarehouseStock> findAllByWarehouseCode(WarehouseCode warehouseCode);
}
