package com.orlandoprestige.orlandoproject.catalog.internal.repository;

import com.orlandoprestige.orlandoproject.catalog.internal.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    List<Product> findByCategory(String category);
    List<Product> findAllByStockQuantityGreaterThan(int quantity);
    boolean existsBySku(String sku);
}

