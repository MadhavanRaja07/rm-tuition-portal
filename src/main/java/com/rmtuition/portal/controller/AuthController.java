package com.rmtuition.portal.controller;

import com.rmtuition.portal.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.registerUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getName(),
                    request.getPhone(),
                    request.getStandard()
            );
            return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> response = authService.authenticateUser(
                    request.getEmail(),
                    request.getPassword()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password!"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        // Construct base URL: e.g. http://localhost:8080 or https://custom-domain
        String scheme = servletRequest.getScheme();
        String serverName = servletRequest.getServerName();
        int serverPort = servletRequest.getServerPort();
        
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(scheme).append("://").append(serverName);
        if (serverPort != 80 && serverPort != 443) {
            urlBuilder.append(":").append(serverPort);
        }
        
        authService.processForgotPassword(request.getEmail(), urlBuilder.toString());
        return ResponseEntity.ok(Map.of("message", "If the email is registered, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Data
    public static class RegisterRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String name;
        @NotBlank
        private String phone;
        private String standard; // Required for students
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank
        private String newPassword;
    }
}
