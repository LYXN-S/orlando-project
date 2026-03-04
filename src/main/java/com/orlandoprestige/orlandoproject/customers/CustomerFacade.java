package com.orlandoprestige.orlandoproject.customers;

import com.orlandoprestige.orlandoproject.customers.dto.CustomerCredentialsDto;
import com.orlandoprestige.orlandoproject.customers.dto.CustomerInfoDataDto;
import com.orlandoprestige.orlandoproject.customers.internal.domain.Customer;
import com.orlandoprestige.orlandoproject.customers.internal.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Public facade for the Customers module.
 * Other modules must use this class to interact with customer data.
 */
@Service
@RequiredArgsConstructor
public class CustomerFacade {

    private final CustomerService customerService;

    public Optional<CustomerCredentialsDto> findCredentialsByEmail(String email) {
        return customerService.findByEmail(email)
                .map(c -> new CustomerCredentialsDto(c.getId(), c.getEmail(), c.getPasswordHash()));
    }

    public Optional<CustomerInfoDataDto> findCustomerById(Long id) {
        return customerService.findById(id)
                .map(c -> new CustomerInfoDataDto(c.getId(), c.getFirstName() + " " + c.getLastName(), c.getEmail()));
    }

    public List<CustomerInfoDataDto> findAllCustomers() {
        return customerService.findAll().stream()
                .map(c -> new CustomerInfoDataDto(c.getId(), c.getFirstName() + " " + c.getLastName(), c.getEmail()))
                .toList();
    }

    public boolean existsByEmail(String email) {
        return customerService.existsByEmail(email);
    }

    public void registerCustomer(String firstName, String lastName, String email, String passwordHash) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPasswordHash(passwordHash);
        customerService.save(customer);
    }

    public void deleteCustomer(Long id) {
        customerService.delete(id);
    }
}

