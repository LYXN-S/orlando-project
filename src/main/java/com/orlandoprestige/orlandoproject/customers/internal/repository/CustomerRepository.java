package com.orlandoprestige.orlandoproject.customers.internal.repository;

import com.orlandoprestige.orlandoproject.customers.internal.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
}

