package com.orlandoprestige.orlandoproject.orders.internal.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class SalesInvoicePdfService {

    private static final float CM_TO_POINTS = 72f / 2.54f;
    private static final float PAGE_WIDTH = 24.53f * CM_TO_POINTS;
    private static final float PAGE_HEIGHT = 31.86f * CM_TO_POINTS;
    private static final PDRectangle INVOICE_PAGE_SIZE = new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT);
    private static final String TEMPLATE_RESOURCE = "/sales_invoice.webp";

    private static final float PAGE_MARGIN = 40f;
    private static final float APPROVED_BY_X = 60f;
    private static final float PREPARED_BY_X = 230f;
    private static final float ROW_HEIGHT = 16f;
    private static final float MIN_Y = 70f;
    private static final float DEFAULT_POINTS_TO_DROP = 14f;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);

    public byte[] render(SalesInvoicePdfPayload payload) {
        try (PDDocument document = new PDDocument()) {
            PDImageXObject invoiceTemplate = loadInvoiceTemplate(document);
            PdfCursor cursor = addNewPage(document, invoiceTemplate);

            cursor = addEmptyLine(cursor, 187.2f);
           
            drawCell(cursor, 500f, cursor.y, safe(formatInvoiceDate(payload.invoiceDate())), 11);
            cursor = addEmptyLine(cursor, 47.7f);
            
            cursor = drawCell(cursor, 200f, cursor.y, safe(payload.registeredName()), 11, true, 2, 12f );
            cursor = drawCell(cursor, 200f, cursor.y, safe(payload.tin()), 11, 2, 12f);
            cursor = drawCell(cursor, 200f, cursor.y, safe(payload.businessAddress()), 11, 2, 13f);
            drawCell(cursor, 200f, cursor.y, safe(payload.terms()), 11);
            drawCell(cursor, 500f, cursor.y, safe(payload.poReference()), 11, 14);

            cursor = addEmptyLine(cursor, 42f);

            cursor = ensureRoom(cursor, document, invoiceTemplate, 2);
            // drawTableHeader(cursor);
            cursor = new PdfCursor(cursor.page(), cursor.content(), cursor.y - ROW_HEIGHT);

            for (InvoiceLine line : payload.items()) {
                cursor = ensureRoom(cursor, document, invoiceTemplate, 2);
                drawCell(cursor, 50, cursor.y, safe(line.description()), 9);
                drawCell(cursor, 385, cursor.y, String.valueOf(line.quantity()), 9);
                drawCell(cursor, 435, cursor.y, money(line.unitPrice()), 9);
                drawCell(cursor, 535, cursor.y, money(line.amount()), 9);
                cursor = new PdfCursor(cursor.page(), cursor.content(), cursor.y - ROW_HEIGHT);
            }

            cursor = ensureRoom(cursor, document, invoiceTemplate, 6);
            cursor = addEmptyLine(cursor, 178f);

            float summaryBaseY = cursor.y;
            // Left VAT summary box
            drawAmountIfPositive(cursor, payload.vatableSales(), 140f, summaryBaseY, 10, false);
            drawAmountIfPositive(cursor, payload.vat(), 140f, summaryBaseY - 24f, 10, false);
            drawAmountIfPositive(cursor, payload.zeroRatedSales(), 140f, summaryBaseY - 48f, 10, false);
            drawAmountIfPositive(cursor, payload.vatExSales(), 140f, summaryBaseY - 72f, 10, false);

            // Right totals box
            drawAmountIfPositive(cursor, payload.totalSales(), 530f, summaryBaseY, 11, false);
            drawAmountIfPositive(cursor, payload.lessVat(), 530f, summaryBaseY - 23.5f, 11, false);
            drawAmountIfPositive(cursor, payload.amountNetOfVat(), 530f, summaryBaseY - 47f, 11, false);
            drawAmountIfPositive(cursor, payload.lessWhTax(), 530f, summaryBaseY - 70.5f, 11, false);
            drawAmountIfPositive(cursor, payload.totalAfterWhTax(), 530f, summaryBaseY - 94.5f, 11, false);
            drawAmountIfPositive(cursor, payload.addVat(), 530f, summaryBaseY - 118f, 11, false);
            drawAmountIfPositive(cursor, payload.totalAmountDue(), 530f, summaryBaseY - 142f, 11, true);

            cursor = new PdfCursor(cursor.page(), cursor.content(), summaryBaseY - 168f);

            cursor = addEmptyLine(cursor, 45f);
            if (hasText(payload.approvedBy())) {
                drawCell(cursor, APPROVED_BY_X, cursor.y, payload.approvedBy().trim().toUpperCase(), 10);
            }
            drawCell(cursor, PREPARED_BY_X, cursor.y, safe(payload.preparedBy().toUpperCase()), 10);

            cursor.content().close();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate sales invoice PDF", ex);
        }
    }

    private void drawTableHeader(PdfCursor cursor) throws IOException {
        drawCell(cursor, PAGE_MARGIN, cursor.y, "Item", 10, true);
        drawCell(cursor, 115, cursor.y, "Description", 10, true);
        drawCell(cursor, 320, cursor.y, "Qty", 10, true);
        drawCell(cursor, 370, cursor.y, "Unit Price", 10, true);
        drawCell(cursor, 470, cursor.y, "Amount", 10, true);
    }

    private void drawCell(PdfCursor cursor, float x, float y, String text, int fontSize) throws IOException {
        drawCell(cursor, x, y, text, fontSize, false);
    }

    private void drawCell(PdfCursor cursor, float x, float y, String text, int fontSize, boolean bold) throws IOException {
        cursor.content().beginText();
        cursor.content().setFont(bold ? FONT_BOLD : FONT_REGULAR, fontSize);
        cursor.content().newLineAtOffset(x, y);
        cursor.content().showText(trimText(text, 44));
        cursor.content().endText();
    }

    // Overload 1: For regular text with newlines
    private PdfCursor drawCell(PdfCursor cursor, float x, float y, String text, int fontSize, int newLinesAfter) throws IOException {
        return drawCell(cursor, x, y, text, fontSize, newLinesAfter, DEFAULT_POINTS_TO_DROP);
    }

    // Overload 1b: For regular text with configurable line drop
    private PdfCursor drawCell(PdfCursor cursor, float x, float y, String text, int fontSize, int newLinesAfter, float pointsToDrop) throws IOException {
        drawCell(cursor, x, y, text, fontSize, false);
        return addEmptyLine(cursor, newLinesAfter * pointsToDrop);
    }

    // Overload 2: For bold text with newlines
    private PdfCursor drawCell(PdfCursor cursor, float x, float y, String text, int fontSize, boolean bold, int newLinesAfter) throws IOException {
        return drawCell(cursor, x, y, text, fontSize, bold, newLinesAfter, DEFAULT_POINTS_TO_DROP);
    }

    // Overload 2b: For bold text with configurable line drop
    private PdfCursor drawCell(PdfCursor cursor, float x, float y, String text, int fontSize, boolean bold, int newLinesAfter, float pointsToDrop) throws IOException {
        drawCell(cursor, x, y, text, fontSize, bold);
        return addEmptyLine(cursor, newLinesAfter * pointsToDrop);
    }

    private PdfCursor writeLine(PdfCursor cursor, String text, PDType1Font font, int fontSize, float x, float y) throws IOException {
        cursor.content().beginText();
        cursor.content().setFont(font, fontSize);
        cursor.content().newLineAtOffset(x, y);
        cursor.content().showText(safe(text));
        cursor.content().endText();
        return new PdfCursor(cursor.page(), cursor.content(), y - 14);
    }

    private PdfCursor ensureRoom(PdfCursor cursor, PDDocument document, PDImageXObject templateImage, int rowsNeeded) throws IOException {
        if (cursor.y() - (rowsNeeded * ROW_HEIGHT) > MIN_Y) {
            return cursor;
        }
        cursor.content().close();
        PdfCursor next = addNewPage(document, templateImage);
        drawTableHeader(next);
        return new PdfCursor(next.page(), next.content(), next.y() - ROW_HEIGHT);
    }

    private PdfCursor addNewPage(PDDocument document, PDImageXObject templateImage) {
        PDPage page = new PDPage(INVOICE_PAGE_SIZE);
        document.addPage(page);
        try {
            PDPageContentStream background = new PDPageContentStream(document, page);
            background.drawImage(templateImage, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            background.close();
            PDPageContentStream textStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
            return new PdfCursor(page, textStream, page.getMediaBox().getHeight() - PAGE_MARGIN);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to initialize PDF page", ex);
        }
    }

    private PDImageXObject loadInvoiceTemplate(PDDocument document) {
        try (InputStream stream = SalesInvoicePdfService.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (stream == null) {
                throw new RuntimeException("Missing invoice template resource: " + TEMPLATE_RESOURCE);
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new RuntimeException("Failed to decode invoice template image: " + TEMPLATE_RESOURCE);
            }
            return LosslessFactory.createFromImage(document, image);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load invoice template image", ex);
        }
    }

    private String trimText(String input, int maxChars) {
        String text = safe(input);
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private PdfCursor addEmptyLine(PdfCursor cursor, float pointsToDrop) {
    return new PdfCursor(cursor.page(), cursor.content(), cursor.y() - pointsToDrop);
    }

    private String formatInvoiceDate(LocalDate date) {
        LocalDate safeDate = date != null ? date : LocalDate.now();
        return safeDate.format(DATE_FORMATTER);
    }

    private String money(BigDecimal value) {
        BigDecimal safeValue = value != null ? value : BigDecimal.ZERO;
        NumberFormat moneyFormat = NumberFormat.getNumberInstance(Locale.US);
        moneyFormat.setMinimumFractionDigits(2);
        moneyFormat.setMaximumFractionDigits(2);
        moneyFormat.setGroupingUsed(true);
        return "Php " + moneyFormat.format(safeValue.setScale(2, RoundingMode.HALF_UP));
    }

    private void drawAmountIfPositive(PdfCursor cursor, BigDecimal value, float x, float y, int fontSize, boolean bold) throws IOException {
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            drawCell(cursor, x, y, money(value), fontSize, bold);
        }
    }

    private String safe(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record PdfCursor(PDPage page, PDPageContentStream content, float y) {
    }

    public record InvoiceLine(String label, String description, int quantity, BigDecimal unitPrice, BigDecimal amount) {
    }

    public record SalesInvoicePdfPayload(
            LocalDate invoiceDate,
            String registeredName,
            String tin,
            String businessAddress,
            String terms,
            String poReference,
            List<InvoiceLine> items,
            String preparedBy,
            String approvedBy,
            BigDecimal totalSales,
            BigDecimal lessVat,
            BigDecimal amountNetOfVat,
            BigDecimal lessWhTax,
            BigDecimal totalAfterWhTax,
            BigDecimal addVat,
            BigDecimal vatableSales,
            BigDecimal vat,
            BigDecimal zeroRatedSales,
            BigDecimal vatExSales,
            BigDecimal totalAmountDue
    ) {
    }
}