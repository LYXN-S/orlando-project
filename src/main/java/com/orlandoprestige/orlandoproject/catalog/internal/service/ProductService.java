package com.orlandoprestige.orlandoproject.catalog.internal.service;

import com.orlandoprestige.orlandoproject.catalog.internal.domain.Product;
import com.orlandoprestige.orlandoproject.catalog.internal.domain.ProductAvailabilityStatus;
import com.orlandoprestige.orlandoproject.catalog.internal.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Cacheable("productLists")
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable("catalogPages")
    public Page<Product> findCatalogPage(String category, String search, Boolean bestSeller, Boolean excludeBestSeller, boolean includeUnavailable, int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sortBy));

        Specification<Product> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), pattern)
                ));
            }

            if (Boolean.TRUE.equals(bestSeller)) {
                predicates.add(criteriaBuilder.isTrue(root.get("bestSeller")));
            }

            if (Boolean.TRUE.equals(excludeBestSeller)) {
                predicates.add(criteriaBuilder.isFalse(root.get("bestSeller")));
            }

            if (!includeUnavailable) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("availabilityStatus")),
                        criteriaBuilder.equal(root.get("availabilityStatus"), ProductAvailabilityStatus.AVAILABLE)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productLists", key = "'category:' + #category")
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productLists", key = "'available'")
    public List<Product> findAllAvailable() {
        return productRepository.findAll().stream()
                .filter(p -> p.getAvailabilityStatus() == null || p.getAvailabilityStatus() == ProductAvailabilityStatus.AVAILABLE)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productLists", key = "'available:category:' + #category")
    public List<Product> findAvailableByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .filter(p -> p.getAvailabilityStatus() == null || p.getAvailabilityStatus() == ProductAvailabilityStatus.AVAILABLE)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productById", key = "#id")
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productBySku", key = "#sku")
    public Optional<Product> findBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "catalogPages", allEntries = true),
            @CacheEvict(value = "productById", allEntries = true),
            @CacheEvict(value = "productBySku", allEntries = true),
            @CacheEvict(value = "productLists", allEntries = true)
    })
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "catalogPages", allEntries = true),
            @CacheEvict(value = "productById", allEntries = true),
            @CacheEvict(value = "productBySku", allEntries = true),
            @CacheEvict(value = "productLists", allEntries = true)
    })
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
    @Caching(evict = {
            @CacheEvict(value = "catalogPages", allEntries = true),
            @CacheEvict(value = "productById", allEntries = true),
            @CacheEvict(value = "productBySku", allEntries = true),
            @CacheEvict(value = "productLists", allEntries = true)
    })
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
        @Caching(evict = {
            @CacheEvict(value = "catalogPages", allEntries = true),
            @CacheEvict(value = "productById", allEntries = true),
            @CacheEvict(value = "productBySku", allEntries = true),
            @CacheEvict(value = "productLists", allEntries = true)
        })
    public void incrementStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    @Transactional
        @Caching(evict = {
            @CacheEvict(value = "catalogPages", allEntries = true),
            @CacheEvict(value = "productById", allEntries = true),
            @CacheEvict(value = "productBySku", allEntries = true),
            @CacheEvict(value = "productLists", allEntries = true)
        })
    public Product updateAvailability(Long productId, ProductAvailabilityStatus status, Long staffId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        product.setAvailabilityStatus(status);
        product.setAvailabilityUpdatedBy(staffId);
        return productRepository.save(product);
    }

    @Transactional
        @Caching(evict = {
            @CacheEvict(value = "catalogPages", allEntries = true),
            @CacheEvict(value = "productById", allEntries = true),
            @CacheEvict(value = "productBySku", allEntries = true),
            @CacheEvict(value = "productLists", allEntries = true)
        })
    public Product updateOperationalFields(Long productId, String name, String sku, String category) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        product.setName(name);
        product.setSku(sku);
        product.setCategory(category);
        return productRepository.save(product);
    }

    @Transactional
        @Caching(evict = {
            @CacheEvict(value = "catalogPages", allEntries = true),
            @CacheEvict(value = "productById", allEntries = true),
            @CacheEvict(value = "productBySku", allEntries = true),
            @CacheEvict(value = "productLists", allEntries = true)
        })
    public Product updateBestSeller(Long productId, boolean bestSeller) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        product.setBestSeller(bestSeller);
        return productRepository.save(product);
    }

    private Sort resolveSort(String sortBy) {
        if ("best-seller".equalsIgnoreCase(sortBy) || "featured".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Order.desc("bestSeller"), Sort.Order.desc("id"));
        }
        if ("alpha-asc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.ASC, "name");
        }
        if ("alpha-desc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "name");
        }
        if ("price-asc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.ASC, "price");
        }
        if ("price-desc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "price");
        }
        return Sort.by(Sort.Direction.DESC, "id");
    }
}

