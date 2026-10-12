package com.studentactivityhub.controller;

import com.studentactivityhub.dto.RegisterRequest;
import com.studentactivityhub.dto.LoginRequest;
import com.studentactivityhub.dto.AuthResponse;
import com.studentactivityhub.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Register a new student
    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // Login an existing user
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Change the password of the logged-in user
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> request) {

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        authService.changePassword(currentPassword, newPassword);

        return ResponseEntity.ok(
                Map.of("message", "Password changed successfully!")
        );
    }
}