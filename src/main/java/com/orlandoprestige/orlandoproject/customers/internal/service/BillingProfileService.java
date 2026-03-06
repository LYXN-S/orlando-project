package com.orlandoprestige.orlandoproject.customers.internal.service;

import com.orlandoprestige.orlandoproject.customers.internal.domain.BillingProfile;
import com.orlandoprestige.orlandoproject.customers.internal.repository.BillingProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingProfileService {

    private final BillingProfileRepository billingProfileRepository;

    @Transactional(readOnly = true)
    public List<BillingProfile> getByCustomerId(Long customerId) {
        return billingProfileRepository.findAllByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public Optional<BillingProfile> getByIdAndCustomerId(Long id, Long customerId) {
        return billingProfileRepository.findByIdAndCustomerId(id, customerId);
    }

    @Transactional
    public BillingProfile create(BillingProfile profile) {
        // If this is the first profile, make it default
        if (!billingProfileRepository.existsByCustomerId(profile.getCustomerId())) {
            profile.setDefault(true);
        }
        return billingProfileRepository.save(profile);
    }

    @Transactional
    public BillingProfile update(Long id, Long customerId, BillingProfile updated) {
        BillingProfile existing = billingProfileRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Billing profile not found: " + id));

        existing.setBillingType(updated.getBillingType());
        existing.setName(updated.getName());
        existing.setTin(updated.getTin());
        existing.setAddress(updated.getAddress());
        existing.setPaymentTerms(updated.getPaymentTerms());

        return billingProfileRepository.save(existing);
    }

    @Transactional
    public void delete(Long id, Long customerId) {
        BillingProfile profile = billingProfileRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Billing profile not found: " + id));
        billingProfileRepository.delete(profile);
    }

    public boolean existsByCustomerId(Long customerId) {
        return billingProfileRepository.existsByCustomerId(customerId);
    }
}
