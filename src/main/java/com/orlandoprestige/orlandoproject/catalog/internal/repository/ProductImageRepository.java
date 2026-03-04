package com.orlandoprestige.orlandoproject.catalog.internal.repository;

import com.orlandoprestige.orlandoproject.catalog.internal.domain.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);
    int countByProductId(Long productId);
}
