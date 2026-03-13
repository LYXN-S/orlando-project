package com.orlandoprestige.orlandoproject.catalog;

import com.orlandoprestige.orlandoproject.catalog.dto.ProductInfoDto;
import com.orlandoprestige.orlandoproject.catalog.internal.domain.ProductAvailabilityStatus;
import com.orlandoprestige.orlandoproject.catalog.internal.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Public facade for the Catalog module.
 * Other modules (Cart, Orders, Inventory) must use this class to interact with product data.
 */
@Service
@RequiredArgsConstructor
public class CatalogFacade {

    private final ProductService productService;

    public List<ProductInfoDto> findAll() {
        return productService.findAll().stream()
                .map(p -> new ProductInfoDto(p.getId(), p.getName(), p.getSku(), p.getPrice(), p.getStockQuantity(), p.getCategory()))
                .toList();
    }

    public Optional<ProductInfoDto> findById(Long productId) {
        return productService.findById(productId)
                .map(p -> new ProductInfoDto(p.getId(), p.getName(), p.getSku(), p.getPrice(), p.getStockQuantity(), p.getCategory()));
    }

    public boolean existsById(Long productId) {
        return productService.existsById(productId);
    }

    public void decrementStock(Long productId, int quantity) {
        productService.decrementStock(productId, quantity);
    }

    public void incrementStock(Long productId, int quantity) {
        productService.incrementStock(productId, quantity);
    }

    public void updateOperationalFields(Long productId, String name, String sku, String category) {
        productService.updateOperationalFields(productId, name, sku, category);
    }

    public void updateAvailability(Long productId, ProductAvailabilityStatus status, Long staffId) {
        productService.updateAvailability(productId, status, staffId);
    }
}

