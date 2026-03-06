package com.orlandoprestige.orlandoproject.customers.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.customers.internal.domain.BillingProfile;
import com.orlandoprestige.orlandoproject.customers.internal.domain.BillingType;
import com.orlandoprestige.orlandoproject.customers.internal.presentation.dto.BillingProfileDto;
import com.orlandoprestige.orlandoproject.customers.internal.presentation.dto.CreateBillingProfileDto;
import com.orlandoprestige.orlandoproject.customers.internal.service.BillingProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing-profiles")
@RequiredArgsConstructor
@Tag(name = "Billing Profiles", description = "Customer billing profile endpoints")
public class BillingProfileController {

    private final BillingProfileService billingProfileService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get current customer's billing profiles")
    public ResponseEntity<List<BillingProfileDto>> getMyProfiles(
            @AuthenticationPrincipal AuthenticatedUser user) {
        List<BillingProfileDto> profiles = billingProfileService.getByCustomerId(user.userId())
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(profiles);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Create a new billing profile")
    public ResponseEntity<BillingProfileDto> createProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateBillingProfileDto dto) {
        BillingProfile profile = new BillingProfile();
        profile.setCustomerId(user.userId());
        profile.setBillingType(BillingType.valueOf(dto.billingType()));
        profile.setName(dto.name());
        profile.setTin(dto.tin());
        profile.setAddress(dto.address());
        profile.setPaymentTerms(dto.paymentTerms());

        BillingProfile saved = billingProfileService.create(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Update a billing profile")
    public ResponseEntity<BillingProfileDto> updateProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateBillingProfileDto dto) {
        BillingProfile updated = new BillingProfile();
        updated.setBillingType(BillingType.valueOf(dto.billingType()));
        updated.setName(dto.name());
        updated.setTin(dto.tin());
        updated.setAddress(dto.address());
        updated.setPaymentTerms(dto.paymentTerms());

        BillingProfile saved = billingProfileService.update(id, user.userId(), updated);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Delete a billing profile")
    public ResponseEntity<Void> deleteProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        billingProfileService.delete(id, user.userId());
        return ResponseEntity.noContent().build();
    }

    private BillingProfileDto toDto(BillingProfile profile) {
        return new BillingProfileDto(
                profile.getId(),
                profile.getCustomerId(),
                profile.getBillingType(),
                profile.getName(),
                profile.getTin(),
                profile.getAddress(),
                profile.getPaymentTerms(),
                profile.isDefault(),
                profile.getCreatedAt()
        );
    }
}
