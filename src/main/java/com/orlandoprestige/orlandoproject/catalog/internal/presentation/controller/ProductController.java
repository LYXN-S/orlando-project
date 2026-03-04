package com.orlandoprestige.orlandoproject.catalog.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.catalog.internal.domain.Product;
import com.orlandoprestige.orlandoproject.catalog.internal.domain.ProductImage;
import com.orlandoprestige.orlandoproject.catalog.internal.presentation.dto.*;
import com.orlandoprestige.orlandoproject.catalog.internal.service.ProductImageService;
import com.orlandoprestige.orlandoproject.catalog.internal.service.ProductService;
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

    @GetMapping
    @Operation(summary = "Get all active products (public)")
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(required = false) String category) {
        List<Product> products = category != null
                ? productService.findByCategory(category)
                : productService.findAll();
        return ResponseEntity.ok(products.stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID (public)")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return productService.findById(id)
                .map(this::toDto)
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
        product.setStockQuantity(dto.stockQuantity());
        product.setCategory(dto.category());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(productService.save(product)));
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
                    product.setStockQuantity(dto.stockQuantity());
                    product.setCategory(dto.category());
                    return ResponseEntity.ok(toDto(productService.save(product)));
                })
                .orElse(ResponseEntity.notFound().build());
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
    @Operation(summary = "Adjust product stock quantity (Staff with inventory permission)")
    public ResponseEntity<ProductDto> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentDto dto) {
        return productService.findById(id)
                .map(product -> {
                    int newStock = product.getStockQuantity() + dto.adjustment();
                    if (newStock < 0) {
                        throw new IllegalArgumentException("Stock cannot be negative. Current: "
                                + product.getStockQuantity() + ", adjustment: " + dto.adjustment());
                    }
                    product.setStockQuantity(newStock);
                    return ResponseEntity.ok(toDto(productService.save(product)));
                })
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
                product.getStockQuantity(),
                product.getCategory(),
                imageDtos
        );
    }

    private ImageDto toImageDto(Long productId, ProductImage image) {
        String url = "/api/v1/catalog/products/" + productId + "/images/" + image.getId();
        return new ImageDto(image.getId(), url, image.getDisplayOrder());
    }
}

