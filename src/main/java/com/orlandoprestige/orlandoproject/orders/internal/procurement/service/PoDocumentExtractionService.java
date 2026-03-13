package com.orlandoprestige.orlandoproject.orders.internal.procurement.service;

import com.orlandoprestige.orlandoproject.orders.internal.procurement.dto.ParsedPurchaseOrderDto;

public interface PoDocumentExtractionService {
    ParsedPurchaseOrderDto extractFromDocument(String fileName, String mimeType, byte[] content);
}
