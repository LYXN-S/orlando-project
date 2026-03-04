package com.orlandoprestige.orlandoproject.customers.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.customers.internal.domain.Customer;
import com.orlandoprestige.orlandoproject.customers.internal.presentation.dto.CustomerDto;
import com.orlandoprestige.orlandoproject.customers.internal.presentation.dto.UpdateCustomerDto;
import com.orlandoprestige.orlandoproject.customers.internal.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management endpoints")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_USERS')")
    @Operation(summary = "Get all customers (Staff with permission)")
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        List<CustomerDto> customers = customerService.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_USERS') or #id == authentication.principal.userId()")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id) {
        return customerService.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.userId()")
    @Operation(summary = "Update customer profile")
    public ResponseEntity<CustomerDto> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerDto dto) {
        return customerService.findById(id)
                .map(customer -> {
                    customer.setFirstName(dto.firstName());
                    customer.setLastName(dto.lastName());
                    customer.setEmail(dto.email());
                    return ResponseEntity.ok(toDto(customerService.save(customer)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_USERS')")
    @Operation(summary = "Soft-delete a customer (Staff with permission)")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private CustomerDto toDto(Customer customer) {
        return new CustomerDto(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail()
        );
    }
}

