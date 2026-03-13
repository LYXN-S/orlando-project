package com.orlandoprestige.orlandoproject.orders.internal.procurement.service;

import com.orlandoprestige.orlandoproject.orders.internal.procurement.dto.ParsedPurchaseOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class RoutingPoDocumentExtractionService implements PoDocumentExtractionService {

    private final HeuristicPoDocumentExtractionService heuristicService;
    private final GeminiPoDocumentExtractionService geminiService;

    @Value("${app.procurement.extraction.mode:auto}")
    private String extractionMode;

    @Override
    public ParsedPurchaseOrderDto extractFromDocument(String fileName, String mimeType, byte[] content) {
        int sizeKb = content != null ? content.length / 1024 : 0;
        boolean preferGemini = "gemini".equalsIgnoreCase(extractionMode) || "auto".equalsIgnoreCase(extractionMode);

        log.info("======================================================");
        log.info("[PO EXTRACTION] File       : {}", fileName);
        log.info("[PO EXTRACTION] MIME Type  : {}", mimeType);
        log.info("[PO EXTRACTION] Size       : {} KB", sizeKb);
        log.info("[PO EXTRACTION] Mode       : {} (configured: {})", preferGemini ? "Gemini AI" : "Heuristic", extractionMode);

        if (!preferGemini) {
            log.info("[PO EXTRACTION] -> Using heuristic extractor (mode={})", extractionMode);
            ParsedPurchaseOrderDto result = heuristicService.extractFromDocument(fileName, mimeType, content);
            logResult("HEURISTIC", result);
            return result;
        }

        try {
            log.info("[PO EXTRACTION] -> Sending to Gemini AI...");
            ParsedPurchaseOrderDto result = geminiService.extractFromDocument(fileName, mimeType, content);
            logResult("GEMINI", result);
            return result;
        } catch (Exception ex) {
            log.warn("[PO EXTRACTION] Gemini failed - falling back to heuristic. Reason: {}", ex.getMessage());
            log.debug("[PO EXTRACTION] Gemini exception detail:", ex);
            ParsedPurchaseOrderDto result = heuristicService.extractFromDocument(fileName, mimeType, content);
            logResult("HEURISTIC (fallback)", result);
            return result;
        }
    }

    private void logResult(String engine, ParsedPurchaseOrderDto result) {
        log.info("[PO EXTRACTION] {} extraction complete", engine);
        log.info("[PO EXTRACTION]   Customer   : {}", result.customerName());
        log.info("[PO EXTRACTION]   PO Number  : {}", result.poNumber());
        log.info("[PO EXTRACTION]   Items      : {}", result.items() != null ? result.items().size() : 0);
        log.info("[PO EXTRACTION]   Total      : {}", result.total());
        log.info("[PO EXTRACTION]   Confidence : {}", String.format("%.0f%%", result.confidence() * 100));
        if (result.warnings() != null && !result.warnings().isBlank()) {
            log.warn("[PO EXTRACTION]   Warnings   : {}", result.warnings());
        }
        log.info("======================================================");
    }
}
