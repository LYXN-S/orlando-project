package com.orlandoprestige.orlandoproject.catalog.internal.service;

import com.orlandoprestige.orlandoproject.catalog.internal.domain.Product;
import com.orlandoprestige.orlandoproject.catalog.internal.domain.ProductAvailabilityStatus;
import com.orlandoprestige.orlandoproject.catalog.internal.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Product> findAllAvailable() {
        return productRepository.findAll().stream()
                .filter(p -> p.getAvailabilityStatus() == null || p.getAvailabilityStatus() == ProductAvailabilityStatus.AVAILABLE)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> findAvailableByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .filter(p -> p.getAvailabilityStatus() == null || p.getAvailabilityStatus() == ProductAvailabilityStatus.AVAILABLE)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return productRepository.existsById(id);
    }

    public boolean existsBySku(String sku) {
        return productRepository.existsBySku(sku);
    }

    @Transactional
    public void decrementStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        if (product.getStockQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + product.getName());
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
    }

    @Transactional
    public void incrementStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    @Transactional
    public Product updateAvailability(Long productId, ProductAvailabilityStatus status, Long staffId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        product.setAvailabilityStatus(status);
        product.setAvailabilityUpdatedBy(staffId);
        return productRepository.save(product);
    }

    @Transactional
    public Product updateOperationalFields(Long productId, String name, String sku, String category) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        product.setName(name);
        product.setSku(sku);
        product.setCategory(category);
        return productRepository.save(product);
    }
}

