package com.orlandoprestige.orlandoproject.orders.internal.procurement.presentation;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.domain.SystemPurchaseOrderAttachment;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.dto.*;
import com.orlandoprestige.orlandoproject.orders.internal.procurement.service.SystemPurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/procurement/pos")
@RequiredArgsConstructor
@Tag(name = "Procurement POs", description = "Manual and upload-assisted purchase order workflows")
public class SystemPurchaseOrderController {

    private final SystemPurchaseOrderService service;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "List procurement purchase orders")
    public ResponseEntity<List<SystemPoResponseDto>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.list(status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Get procurement purchase order")
    public ResponseEntity<SystemPoResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Create manual draft PO")
    public ResponseEntity<SystemPoResponseDto> createDraft(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SystemPoCreateRequestDto dto) {
        return ResponseEntity.ok(service.createManualDraft(user.userId(), dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Update draft PO")
    public ResponseEntity<SystemPoResponseDto> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody SystemPoUpdateRequestDto dto) {
        return ResponseEntity.ok(service.updateDraft(id, dto));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Confirm draft PO")
    public ResponseEntity<SystemPoResponseDto> confirm(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) SystemPoConfirmRequestDto dto) {
        String note = dto != null ? dto.note() : null;
        return ResponseEntity.ok(service.confirm(id, user.userId(), note));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Delete draft procurement PO (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload-extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Upload PO document and auto-populate draft form")
    public ResponseEntity<SystemPoResponseDto> uploadAndExtract(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "poId", required = false) String poId) throws IOException {
        Long existingPoId = (poId != null && !poId.isBlank()) ? Long.parseLong(poId) : null;
        return ResponseEntity.ok(service.uploadAndExtract(user.userId(), existingPoId, file));
    }

    @PostMapping(value = "/upload-extract-async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Start async upload and extraction, then poll by requestId")
    public ResponseEntity<SystemPoExtractionAsyncStatusDto> uploadAndExtractAsync(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "poId", required = false) String poId) throws IOException {
        Long existingPoId = (poId != null && !poId.isBlank()) ? Long.parseLong(poId) : null;
        String requestId = service.uploadAndExtractAsync(user.userId(), existingPoId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SystemPoExtractionAsyncStatusDto.pending(requestId));
    }

    @GetMapping("/upload-extract-async/{requestId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Poll async extraction status/result by requestId")
    public ResponseEntity<SystemPoExtractionAsyncStatusDto> getAsyncExtractionResult(@PathVariable String requestId) {
        SystemPoExtractionAsyncStatusDto status = service.getAsyncExtractionStatus(requestId);
        if ("PENDING".equalsIgnoreCase(status.status())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(status);
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Download/reference uploaded PO source document")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) throws IOException {
        Resource resource = service.loadAttachment(attachmentId);
        SystemPurchaseOrderAttachment attachment = service.getAttachment(attachmentId);
        String contentType = attachment.getMimeType() != null ? attachment.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }
}
