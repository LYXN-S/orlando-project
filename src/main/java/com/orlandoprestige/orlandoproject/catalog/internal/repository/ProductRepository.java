package com.orlandoprestige.orlandoproject.catalog.internal.repository;

import com.orlandoprestige.orlandoproject.catalog.internal.domain.Product;
import com.orlandoprestige.orlandoproject.catalog.internal.domain.ProductAvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySku(String sku);
    List<Product> findByCategory(String category);
    List<Product> findAllByAvailabilityStatus(ProductAvailabilityStatus availabilityStatus);
    List<Product> findByCategoryAndAvailabilityStatus(String category, ProductAvailabilityStatus availabilityStatus);
    List<Product> findAllByStockQuantityGreaterThan(int quantity);
    boolean existsBySku(String sku);
}

