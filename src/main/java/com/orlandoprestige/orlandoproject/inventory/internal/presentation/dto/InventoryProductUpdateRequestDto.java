package com.orlandoprestige.orlandoproject.inventory.internal.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record InventoryProductUpdateRequestDto(
        @NotBlank(message = "Product name is required") String productName,
        @NotBlank(message = "SKU is required") String sku,
        @NotBlank(message = "Category is required") String category,
        @Min(value = 0, message = "Reorder level cannot be negative") int reorderLevel
) {
}
