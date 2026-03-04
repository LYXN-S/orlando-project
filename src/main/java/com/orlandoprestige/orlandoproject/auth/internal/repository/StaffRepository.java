package com.orlandoprestige.orlandoproject.auth.internal.repository;

import com.orlandoprestige.orlandoproject.auth.internal.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByEmail(String email);
    boolean existsByEmail(String email);
}

