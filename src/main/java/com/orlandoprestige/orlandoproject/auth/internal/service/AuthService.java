package com.orlandoprestige.orlandoproject.auth.internal.service;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.auth.internal.domain.Permission;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.LoginRequestDto;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.LoginResponseDto;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.RegisterRequestDto;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.UserProfileResponseDto;
import com.orlandoprestige.orlandoproject.auth.internal.repository.StaffRepository;
import com.orlandoprestige.orlandoproject.customers.CustomerFacade;
import com.orlandoprestige.orlandoproject.customers.dto.CustomerCredentialsDto;
import com.orlandoprestige.orlandoproject.customers.dto.CustomerInfoDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CustomerFacade customerFacade;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponseDto login(LoginRequestDto request) {
        // Dual-lookup: check customers first, then staff
        var customerOpt = customerFacade.findCredentialsByEmail(request.email());
        if (customerOpt.isPresent()) {
            CustomerCredentialsDto creds = customerOpt.get();
            if (!passwordEncoder.matches(request.password(), creds.passwordHash())) {
                throw new IllegalArgumentException("Invalid credentials");
            }
            String token = jwtService.generateToken(creds.userId(), creds.username(), "ROLE_CUSTOMER");
            return new LoginResponseDto(token, "ROLE_CUSTOMER", creds.userId(), List.of());
        }

        // Check staff
        var staffOpt = staffRepository.findByEmail(request.email());
        if (staffOpt.isPresent()) {
            var staff = staffOpt.get();
            if (!passwordEncoder.matches(request.password(), staff.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid credentials");
            }
            String role;
            List<String> permissions;
            if (staff.isSuperAdmin()) {
                role = "ROLE_SUPER_ADMIN";
                permissions = Arrays.stream(Permission.values()).map(Enum::name).toList();
            } else {
                role = "ROLE_STAFF";
                permissions = staff.getPermissions().stream().map(Enum::name).toList();
            }
            String token = jwtService.generateToken(staff.getId(), staff.getEmail(), role, permissions);
            return new LoginResponseDto(token, role, staff.getId(), permissions);
        }

        throw new IllegalArgumentException("Invalid credentials");
    }

    public void register(RegisterRequestDto request) {
        if (customerFacade.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        String hashedPassword = passwordEncoder.encode(request.password());
        customerFacade.registerCustomer(
                request.firstName(),
                request.lastName(),
                request.email(),
                hashedPassword
        );
    }

    public UserProfileResponseDto getUserProfile(AuthenticatedUser user) {
        if ("ROLE_CUSTOMER".equals(user.role())) {
            CustomerInfoDataDto info = customerFacade.findCustomerById(user.userId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            String[] nameParts = info.name().split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            return new UserProfileResponseDto(
                    user.userId(), user.email(), firstName, lastName, user.role(), List.of());
        }

        // Staff or Super Admin
        var staff = staffRepository.findById(user.userId())
                .orElseThrow(() -> new IllegalArgumentException("Staff not found"));
        List<String> permissions;
        if (staff.isSuperAdmin()) {
            permissions = Arrays.stream(Permission.values()).map(Enum::name).toList();
        } else {
            permissions = staff.getPermissions().stream().map(Enum::name).toList();
        }
        return new UserProfileResponseDto(
                staff.getId(), staff.getEmail(), staff.getFirstName(), staff.getLastName(),
                user.role(), permissions);
    }
}

