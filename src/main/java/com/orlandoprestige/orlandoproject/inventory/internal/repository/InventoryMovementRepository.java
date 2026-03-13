package com.orlandoprestige.orlandoproject.inventory.internal.repository;

import com.orlandoprestige.orlandoproject.inventory.internal.domain.InventoryMovement;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.MovementType;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findAllByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryMovement> findAllByMovementTypeOrderByCreatedAtDesc(MovementType movementType);

    List<InventoryMovement> findAllByOrderByCreatedAtDesc();

    @Query("SELECT m FROM InventoryMovement m WHERE " +
           "(:productId IS NULL OR m.productId = :productId) AND " +
           "(:warehouseCode IS NULL OR m.warehouseCode = :warehouseCode) AND " +
           "(:type IS NULL OR m.movementType = :type) AND " +
           "(:from IS NULL OR m.createdAt >= :from) AND " +
           "(:to IS NULL OR m.createdAt <= :to) " +
           "ORDER BY m.createdAt DESC")
    List<InventoryMovement> findFiltered(
            @Param("productId") Long productId,
            @Param("warehouseCode") WarehouseCode warehouseCode,
            @Param("type") MovementType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT m FROM InventoryMovement m WHERE m.createdAt >= :dayStart AND m.createdAt < :dayEnd ORDER BY m.productId, m.createdAt")
    List<InventoryMovement> findByDay(@Param("dayStart") LocalDateTime dayStart, @Param("dayEnd") LocalDateTime dayEnd);
}
