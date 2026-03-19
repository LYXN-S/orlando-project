package com.orlandoprestige.orlandoproject.orders.internal.procurement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.dto.ParsedPurchaseOrderDto;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.dto.SystemPoItemDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service("geminiPoDocumentExtractionService")
public class GeminiPoDocumentExtractionService implements PoDocumentExtractionService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model.name:gemini-3.1-flash-lite}")
    private String geminiModelName;

    @Value("${gemini.model.fallbacks:gemini-1.5-flash}")
    private String geminiModelFallbacks;

    @Value("${gemini.api.timeout:15000}")
    private Integer geminiTimeout;

    @Value("${gemini.api.retries:2}")
    private Integer geminiRetries;

    @Value("${gemini.prompt.max-chars:6000}")
    private Integer geminiPromptMaxChars;

    private static final String JSON_SCHEMA = """
            {
              "customerName": "string",
              "poNumber": "string",
              "poDate": "YYYY-MM-DD or null",
              "deliveryDate": "YYYY-MM-DD or null",
              "currency": "string",
                            "businessAddress": "string or null",
                            "deliveryAddress": "string or null",
              "subtotal": number,
              "tax": number,
              "total": number,
              "confidence": number,
              "warnings": "string",
              "items": [
                {
                  "lineNumber": number,
                  "description": "string",
                  "sku": "string or null",
                  "quantity": number,
                  "unitPrice": number,
                  "lineTotal": number
                }
              ]
            }""";

        private static final String EXTRACTION_RULES = """
                        Extraction rules:
                        - Orlando Prestige is always the supplier/system company.
                        - customerName must be the external company counterparty, never Orlando Prestige.
                        - Support both Request for Quotation (RFQ) and Purchase Order (PO) layouts.
                        - poNumber may appear as RFQ number or PO number after a # sign.
                        - deliveryDate may appear as Delivery Date, Target Date, Date Req., or Needed By.
                        - total may appear as Grand Total, Net Amount, or Total.
                        - subtotal may appear as Subtotal or be derived from line items if missing.
                        - businessAddress should be extracted from billing/company address blocks.
                        - deliveryAddress should be extracted only if explicitly present (e.g., Deliver To/Ship To).
                        - If delivery address is not present in the document, set deliveryAddress to null.
                        - unitPrice may appear as Unit Price or Net Price.
                        - lineTotal may appear as Amount or Subtotal in line item rows.
                        - Extract all visible line items from tabular rows when possible.
                        - Return only JSON and use null for missing dates.
                        """;

    @Override
    public ParsedPurchaseOrderDto extractFromDocument(String fileName, String mimeType, byte[] content) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured.");
        }

        boolean isImage = mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("image/");
        String mode = isImage ? "multimodal/image" : "text";

        log.info("[GEMINI] Model   : {}", geminiModelName);
        log.info("[GEMINI] Mode    : {}", mode);
        log.info("[GEMINI] Timeout : {} ms", geminiTimeout);
        log.info("[GEMINI] Retries : {}", geminiRetries);
        log.info("[GEMINI] Max chars for prompt context : {}", geminiPromptMaxChars);
        log.info("[GEMINI] Calling Gemini API...");

        try (Client client = Client.builder()
                .apiKey(geminiApiKey)
                .httpOptions(com.google.genai.types.HttpOptions.builder().timeout(geminiTimeout).build())
                .build()) {

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .build();

                GenerateContentResponse response = invokeWithModelFallback(client, config, fileName, mimeType, content, isImage);

                log.info("[GEMINI] Response received - parsing JSON...");
            String json = sanitizeJson(response.text());
            JsonNode root = objectMapper.readTree(json);

            List<SystemPoItemDto> items = new ArrayList<>();
            if (root.has("items") && root.get("items").isArray()) {
                int idx = 1;
                for (JsonNode itemNode : root.get("items")) {
                    items.add(new SystemPoItemDto(
                            itemNode.path("lineNumber").isNumber() ? itemNode.path("lineNumber").asInt() : idx,
                            itemNode.path("description").asText("Line item " + idx),
                            itemNode.path("sku").isNull() ? null : itemNode.path("sku").asText(null),
                            itemNode.path("quantity").asInt(1),
                            toBigDecimal(itemNode.path("unitPrice")),
                            toBigDecimal(itemNode.path("lineTotal"))
                    ));
                    idx++;
                }
            }

            if (items.isEmpty()) {
                items.add(new SystemPoItemDto(1, "Unparsed line item from Gemini output", null, 1, BigDecimal.ZERO, BigDecimal.ZERO));
            }

            String extractedCustomerName = root.path("customerName").asText("");
            if (extractedCustomerName.isBlank()) {
                extractedCustomerName = root.path("supplierName").asText("Unknown Customer");
            }

            return new ParsedPurchaseOrderDto(
                    extractedCustomerName,
                    root.path("poNumber").asText("AUTO-" + System.currentTimeMillis()),
                    parseDate(root.path("poDate").asText(null)),
                    parseDate(root.path("deliveryDate").asText(null)),
                    root.path("currency").asText("PHP"),
                    root.path("businessAddress").isNull() ? null : root.path("businessAddress").asText(null),
                    root.path("deliveryAddress").isNull() ? null : root.path("deliveryAddress").asText(null),
                    toBigDecimal(root.path("subtotal")),
                    toBigDecimal(root.path("tax")),
                    toBigDecimal(root.path("total")),
                    items,
                    root.path("confidence").asDouble(0.75),
                    root.path("warnings").asText("Verify extracted fields before confirmation."),
                        json
            );
        } catch (Exception ex) {
            log.error("[GEMINI] Extraction failed: {}", ex.getMessage(), ex);
            log.error("[GEMINI] Cause chain: {}", flattenCauseChain(ex));
            throw new IllegalStateException("Gemini extraction failed: " + ex.getMessage(), ex);
        }
    }

    private GenerateContentResponse invokeWithModelFallback(Client client,
                                                            GenerateContentConfig config,
                                                            String fileName,
                                                            String mimeType,
                                                            byte[] content,
                                                            boolean isImage) {
        List<String> modelsToTry = new ArrayList<>();
        modelsToTry.add(geminiModelName);
        modelsToTry.addAll(Arrays.stream(geminiModelFallbacks.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank() && !s.equals(geminiModelName))
                .toList());

        Exception lastError = null;
        for (String model : modelsToTry) {
            int maxAttempts = Math.max(1, geminiRetries != null ? geminiRetries : 1);
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    log.info("[GEMINI] Trying model: {} (attempt {}/{})", model, attempt, maxAttempts);
                    return invokeGenerateContent(client, config, model, fileName, mimeType, content, isImage, attempt);
                } catch (Exception ex) {
                    lastError = ex;

                    if (isModelNotFound(ex)) {
                        log.warn("[GEMINI] Model unavailable: {}. Trying next fallback if available.", model);
                        break;
                    }

                    if (isNetworkTransportError(ex) && attempt < maxAttempts) {
                        log.warn("[GEMINI] Network/transport error on model {} attempt {}/{}: {}", model, attempt, maxAttempts, ex.getMessage());
                        sleepQuietly(500L * attempt);
                        continue;
                    }

                    throw ex;
                }
            }
        }

        throw new IllegalStateException("No available Gemini model for generateContent. Last error: " +
                (lastError != null ? lastError.getMessage() : "unknown"), lastError);
    }

    private boolean isModelNotFound(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null) {
            return false;
        }
        String m = msg.toLowerCase(Locale.ROOT);
        return m.contains("not found") || m.contains("is not supported for generatecontent") || m.contains("404");
    }

    private boolean isNetworkTransportError(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null) {
            return false;
        }
        String m = msg.toLowerCase(Locale.ROOT);
        return m.contains("failed to execute http request")
                || m.contains("timed out")
                || m.contains("timeout")
                || m.contains("connection reset")
                || m.contains("connection refused")
                || m.contains("unknownhost")
                || m.contains("ssl")
                || m.contains("handshake");
    }

    private GenerateContentResponse invokeGenerateContent(Client client,
                                                          GenerateContentConfig config,
                                                          String model,
                                                          String fileName,
                                                          String mimeType,
                                                          byte[] content,
                                                          boolean isImage,
                                                          int attempt) {
        if (isImage) {
            log.info("[GEMINI] Sending image bytes ({} bytes) as inline multimodal content", content != null ? content.length : 0);
            String instruction = "You are a procurement parser. Read the purchase order in this image and " +
                    "extract its data into strict JSON only.\n" + EXTRACTION_RULES +
                    "Return ONLY valid JSON with this exact structure:\n" + JSON_SCHEMA;
            Content userContent = Content.fromParts(
                    Part.fromText(instruction),
                    Part.fromBytes(content, mimeType)
            );
            return client.models.generateContent(model, userContent, config);
        }

        int configuredMaxChars = geminiPromptMaxChars != null ? geminiPromptMaxChars : 6000;
        int attemptMaxChars = Math.max(1500, configuredMaxChars / Math.max(1, attempt));
        String contentPreview = summarizeContent(fileName, mimeType, content, attemptMaxChars);
        log.info("[GEMINI] Sending text content ({} chars, budget={} chars, attempt={})", contentPreview.length(), attemptMaxChars, attempt);
        String prompt = """
                You are a procurement parser. Extract a purchase order into strict JSON only.
            %s
                Return ONLY valid JSON with this exact structure:
                %s

                Source file name: %s
                Source mime type: %s
                Source text/content preview:
                %s
            """.formatted(EXTRACTION_RULES, JSON_SCHEMA, fileName, mimeType, contentPreview);
        return client.models.generateContent(model, prompt, config);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private String flattenCauseChain(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            if (!sb.isEmpty()) {
                sb.append(" -> ");
            }
            sb.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage());
            current = current.getCause();
        }
        return sb.toString();
    }

    private String summarizeContent(String fileName, String mimeType, byte[] content, int maxChars) {
        if (content == null || content.length == 0) {
            return "";
        }

        String safeMime = mimeType != null ? mimeType.toLowerCase(Locale.ROOT) : "";
        boolean isPdf = safeMime.contains("pdf") || (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf"));

        if (isPdf) {
            try (PDDocument document = Loader.loadPDF(content)) {
                String text = new PDFTextStripper().getText(document);
                int max = Math.min(text.length(), maxChars);
                String trimmed = text.length() <= max ? text : text.substring(0, max);
                log.info("[GEMINI] Extracted {} chars from PDF ({} page(s)) for prompt", trimmed.length(), document.getNumberOfPages());
                return trimmed;
            } catch (Exception ex) {
                log.warn("[GEMINI] PDF text extraction failed, falling back to byte preview: {}", ex.getMessage());
            }
        }

        int max = Math.min(content.length, maxChars);
        String raw = new String(content, StandardCharsets.UTF_8);
        raw = raw.replace("\u0000", " ");
        return raw.length() <= max ? raw : raw.substring(0, max);
    }

    private String sanitizeJson(String value) {
        if (value == null) {
            return "{}";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "").replaceFirst("^```", "");
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        return trimmed;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return BigDecimal.ZERO;
        }
        try {
            if (node.isNumber()) {
                return node.decimalValue();
            }
            return new BigDecimal(node.asText("0").replace(",", ""));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }
}
