package com.orlandoprestige.orlandoproject.orders.internal.procurement.service;

import com.orlandoprestige.orlandoproject.auth.internal.domain.Staff;
import com.orlandoprestige.orlandoproject.auth.internal.repository.StaffRepository;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.domain.*;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.dto.*;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.repository.*;
import com.orlandoprestige.orlandoproject.orders.internal.service.SalesInvoicePdfService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SystemPurchaseOrderService {

    private static final ConcurrentHashMap<String, SystemPoExtractionAsyncStatusDto> ASYNC_RESULTS = new ConcurrentHashMap<>();

    private final SystemPurchaseOrderRepository poRepository;
    private final SystemPurchaseOrderItemRepository itemRepository;
    private final SystemPurchaseOrderAttachmentRepository attachmentRepository;
    private final SystemPurchaseOrderExtractionRunRepository extractionRunRepository;
    private final SystemSalesInvoiceRepository salesInvoiceRepository;
    private final SystemSalesInvoiceItemRepository salesInvoiceItemRepository;
    private final StaffRepository staffRepository;
    private final PoDocumentExtractionService extractionService;
    private final SalesInvoicePdfService salesInvoicePdfService;

    @Value("${app.upload.po-dir:uploads/po}")
    private String poUploadDir;

    @Transactional(readOnly = true)
    public List<SystemPoResponseDto> list(String status) {
        List<SystemPurchaseOrder> orders = status == null
                ? poRepository.findAllByOrderByCreatedAtDesc()
                : poRepository.findAllByStatusOrderByCreatedAtDesc(SystemPoStatus.valueOf(status));

        return orders.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SystemPoResponseDto getById(Long id) {
        SystemPurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("System PO not found: " + id));
        return toDto(po);
    }

    @Transactional
    public SystemPoResponseDto createManualDraft(Long userId, SystemPoCreateRequestDto dto) {
        SystemPurchaseOrder po = new SystemPurchaseOrder();
        po.setSourceType(SystemPoSourceType.MANUAL);
        po.setStatus(SystemPoStatus.DRAFT);
        po.setCreatedBy(userId);
        applyHeader(po, dto.resolvedCustomerName(), dto.poNumber(), dto.poDate(), dto.deliveryDate(), dto.currency(), dto.tin(), dto.businessAddress(), dto.deliveryAddress(), dto.sameAsBusinessAddress(), dto.subtotal(), dto.tax(), dto.total(), dto.notes());
        SystemPurchaseOrder saved = poRepository.save(po);
        replaceItems(saved, dto.safeItems());
        return toDto(saved);
    }

    @Transactional
    public SystemPoResponseDto updateDraft(Long id, SystemPoUpdateRequestDto dto) {
        SystemPurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("System PO not found: " + id));
        if (po.getStatus() != SystemPoStatus.DRAFT) {
            throw new IllegalStateException("Only draft POs can be updated.");
        }

        applyHeader(po, dto.resolvedCustomerName(), dto.poNumber(), dto.poDate(), dto.deliveryDate(), dto.currency(), dto.tin(), dto.businessAddress(), dto.deliveryAddress(), dto.sameAsBusinessAddress(), dto.subtotal(), dto.tax(), dto.total(), dto.notes());
        SystemPurchaseOrder saved = poRepository.save(po);
        replaceItems(saved, dto.safeItems());
        return toDto(saved);
    }

    @Transactional
    public SystemPoResponseDto confirm(Long id, Long userId, String note) {
        SystemPurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("System PO not found: " + id));
        if (po.getStatus() != SystemPoStatus.DRAFT) {
            throw new IllegalStateException("Only draft POs can be confirmed.");
        }

        po.setStatus(SystemPoStatus.CONFIRMED);
        po.setConfirmedBy(userId);
        po.setConfirmedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            po.setNotes(note);
        }
        SystemPurchaseOrder saved = poRepository.save(po);
        ensureSalesInvoice(saved, userId);
        return toDto(saved);
    }

    @Transactional
    public SystemPoResponseDto updateSalesInvoice(Long poId, Long userId, SystemSalesInvoiceUpdateRequestDto dto) {
        SystemPurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new EntityNotFoundException("System PO not found: " + poId));

        SystemSalesInvoice invoice = salesInvoiceRepository.findByPurchaseOrderId(poId)
                .orElseGet(() -> ensureSalesInvoice(po, userId));

        invoice.setInvoiceDate(dto.invoiceDate());
        invoice.setRegisteredName(dto.registeredName());
        invoice.setTin(dto.tin());
        invoice.setBusinessAddress(dto.businessAddress());
        invoice.setTerms(dto.terms());
        invoice.setPoNumber(dto.poNumber());
        invoice.setTotal(dto.total());
        invoice.setTotalSales(dto.totalSales());
        invoice.setLessVat(dto.lessVat());
        invoice.setAmountNetOfVat(dto.amountNetOfVat());
        invoice.setLessWhTax(dto.lessWhTax());
        invoice.setTotalAfterWhTax(dto.totalAfterWhTax());
        invoice.setAddVat(dto.addVat());
        invoice.setTotalAmountDue(dto.totalAmountDue());
        invoice.setVatableSales(dto.vatableSales());
        invoice.setVat(dto.vat());
        invoice.setZeroRatedSales(dto.zeroRatedSales());
        invoice.setVatExSales(dto.vatExSales());
        invoice.setApprovedBy(dto.approvedBy());

        applyInvoiceTotalsMapping(invoice, dto.total(), dto.totalSales(), dto.vat(), dto.lessVat());

        salesInvoiceRepository.save(invoice);

        salesInvoiceItemRepository.deleteAllBySalesInvoiceId(invoice.getId());
        List<SystemSalesInvoiceItem> invoiceItems = new ArrayList<>();
        int line = 1;
        for (SystemSalesInvoiceItemDto item : dto.safeItems()) {
            SystemSalesInvoiceItem entity = new SystemSalesInvoiceItem();
            entity.setSalesInvoice(invoice);
            entity.setLineNumber(item.lineNumber() != null ? item.lineNumber() : line++);
            entity.setItemLabel(item.itemLabel());
            entity.setDescription(item.description());
            entity.setQuantity(item.quantity());
            entity.setUnitPrice(item.unitPrice());
            entity.setAmount(item.amount());
            invoiceItems.add(entity);
        }
        salesInvoiceItemRepository.saveAll(invoiceItems);

        return toDto(po);
    }

        @Transactional(readOnly = true)
        public byte[] generateSalesInvoicePdf(Long poId, Long userId) {
        SystemPurchaseOrder po = poRepository.findById(poId)
            .orElseThrow(() -> new EntityNotFoundException("System PO not found: " + poId));
        if (po.getStatus() != SystemPoStatus.CONFIRMED) {
            throw new IllegalStateException("Sales invoice is only available for confirmed POs.");
        }

        SystemSalesInvoice invoice = salesInvoiceRepository.findByPurchaseOrderId(poId)
            .orElseGet(() -> ensureSalesInvoice(po, userId));

        // Prepared-by should reflect the account generating the invoice PDF.
        invoice.setPreparedBy(resolveStaffDisplayName(userId));
        invoice.setPreparedByUserId(userId);
        salesInvoiceRepository.save(invoice);

        List<SalesInvoicePdfService.InvoiceLine> lines = salesInvoiceItemRepository
            .findAllBySalesInvoiceIdOrderByLineNumberAsc(invoice.getId())
            .stream()
            .map(item -> new SalesInvoicePdfService.InvoiceLine(
                item.getItemLabel(),
                item.getDescription(),
                item.getQuantity() != null ? item.getQuantity() : 0,
                nvl(item.getUnitPrice()),
                nvl(item.getAmount())
            ))
            .toList();

        SalesInvoicePdfService.SalesInvoicePdfPayload payload = new SalesInvoicePdfService.SalesInvoicePdfPayload(
            invoice.getInvoiceDate(),
            invoice.getRegisteredName(),
            invoice.getTin(),
            invoice.getBusinessAddress(),
            invoice.getTerms(),
            invoice.getPoNumber(),
            lines,
            invoice.getPreparedBy(),
            invoice.getApprovedBy(),
            nvl(invoice.getTotalSales()),
            nvl(invoice.getLessVat()),
            nvl(invoice.getAmountNetOfVat()),
            nvl(invoice.getLessWhTax()),
            nvl(invoice.getTotalAfterWhTax()),
            nvl(invoice.getAddVat()),
            nvl(invoice.getVatableSales()),
            nvl(invoice.getVat()),
            nvl(invoice.getZeroRatedSales()),
            nvl(invoice.getVatExSales()),
            nvl(invoice.getTotalAmountDue())
        );
        return salesInvoicePdfService.render(payload);
        }

    @Transactional
    public void delete(Long id) {
        SystemPurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("System PO not found: " + id));

        if (po.getStatus() != SystemPoStatus.DRAFT) {
            throw new IllegalStateException("Only draft POs can be deleted.");
        }

        poRepository.delete(po);
    }

    @Transactional
    public SystemPoResponseDto uploadAndExtract(Long userId, Long existingPoId, MultipartFile file) throws IOException {
        validateUpload(file);
        return processUploadAndExtract(
                userId,
                existingPoId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes(),
                file.getSize()
        );
    }

    public String uploadAndExtractAsync(Long userId, Long existingPoId, MultipartFile file) throws IOException {
        validateUpload(file);

        String requestId = UUID.randomUUID().toString();
        byte[] bytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long size = file.getSize();

        ASYNC_RESULTS.put(requestId, SystemPoExtractionAsyncStatusDto.pending(requestId));

        CompletableFuture.runAsync(() -> {
            try {
                SystemPoResponseDto result = processUploadAndExtract(
                        userId,
                        existingPoId,
                        originalFilename,
                        contentType,
                        bytes,
                        size
                );
                ASYNC_RESULTS.put(requestId, SystemPoExtractionAsyncStatusDto.success(requestId, result));
            } catch (Exception ex) {
                ASYNC_RESULTS.put(requestId, SystemPoExtractionAsyncStatusDto.failed(requestId, ex.getMessage()));
            }
        });

        return requestId;
    }

    @Transactional(readOnly = true)
    public SystemPoExtractionAsyncStatusDto getAsyncExtractionStatus(String requestId) {
        return ASYNC_RESULTS.getOrDefault(requestId, SystemPoExtractionAsyncStatusDto.failed(requestId, "Request ID not found"));
    }

    @Transactional
    protected SystemPoResponseDto processUploadAndExtract(Long userId,
                                                          Long existingPoId,
                                                          String originalFilename,
                                                          String contentType,
                                                          byte[] fileBytes,
                                                          long fileSize) throws IOException {

        SystemPurchaseOrder po;
        if (existingPoId != null) {
            po = poRepository.findById(existingPoId)
                    .orElseThrow(() -> new EntityNotFoundException("System PO not found: " + existingPoId));
            if (po.getStatus() != SystemPoStatus.DRAFT) {
                throw new IllegalStateException("Attachments can only be uploaded to draft POs.");
            }
        } else {
            po = new SystemPurchaseOrder();
            po.setSourceType(SystemPoSourceType.UPLOAD_AI);
            po.setStatus(SystemPoStatus.DRAFT);
            po.setCreatedBy(userId);
            po = poRepository.save(po);
        }

        Path dir = Paths.get(poUploadDir);
        Files.createDirectories(dir);
        String extension = getExtension(originalFilename);
        String storedName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        Path storedPath = dir.resolve(storedName);
        Files.write(storedPath, fileBytes);

        SystemPurchaseOrderAttachment attachment = new SystemPurchaseOrderAttachment();
        attachment.setPurchaseOrderId(po.getId());
        attachment.setFilePath(storedPath.toString());
        attachment.setFileName(originalFilename != null ? originalFilename : storedName);
        attachment.setMimeType(contentType);
        attachment.setFileSize(fileSize);
        attachment.setUploadedBy(userId);
        attachment = attachmentRepository.save(attachment);

        ParsedPurchaseOrderDto parsed = extractionService.extractFromDocument(
                attachment.getFileName(),
                attachment.getMimeType(),
            fileBytes
        );

        Boolean sameAsBusiness = parsed.businessAddress() != null
            && parsed.deliveryAddress() != null
            && parsed.businessAddress().trim().equalsIgnoreCase(parsed.deliveryAddress().trim());

        applyHeader(
            po,
            parsed.customerName(),
            parsed.poNumber(),
            parsed.poDate(),
            parsed.deliveryDate(),
            parsed.currency(),
            null,
            parsed.businessAddress(),
            parsed.deliveryAddress(),
            sameAsBusiness,
            parsed.subtotal(),
            parsed.tax(),
            parsed.total(),
            parsed.warnings()
        );
        poRepository.save(po);
        replaceItems(po, parsed.safeItems());

        SystemPurchaseOrderExtractionRun run = new SystemPurchaseOrderExtractionRun();
        run.setPurchaseOrderId(po.getId());
        run.setAttachmentId(attachment.getId());
        run.setStatus(parsed.confidence() >= 0.7 ? SystemPoExtractionStatus.SUCCESS : SystemPoExtractionStatus.PARTIAL);
        run.setModelName("spring-ai-gemini-configured-or-fallback");
        run.setConfidence(parsed.confidence());
        run.setWarnings(parsed.warnings());
        run.setExtractedJson(parsed.rawStructuredOutput());
        extractionRunRepository.save(run);

        return toDto(po);
    }

    @Transactional(readOnly = true)
    public Resource loadAttachment(Long attachmentId) throws IOException {
        SystemPurchaseOrderAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + attachmentId));

        Resource resource = new UrlResource(Paths.get(attachment.getFilePath()).toUri());
        if (!resource.exists()) {
            throw new EntityNotFoundException("Attachment file missing: " + attachment.getFileName());
        }
        return resource;
    }

    @Transactional(readOnly = true)
    public SystemPurchaseOrderAttachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + attachmentId));
    }

    private void validateUpload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded PO file is empty.");
        }

        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        boolean supported = contentType.equalsIgnoreCase(MediaType.APPLICATION_PDF_VALUE)
                || contentType.equalsIgnoreCase(MediaType.IMAGE_JPEG_VALUE)
                || contentType.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)
                || contentType.equalsIgnoreCase("image/webp");

        if (!supported) {
            throw new IllegalArgumentException("Only PDF, JPG, PNG, or WEBP files are allowed.");
        }

        long maxSize = 10L * 1024L * 1024L;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("PO file size cannot exceed 10MB.");
        }
    }

    private void applyHeader(SystemPurchaseOrder po, String customerName, String poNumber, java.time.LocalDate poDate,
                             java.time.LocalDate deliveryDate, String currency, String tin,
                             String businessAddress, String deliveryAddress, Boolean sameAsBusinessAddress,
                             BigDecimal subtotal, BigDecimal tax, BigDecimal total, String notes) {
        if (customerName != null) po.setCustomerName(customerName);
        if (poNumber != null) po.setPoNumber(poNumber);
        if (poDate != null) po.setPoDate(poDate);
        if (deliveryDate != null) po.setDeliveryDate(deliveryDate);
        if (currency != null) po.setCurrency(currency);
        if (tin != null) po.setTin(tin);
        if (businessAddress != null) po.setBusinessAddress(businessAddress);
        if (sameAsBusinessAddress != null) po.setSameAsBusinessAddress(sameAsBusinessAddress);
        if (deliveryAddress != null) {
            po.setDeliveryAddress(deliveryAddress);
        }
        if (Boolean.TRUE.equals(sameAsBusinessAddress) && businessAddress != null) {
            po.setDeliveryAddress(businessAddress);
        }
        if (subtotal != null) po.setSubtotal(subtotal);
        if (tax != null) po.setTax(tax);
        if (total != null) po.setTotal(total);
        if (notes != null) po.setNotes(notes);
    }

    private void replaceItems(SystemPurchaseOrder po, List<SystemPoItemDto> items) {
        itemRepository.deleteAllByPurchaseOrderId(po.getId());
        List<SystemPurchaseOrderItem> toSave = new ArrayList<>();
        int line = 1;
        for (SystemPoItemDto item : items) {
            SystemPurchaseOrderItem entity = new SystemPurchaseOrderItem();
            entity.setPurchaseOrder(po);
            entity.setLineNumber(item.lineNumber() != null ? item.lineNumber() : line++);
            entity.setDescription(item.description());
            entity.setSku(item.sku());
            entity.setQuantity(item.quantity());
            entity.setUnitPrice(item.unitPrice());
            entity.setLineTotal(item.lineTotal());
            toSave.add(entity);
        }
        itemRepository.saveAll(toSave);
    }

    private SystemPoResponseDto toDto(SystemPurchaseOrder po) {
        List<SystemPoItemDto> items = itemRepository.findAllByPurchaseOrderId(po.getId()).stream()
                .map(item -> new SystemPoItemDto(
                        item.getLineNumber(),
                        item.getDescription(),
                        item.getSku(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        List<SystemPoAttachmentDto> attachments = attachmentRepository.findAllByPurchaseOrderIdOrderByUploadedAtDesc(po.getId()).stream()
                .map(a -> new SystemPoAttachmentDto(
                        a.getId(),
                        a.getFileName(),
                        a.getMimeType(),
                        a.getFileSize(),
                        a.getUploadedAt(),
                        "/api/v1/procurement/pos/attachments/" + a.getId()
                ))
                .toList();

        SystemPoExtractionRunDto latestExtraction = extractionRunRepository.findAllByPurchaseOrderIdOrderByCreatedAtDesc(po.getId()).stream()
                .findFirst()
                .map(r -> new SystemPoExtractionRunDto(
                        r.getId(),
                        r.getStatus().name(),
                        r.getModelName(),
                        r.getConfidence(),
                        r.getWarnings(),
                        r.getCreatedAt()
                ))
                .orElse(null);

            SystemSalesInvoiceDto salesInvoice = salesInvoiceRepository.findByPurchaseOrderId(po.getId())
                .map(this::toSalesInvoiceDto)
                .orElse(null);

        return new SystemPoResponseDto(
                po.getId(),
                po.getSourceType().name(),
                po.getStatus().name(),
                po.getCustomerName(),
                po.getCustomerName(),
                po.getPoNumber(),
                po.getPoDate(),
                po.getDeliveryDate(),
                po.getCurrency(),
                po.getTin(),
                po.getBusinessAddress(),
                po.getDeliveryAddress(),
                po.getSameAsBusinessAddress(),
                po.getSubtotal(),
                po.getTax(),
                po.getTotal(),
                po.getNotes(),
                po.getCreatedBy(),
                po.getConfirmedBy(),
                po.getConfirmedAt(),
                po.getCreatedAt(),
                items,
                attachments,
                latestExtraction,
                salesInvoice
        );
    }

            private SystemSalesInvoice ensureSalesInvoice(SystemPurchaseOrder po, Long userId) {
            return salesInvoiceRepository.findByPurchaseOrderId(po.getId())
                .orElseGet(() -> {
                    SystemSalesInvoice invoice = new SystemSalesInvoice();
                    invoice.setPurchaseOrderId(po.getId());
                    invoice.setInvoiceDate(LocalDate.now());
                    invoice.setRegisteredName(po.getCustomerName());
                    invoice.setTin(po.getTin() != null ? po.getTin() : "");
                    invoice.setBusinessAddress(po.getBusinessAddress() != null ? po.getBusinessAddress() : "");
                    invoice.setTerms("30 Days");
                    invoice.setPoNumber(po.getPoNumber());
                    invoice.setApprovedBy("");
                    invoice.setPreparedBy(resolveStaffDisplayName(userId));
                    invoice.setPreparedByUserId(userId);

                    List<SystemPoItemDto> poItems = itemRepository.findAllByPurchaseOrderId(po.getId()).stream()
                        .map(item -> new SystemPoItemDto(
                            item.getLineNumber(),
                            item.getDescription(),
                            item.getSku(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getLineTotal()
                        ))
                        .toList();

                    BigDecimal totalSales = BigDecimal.ZERO;
                    List<SystemSalesInvoiceItem> invoiceItems = new ArrayList<>();
                    int line = 1;
                    for (SystemPoItemDto poItem : poItems) {
                    BigDecimal unitPrice = nvl(poItem.unitPrice());
                    BigDecimal amount = poItem.lineTotal() != null
                        ? poItem.lineTotal()
                        : unitPrice.multiply(BigDecimal.valueOf(poItem.quantity()));
                    totalSales = totalSales.add(amount);

                    SystemSalesInvoiceItem invoiceItem = new SystemSalesInvoiceItem();
                    invoiceItem.setSalesInvoice(invoice);
                    invoiceItem.setLineNumber(poItem.lineNumber() != null ? poItem.lineNumber() : line++);
                    invoiceItem.setItemLabel("Item " + invoiceItem.getLineNumber());
                    invoiceItem.setDescription(poItem.description());
                    invoiceItem.setQuantity(poItem.quantity());
                    invoiceItem.setUnitPrice(unitPrice);
                    invoiceItem.setAmount(amount);
                    invoiceItems.add(invoiceItem);
                    }

                    BigDecimal lessVat = totalSales.multiply(BigDecimal.valueOf(12))
                        .divide(BigDecimal.valueOf(112), 2, RoundingMode.HALF_UP);
                    BigDecimal amountNetOfVat = totalSales.subtract(lessVat);
                    BigDecimal lessWhTax = BigDecimal.ZERO;
                    BigDecimal totalAfterWhTax = amountNetOfVat.subtract(lessWhTax);
                    BigDecimal addVat = lessVat;
                    BigDecimal totalAmountDue = totalAfterWhTax.add(addVat);

                    invoice.setTotal(totalSales);
                    invoice.setTotalSales(totalSales);
                    invoice.setLessVat(lessVat);
                    invoice.setAmountNetOfVat(amountNetOfVat);
                    invoice.setLessWhTax(lessWhTax);
                    invoice.setTotalAfterWhTax(totalAfterWhTax);
                    invoice.setAddVat(addVat);
                    invoice.setTotalAmountDue(totalAmountDue);
                    invoice.setVatableSales(totalSales);
                    invoice.setVat(lessVat);
                    invoice.setZeroRatedSales(BigDecimal.ZERO);
                    invoice.setVatExSales(BigDecimal.ZERO);

                    applyInvoiceTotalsMapping(invoice, totalSales, totalSales, lessVat, lessVat);

                    SystemSalesInvoice savedInvoice = salesInvoiceRepository.save(invoice);
                    salesInvoiceItemRepository.saveAll(invoiceItems);
                    return savedInvoice;
                });
            }

            private SystemSalesInvoiceDto toSalesInvoiceDto(SystemSalesInvoice invoice) {
            List<SystemSalesInvoiceItemDto> items = salesInvoiceItemRepository.findAllBySalesInvoiceIdOrderByLineNumberAsc(invoice.getId()).stream()
                .map(item -> new SystemSalesInvoiceItemDto(
                    item.getLineNumber(),
                    item.getItemLabel(),
                    item.getDescription(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getAmount()
                ))
                .toList();

            return new SystemSalesInvoiceDto(
                invoice.getId(),
                invoice.getPurchaseOrderId(),
                invoice.getInvoiceDate(),
                invoice.getRegisteredName(),
                invoice.getTin(),
                invoice.getBusinessAddress(),
                invoice.getTerms(),
                invoice.getPoNumber(),
                invoice.getTotal(),
                invoice.getTotalSales(),
                invoice.getLessVat(),
                invoice.getAmountNetOfVat(),
                invoice.getLessWhTax(),
                invoice.getTotalAfterWhTax(),
                invoice.getAddVat(),
                invoice.getTotalAmountDue(),
                invoice.getVatableSales(),
                invoice.getVat(),
                invoice.getZeroRatedSales(),
                invoice.getVatExSales(),
                invoice.getApprovedBy(),
                invoice.getPreparedBy(),
                invoice.getPreparedByUserId(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt(),
                items
            );
            }

            private String resolveStaffDisplayName(Long userId) {
            return staffRepository.findById(userId)
                .map(staff -> (staff.getFirstName() + " " + staff.getLastName()).trim())
                .filter(name -> !name.isBlank())
                .orElse("Staff #" + userId);
            }

            private BigDecimal nvl(BigDecimal value) {
            return value != null ? value : BigDecimal.ZERO;
            }

    private void applyInvoiceTotalsMapping(SystemSalesInvoice invoice,
                                           BigDecimal total,
                                           BigDecimal totalSales,
                                           BigDecimal vat,
                                           BigDecimal lessVat) {
        BigDecimal grossTotal = nvl(totalSales);
        if (grossTotal.compareTo(BigDecimal.ZERO) == 0 && total != null) {
            grossTotal = total;
        }

        BigDecimal resolvedVat = nvl(vat);
        if (resolvedVat.compareTo(BigDecimal.ZERO) == 0 && lessVat != null) {
            resolvedVat = lessVat;
        }

        BigDecimal netOfVat = grossTotal.subtract(resolvedVat);

        // Requested mapping:
        // vatable sales = vat
        // total sales = total sales including vat
        // netOfVat = total sales - vat
        // totalAmount = total
        invoice.setTotal(grossTotal);
        invoice.setTotalSales(grossTotal);
        invoice.setVat(resolvedVat);
        invoice.setLessVat(resolvedVat);
        invoice.setAmountNetOfVat(netOfVat);
        invoice.setTotalAmountDue(grossTotal);
        invoice.setVatableSales(resolvedVat);
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
