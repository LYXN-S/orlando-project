package com.orlandoprestige.orlandoproject.catalog.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.catalog.internal.domain.ProductAvailabilityStatus;
import com.orlandoprestige.orlandoproject.catalog.internal.domain.Product;
import com.orlandoprestige.orlandoproject.catalog.internal.domain.ProductImage;
import com.orlandoprestige.orlandoproject.catalog.internal.presentation.dto.*;
import com.orlandoprestige.orlandoproject.catalog.internal.service.ProductImageService;
import com.orlandoprestige.orlandoproject.catalog.internal.service.ProductService;
import com.orlandoprestige.orlandoproject.inventory.InventoryFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Product catalog endpoints")
public class ProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;
    private final InventoryFacade inventoryFacade;

    @GetMapping
    @Operation(summary = "Get all active products (public)")
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String category) {
        boolean includeUnavailable = canViewStock(user);
        List<Product> products;
        if (includeUnavailable) {
            products = category != null ? productService.findByCategory(category) : productService.findAll();
        } else {
            products = category != null ? productService.findAvailableByCategory(category) : productService.findAllAvailable();
        }
        boolean includeStock = canViewStock(user);
        return ResponseEntity.ok(products.stream().map(p -> toDto(p, includeStock)).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID (public)")
    public ResponseEntity<ProductDto> getProductById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        boolean includeStock = canViewStock(user);
        return productService.findById(id)
            .filter(product -> canViewStock(user) || product.getAvailabilityStatus() == ProductAvailabilityStatus.AVAILABLE)
                .map(product -> toDto(product, includeStock))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_PRODUCTS')")
    @Operation(summary = "Create a new product (Staff with permission)")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductDto dto) {
        if (productService.existsBySku(dto.sku())) {
            throw new IllegalArgumentException("SKU already exists: " + dto.sku());
        }
        Product product = new Product();
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setSku(dto.sku());
        product.setPrice(dto.price());
        product.setStockQuantity(0);
        product.setCategory(dto.category());
        product.setAvailabilityStatus(dto.availabilityStatus() != null
            ? ProductAvailabilityStatus.valueOf(dto.availabilityStatus())
            : ProductAvailabilityStatus.AVAILABLE);
        product.setAvailabilityUpdatedBy(null);
        Product saved = productService.save(product);
        inventoryFacade.createOrUpdate(saved.getId(), saved.getName(), saved.getSku(), dto.stockQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_PRODUCTS')")
    @Operation(summary = "Update a product (Staff with permission)")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductDto dto) {
        return productService.findById(id)
                .map(product -> {
                    product.setName(dto.name());
                    product.setDescription(dto.description());
                    product.setPrice(dto.price());
                    product.setCategory(dto.category());
                    if (dto.availabilityStatus() != null) {
                        product.setAvailabilityStatus(ProductAvailabilityStatus.valueOf(dto.availabilityStatus()));
                    }
                    Product updated = productService.save(product);
                    inventoryFacade.createOrUpdate(updated.getId(), updated.getName(), updated.getSku(), updated.getStockQuantity());
                    return ResponseEntity.ok(toDto(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_PRODUCTS')")
    @Operation(summary = "Toggle product availability")
    public ResponseEntity<ProductDto> updateAvailability(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProductAvailabilityUpdateDto dto) {
        ProductAvailabilityStatus status = ProductAvailabilityStatus.valueOf(dto.availabilityStatus());
        Product updated = productService.updateAvailability(id, status, user.userId());
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_PRODUCTS')")
    @Operation(summary = "Soft-delete a product (Staff with permission)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_INVENTORY')")
    @Operation(summary = "Adjust product stock quantity via inventory module (Staff with inventory permission)")
    public ResponseEntity<ProductDto> adjustStock(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody StockAdjustmentDto dto) {
        if (!productService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // Delegate to inventory module — it handles movement logging and catalog sync
        inventoryFacade.adjustStock(id, dto.adjustment(), "Stock adjustment via product endpoint", user.userId());
        // Re-fetch product to return updated data
        return productService.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_PRODUCTS')")
    @Operation(summary = "Upload an image for a product (Staff with permission)")
    public ResponseEntity<ImageDto> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        Product product = productService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        ProductImage image = productImageService.saveImage(product, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(toImageDto(id, image));
    }

    @GetMapping("/{id}/images/{imageId}")
    @Operation(summary = "Get product image file (public)")
    public ResponseEntity<Resource> getImage(
            @PathVariable Long id,
            @PathVariable Long imageId) throws IOException {
        ProductImage image = productImageService.getImageById(imageId);
        Path filePath = Paths.get(image.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_PRODUCTS')")
    @Operation(summary = "Delete a product image (Staff with permission)")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId) throws IOException {
        productImageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    private ProductDto toDto(Product product) {
        return toDto(product, true);
    }

    private ProductDto toDto(Product product, boolean includeStock) {
        List<ImageDto> imageDtos;
        try {
            imageDtos = productImageService.getImagesByProductId(product.getId()).stream()
                    .map(img -> toImageDto(product.getId(), img))
                    .toList();
        } catch (Exception e) {
            imageDtos = Collections.emptyList();
        }
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getPrice(),
                includeStock ? product.getStockQuantity() : null,
                product.getCategory(),
                product.getAvailabilityStatus() != null ? product.getAvailabilityStatus().name() : ProductAvailabilityStatus.AVAILABLE.name(),
                product.getAvailabilityUpdatedBy(),
                imageDtos
        );
    }

    private boolean canViewStock(AuthenticatedUser user) {
        return user != null && ("ROLE_SUPER_ADMIN".equals(user.role()) || "ROLE_STAFF".equals(user.role()));
    }

    private ImageDto toImageDto(Long productId, ProductImage image) {
        String url = "/api/v1/catalog/products/" + productId + "/images/" + image.getId();
        return new ImageDto(image.getId(), url, image.getDisplayOrder());
    }
}

