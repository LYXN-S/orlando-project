package com.orlandoprestige.orlandoproject.customers.internal.repository;

import com.orlandoprestige.orlandoproject.customers.internal.domain.BillingProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingProfileRepository extends JpaRepository<BillingProfile, Long> {
    List<BillingProfile> findAllByCustomerId(Long customerId);
    Optional<BillingProfile> findByIdAndCustomerId(Long id, Long customerId);
    boolean existsByCustomerId(Long customerId);
}
