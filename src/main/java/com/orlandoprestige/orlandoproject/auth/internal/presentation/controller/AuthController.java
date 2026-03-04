package com.orlandoprestige.orlandoproject.auth.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.LoginRequestDto;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.LoginResponseDto;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.RegisterRequestDto;
import com.orlandoprestige.orlandoproject.auth.internal.presentation.dto.UserProfileResponseDto;
import com.orlandoprestige.orlandoproject.auth.internal.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password, returns JWT token")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDto request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user's profile")
    public ResponseEntity<UserProfileResponseDto> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser user) {
        UserProfileResponseDto profile = authService.getUserProfile(user);
        return ResponseEntity.ok(profile);
    }
}

