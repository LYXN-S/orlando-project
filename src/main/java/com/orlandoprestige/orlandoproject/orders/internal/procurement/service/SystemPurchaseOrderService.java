package com.orlandoprestige.orlandoproject.orders.internal.procurement.service;

import com.orlandoprestige.orlandoproject.orders.internal.procurement.domain.*;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.dto.*;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.repository.*;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    private final PoDocumentExtractionService extractionService;

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
        applyHeader(po, dto.resolvedCustomerName(), dto.poNumber(), dto.poDate(), dto.deliveryDate(), dto.currency(), dto.subtotal(), dto.tax(), dto.total(), dto.notes());
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

        applyHeader(po, dto.resolvedCustomerName(), dto.poNumber(), dto.poDate(), dto.deliveryDate(), dto.currency(), dto.subtotal(), dto.tax(), dto.total(), dto.notes());
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
        return toDto(poRepository.save(po));
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

        applyHeader(po, parsed.customerName(), parsed.poNumber(), parsed.poDate(), parsed.deliveryDate(), parsed.currency(), parsed.subtotal(), parsed.tax(), parsed.total(), parsed.warnings());
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
                             java.time.LocalDate deliveryDate, String currency, BigDecimal subtotal,
                             BigDecimal tax, BigDecimal total, String notes) {
        if (customerName != null) po.setCustomerName(customerName);
        if (poNumber != null) po.setPoNumber(poNumber);
        if (poDate != null) po.setPoDate(poDate);
        if (deliveryDate != null) po.setDeliveryDate(deliveryDate);
        if (currency != null) po.setCurrency(currency);
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
                latestExtraction
        );
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
