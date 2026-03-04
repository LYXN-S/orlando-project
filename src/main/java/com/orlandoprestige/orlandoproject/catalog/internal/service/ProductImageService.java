package com.orlandoprestige.orlandoproject.catalog.internal.service;

import com.orlandoprestige.orlandoproject.catalog.internal.domain.Product;
import com.orlandoprestige.orlandoproject.catalog.internal.domain.ProductImage;
import com.orlandoprestige.orlandoproject.catalog.internal.repository.ProductImageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final String UPLOAD_DIR = "uploads/products";
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final ProductImageRepository imageRepository;

    @Transactional
    public ProductImage saveImage(Product product, MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("File type not allowed. Allowed: JPEG, PNG, WebP");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        // Create directory
        Path uploadPath = Paths.get(UPLOAD_DIR, product.getId().toString());
        Files.createDirectories(uploadPath);

        // Generate unique filename
        String extension = getExtension(file.getOriginalFilename());
        String uniqueFilename = UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(uniqueFilename);

        // Save file to disk
        Files.copy(file.getInputStream(), filePath);

        // Save metadata to DB
        int displayOrder = imageRepository.countByProductId(product.getId());
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setFilename(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setFilePath(filePath.toString());
        image.setDisplayOrder(displayOrder);

        return imageRepository.save(image);
    }

    @Transactional(readOnly = true)
    public List<ProductImage> getImagesByProductId(Long productId) {
        return imageRepository.findByProductIdOrderByDisplayOrderAsc(productId);
    }

    @Transactional(readOnly = true)
    public ProductImage getImageById(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found: " + imageId));
    }

    @Transactional
    public void deleteImage(Long imageId) throws IOException {
        ProductImage image = getImageById(imageId);
        // Delete file from disk
        Path filePath = Paths.get(image.getFilePath());
        Files.deleteIfExists(filePath);
        // Delete from DB
        imageRepository.delete(image);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
