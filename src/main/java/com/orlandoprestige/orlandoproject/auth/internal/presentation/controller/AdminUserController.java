package com.orlandoprestige.orlandoproject.auth.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.auth.internal.domain.Permission;
import com.orlandoprestige.orlandoproject.auth.internal.domain.Staff;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.CreateStaffRequestDto;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.StaffResponseDto;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.UpdatePermissionsRequestDto;
import com.orlandoprestige.orlandoproject.auth.internal.repository.StaffRepository;
import com.orlandoprestige.orlandoproject.customers.CustomerFacade;
import com.orlandoprestige.orlandoproject.customers.dto.CustomerInfoDataDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Super Admin user management endpoints")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final StaffRepository staffRepository;
    private final CustomerFacade customerFacade;
    private final PasswordEncoder passwordEncoder;

    // ======================== STAFF ENDPOINTS ========================

    @GetMapping("/staff")
    @Operation(summary = "List all staff members (Super Admin only)")
    public ResponseEntity<List<StaffResponseDto>> getAllStaff() {
        List<StaffResponseDto> staff = staffRepository.findAll().stream()
                .map(this::toStaffDto)
                .toList();
        return ResponseEntity.ok(staff);
    }

    @GetMapping("/staff/{id}")
    @Operation(summary = "Get staff member details (Super Admin only)")
    public ResponseEntity<StaffResponseDto> getStaffById(@PathVariable Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));
        return ResponseEntity.ok(toStaffDto(staff));
    }

    @PostMapping("/staff")
    @Operation(summary = "Create a new staff account (Super Admin only)")
    public ResponseEntity<StaffResponseDto> createStaff(@Valid @RequestBody CreateStaffRequestDto request) {
        if (staffRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }

        Staff staff = new Staff();
        staff.setFirstName(request.firstName());
        staff.setLastName(request.lastName());
        staff.setEmail(request.email());
        staff.setPasswordHash(passwordEncoder.encode(request.password()));
        staff.setSuperAdmin(false);

        if (request.permissions() != null) {
            Set<Permission> perms = request.permissions().stream()
                    .map(Permission::valueOf)
                    .collect(Collectors.toSet());
            staff.setPermissions(perms);
        }

        Staff saved = staffRepository.save(staff);
        return ResponseEntity.status(HttpStatus.CREATED).body(toStaffDto(saved));
    }

    @PatchMapping("/staff/{id}/permissions")
    @Operation(summary = "Update staff member permissions (Super Admin only)")
    public ResponseEntity<StaffResponseDto> updatePermissions(
            @PathVariable Long id,
            @RequestBody UpdatePermissionsRequestDto request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));

        if (staff.isSuperAdmin()) {
            throw new IllegalArgumentException("Cannot modify super admin permissions");
        }

        Set<Permission> newPerms = new HashSet<>();
        if (request.permissions() != null) {
            for (String perm : request.permissions()) {
                newPerms.add(Permission.valueOf(perm));
            }
        }
        staff.setPermissions(newPerms);
        Staff saved = staffRepository.save(staff);
        return ResponseEntity.ok(toStaffDto(saved));
    }

    @DeleteMapping("/staff/{id}")
    @Operation(summary = "Soft-delete a staff member (Super Admin only)")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));
        if (staff.isSuperAdmin()) {
            throw new IllegalArgumentException("Cannot delete super admin");
        }
        staffRepository.delete(staff);
        return ResponseEntity.noContent().build();
    }

    // ======================== CUSTOMER ENDPOINTS ========================

    @GetMapping("/customers")
    @Operation(summary = "List all customers (Super Admin only)")
    public ResponseEntity<List<CustomerInfoDataDto>> getAllCustomers() {
        return ResponseEntity.ok(customerFacade.findAllCustomers());
    }

    @GetMapping("/customers/{id}")
    @Operation(summary = "Get customer details (Super Admin only)")
    public ResponseEntity<CustomerInfoDataDto> getCustomerById(@PathVariable Long id) {
        CustomerInfoDataDto customer = customerFacade.findCustomerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
        return ResponseEntity.ok(customer);
    }

    @DeleteMapping("/customers/{id}")
    @Operation(summary = "Soft-delete a customer (Super Admin only)")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerFacade.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // ======================== MAPPING HELPERS ========================

    private StaffResponseDto toStaffDto(Staff staff) {
        Set<String> permNames = staff.getPermissions().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        return new StaffResponseDto(
                staff.getId(), staff.getFirstName(), staff.getLastName(),
                staff.getEmail(), staff.isSuperAdmin(), permNames);
    }
}
