package com.orlandoprestige.orlandoproject.orders.internal.procurement.service;

import com.orlandoprestige.orlandoproject.orders.internal.procurement.dto.ParsedPurchaseOrderDto;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.dto.SystemPoItemDto;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service("heuristicPoDocumentExtractionService")
public class HeuristicPoDocumentExtractionService implements PoDocumentExtractionService {

    private static final Pattern CUSTOMER_PATTERN = Pattern.compile("(?im)^(customer|client|buyer|company)\\s*[:\\-]\\s*(.+)$");
    private static final Pattern PO_NUMBER_PATTERN = Pattern.compile("(?im)(request\\s+for\\s+quotation|purchase\\s+order)\\s*#?\\s*([A-Z0-9-]{4,})|^(po\\s*(no|number)?|rfq\\s*(no|number)?)\\s*[:#\\-]\\s*(.+)$");
    private static final Pattern PO_DATE_PATTERN = Pattern.compile("(?im)^(po\\s*date|date|order\\s*deadline)\\s*[:\\-]\\s*(.+)$");
    private static final Pattern DELIVERY_DATE_PATTERN = Pattern.compile("(?im)^(delivery\\s*date|target\\s*date|needed\\s*by|due\\s*date|date\\s*req\\.?)\\s*[:\\-]\\s*(.+)$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("(?i)\\b(PHP|USD|EUR|GBP|JPY|AUD|CAD)\\b");
    private static final Pattern TOTAL_PATTERN = Pattern.compile("(?im)(grand\\s*total|net\\s*amount|total)\\s*[:\\-]?\\s*(?:PHP|₱)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)");
    private static final Pattern SUBTOTAL_PATTERN = Pattern.compile("(?im)(subtotal|sub\\s*total)\\s*[:\\-]?\\s*(?:PHP|₱)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)");
    private static final Pattern TAX_PATTERN = Pattern.compile("(?im)^(tax|vat)\\s*[:\\-]\\s*([0-9,]+(?:\\.[0-9]{1,2})?)$");
    private static final Pattern DATE_TOKEN_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/\\d{4})");
    private static final Pattern RFQ_ITEM_PATTERN = Pattern.compile("(?im)^\\s*(?:[A-Z0-9-]+\\s+)?([A-Za-z][A-Za-z0-9 .,/()%-]+?)\\s+(\\d+(?:\\.\\d+)?)\\s+(pc|pcs|piece|pieces|case|cases|unit|units|kg|g|l|ml|btl|bottle)\\s+([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:₱\\s*)?([0-9,]+(?:\\.[0-9]{1,2})?)\\s*$");
    private static final Pattern PO_ITEM_PATTERN = Pattern.compile("(?im)^\\s*\\d+\\s+\\S+\\s+\\S+\\s+(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s+(CASE|PC|PCS|UNIT|UNITS|KG|G|L|ML|BTL|BOTTLE)\\s+\\d+(?:\\.\\d+)?\\s+([0-9,]+(?:\\.[0-9]{1,2})?)\\s+([0-9,]+(?:\\.[0-9]{1,2})?)\\s*$");
    private static final Pattern ORLANDO_PATTERN = Pattern.compile("(?i)\\borlando\\s+prestige\\b");

    @Override
    public ParsedPurchaseOrderDto extractFromDocument(String fileName, String mimeType, byte[] content) {
        log.info("[HEURISTIC] Starting regex/pattern extraction");

        String text = extractText(fileName, mimeType, content);
        log.info("[HEURISTIC] Extracted {} chars of text from document", text.length());
        if (text.isBlank()) {
            log.warn("[HEURISTIC] No readable text extracted — all fields will be empty/defaults");
        }

        String customerName = extractCustomerName(text);
        String poNumber = extractPoNumber(text).orElse("AUTO-" + System.currentTimeMillis());
        LocalDate poDate = findDate(findGroup(PO_DATE_PATTERN, text, 2).orElse(null)).orElse(LocalDate.now());
        LocalDate deliveryDate = findDate(findGroup(DELIVERY_DATE_PATTERN, text, 2).orElse(null)).orElse(null);
        String currency = findGroup(CURRENCY_PATTERN, text, 1).orElse("PHP");

        BigDecimal subtotal = findAmount(SUBTOTAL_PATTERN, text, 2).orElse(BigDecimal.ZERO);
        BigDecimal tax = findAmount(TAX_PATTERN, text, 2).orElse(BigDecimal.ZERO);
        BigDecimal total = findAmount(TOTAL_PATTERN, text, 2).orElse(BigDecimal.ZERO);

        log.debug("[HEURISTIC] Pattern matches — customer='{}', poNumber='{}', currency='{}', total={}",
                customerName, poNumber, currency, total);

        List<SystemPoItemDto> items = extractItems(text);
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0 && !items.isEmpty()) {
            subtotal = items.stream().map(SystemPoItemDto::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0 && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            total = subtotal.add(tax);
        }
        if (items.isEmpty()) {
            log.warn("[HEURISTIC] No line items detected; inserting placeholder");
            items.add(new SystemPoItemDto(1, "Unparsed line item from uploaded PO", null, 1, BigDecimal.ZERO, BigDecimal.ZERO));
        } else {
            log.info("[HEURISTIC] Found {} line item(s)", items.size());
        }

        double confidence = computeConfidence(customerName, poNumber, poDate, total, items, mimeType);
        String warnings = buildWarnings(confidence, mimeType, text, items.size());
        log.info("[HEURISTIC] Confidence score: {}", String.format("%.0f%%", confidence * 100));

        return new ParsedPurchaseOrderDto(
                customerName,
                poNumber,
                poDate,
                deliveryDate,
                currency,
                subtotal,
                tax,
                total,
                items,
                confidence,
                warnings,
                "{\"extractor\":\"heuristic-parser\",\"fileName\":\"" + fileName + "\",\"mimeType\":\"" + mimeType + "\",\"textLength\":" + text.length() + "}"
        );
    }

    private String extractText(String fileName, String mimeType, byte[] content) {
        try {
            String safeMime = mimeType != null ? mimeType.toLowerCase(Locale.ROOT) : "";
            if (safeMime.contains("pdf") || (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf"))) {
                log.info("[HEURISTIC] Parsing PDF with PDFBox");
                try (PDDocument document = Loader.loadPDF(content)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String text = stripper.getText(document);
                    log.info("[HEURISTIC] PDFBox extracted {} chars from {} page(s)", text.length(), document.getNumberOfPages());
                    return text;
                }
            }

            if (safeMime.startsWith("image/")) {
                log.warn("[HEURISTIC] Image file received — heuristic extractor cannot read images (no OCR). " +
                         "Enable Gemini mode (PO_EXTRACTION_MODE=auto) for image support.");
                return "";
            }

            log.info("[HEURISTIC] Reading document as plain text");
            return new String(content, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("[HEURISTIC] Failed to extract text: {}", ex.getMessage());
            return "";
        }
    }

    private List<SystemPoItemDto> extractItems(String text) {
        List<SystemPoItemDto> items = new ArrayList<>();
        int cap = 1;

        Matcher poMatcher = PO_ITEM_PATTERN.matcher(text);
        while (poMatcher.find() && items.size() < 30) {
            String description = normalizeDescription(poMatcher.group(1), cap);
            int qty = parseIntSafe(poMatcher.group(2), 1);
            BigDecimal unitPrice = parseAmountSafe(poMatcher.group(4)).orElse(BigDecimal.ZERO);
            BigDecimal amount = parseAmountSafe(poMatcher.group(5)).orElse(unitPrice.multiply(BigDecimal.valueOf(qty)));
            items.add(new SystemPoItemDto(cap++, description, null, qty, unitPrice, amount));
        }

        Matcher rfqMatcher = RFQ_ITEM_PATTERN.matcher(text);
        while (rfqMatcher.find() && items.size() < 30) {
            String description = normalizeDescription(rfqMatcher.group(1), cap);
            if (items.stream().anyMatch(existing -> existing.description().equalsIgnoreCase(description))) {
                continue;
            }
            int qty = parseIntSafe(rfqMatcher.group(2), 1);
            BigDecimal unitPrice = parseAmountSafe(rfqMatcher.group(4)).orElse(BigDecimal.ZERO);
            BigDecimal amount = parseAmountSafe(rfqMatcher.group(5)).orElse(unitPrice.multiply(BigDecimal.valueOf(qty)));
            items.add(new SystemPoItemDto(cap++, description, null, qty, unitPrice, amount));
        }
        return items;
    }

    private String extractCustomerName(String text) {
        Optional<String> explicit = findGroup(CUSTOMER_PATTERN, text, 2).filter(this::isExternalCompany);
        if (explicit.isPresent()) {
            return explicit.get();
        }

        List<String> candidates = text.lines()
                .map(String::trim)
                .map(line -> line.replaceAll("\\s+", " "))
                .filter(line -> line.length() >= 4 && line.length() <= 80)
                .filter(this::isExternalCompany)
                .distinct()
                .toList();

        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase("Kitchen City") || candidate.toUpperCase(Locale.ROOT).contains("INC")) {
                return candidate;
            }
        }

        return candidates.isEmpty() ? "Unknown Customer" : candidates.getFirst();
    }

    private boolean isExternalCompany(String value) {
        String line = value == null ? "" : value.trim();
        if (line.isBlank()) {
            return false;
        }
        if (ORLANDO_PATTERN.matcher(line).find()) {
            return false;
        }
        String lowered = line.toLowerCase(Locale.ROOT);
        return !lowered.contains("address")
                && !lowered.contains("deliver to")
                && !lowered.contains("shipping address")
                && !lowered.contains("purchase representative")
                && !lowered.contains("order deadline")
                && !lowered.contains("terms")
                && !lowered.contains("discount")
                && !lowered.contains("warehouse")
                && !lowered.contains("prepared by")
                && !lowered.contains("checked by")
                && !lowered.contains("approved by")
                && !lowered.contains("page ")
                && !lowered.matches(".*\\d{2}/\\d{2}/\\d{4}.*")
                && line.chars().anyMatch(Character::isLetter);
    }

    private Optional<String> extractPoNumber(String text) {
        Matcher matcher = PO_NUMBER_PATTERN.matcher(text != null ? text : "");
        while (matcher.find()) {
            String number = firstNonBlank(matcher.group(2), matcher.group(6));
            if (number != null) {
                return Optional.of(number.replace("#", "").trim());
            }
        }
        Matcher genericHash = Pattern.compile("#\\s*([A-Z0-9-]{4,})").matcher(text != null ? text : "");
        return genericHash.find() ? Optional.of(genericHash.group(1).trim()) : Optional.empty();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeDescription(String raw, int fallbackIndex) {
        if (raw == null || raw.isBlank()) {
            return "Line item " + fallbackIndex;
        }
        return raw.replaceAll("\\s+", " ").trim();
    }

    private double computeConfidence(String customerName, String poNumber, LocalDate poDate, BigDecimal total,
                                     List<SystemPoItemDto> items, String mimeType) {
        double score = 0.2;
        if (customerName != null && !customerName.isBlank() && !"Unknown Customer".equalsIgnoreCase(customerName)) score += 0.2;
        if (poNumber != null && !poNumber.startsWith("AUTO-")) score += 0.2;
        if (poDate != null) score += 0.1;
        if (total != null && total.compareTo(BigDecimal.ZERO) > 0) score += 0.15;
        if (items != null && !items.isEmpty()) score += 0.15;

        String safeMime = mimeType != null ? mimeType.toLowerCase(Locale.ROOT) : "";
        if (safeMime.startsWith("image/")) {
            score -= 0.15;
        }
        return Math.max(0.1, Math.min(0.95, score));
    }

    private String buildWarnings(double confidence, String mimeType, String text, int itemCount) {
        List<String> warnings = new ArrayList<>();
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            warnings.add("Image OCR is not configured yet; extracted values may be incomplete.");
        }
        if (text == null || text.isBlank()) {
            warnings.add("No readable text was extracted from document.");
        }
        if (itemCount == 0) {
            warnings.add("No clear line items detected; review item list manually.");
        }
        if (confidence < 0.7) {
            warnings.add("Low-confidence extraction. Verify customer, dates, and totals before confirmation.");
        }
        return String.join(" ", warnings);
    }

    private Optional<String> findGroup(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text != null ? text : "");
        if (matcher.find()) {
            String value = matcher.group(group);
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> findAmount(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text != null ? text : "");
        if (matcher.find()) {
            return parseAmountSafe(matcher.group(group));
        }
        return Optional.empty();
    }

    private Optional<LocalDate> findDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim();
        Matcher token = DATE_TOKEN_PATTERN.matcher(value);
        if (token.find()) {
            value = token.group(1);
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return Optional.of(LocalDate.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> parseAmountSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(raw.replace(",", "").trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private int parseIntSafe(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
