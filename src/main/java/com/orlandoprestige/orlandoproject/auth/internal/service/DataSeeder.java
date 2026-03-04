package com.orlandoprestige.orlandoproject.auth.internal.service;

import com.orlandoprestige.orlandoproject.auth.internal.domain.Staff;
import com.orlandoprestige.orlandoproject.auth.internal.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the default admin (staff) user on application startup.
 * Idempotent — skips if the email already exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "orlandoprestige@gmail.com";
    private static final String ADMIN_PASSWORD = "Orlando@Prestige0304";
    private static final String ADMIN_FIRST_NAME = "Orlando";
    private static final String ADMIN_LAST_NAME = "Prestige";

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (staffRepository.existsByEmail(ADMIN_EMAIL)) {
            // Ensure existing admin is promoted to super admin
            staffRepository.findByEmail(ADMIN_EMAIL).ifPresent(admin -> {
                if (!admin.isSuperAdmin()) {
                    admin.setSuperAdmin(true);
                    staffRepository.save(admin);
                    log.info("Existing admin promoted to super admin: {}", ADMIN_EMAIL);
                }
            });
            log.info("Admin user already exists, skipping seed.");
            return;
        }

        Staff admin = new Staff();
        admin.setFirstName(ADMIN_FIRST_NAME);
        admin.setLastName(ADMIN_LAST_NAME);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setSuperAdmin(true);

        staffRepository.save(admin);
        log.info("Default super admin user seeded: {}", ADMIN_EMAIL);
    }
}
