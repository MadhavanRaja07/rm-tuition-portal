package com.rmtuition.portal.service;

import com.rmtuition.portal.model.Role;
import com.rmtuition.portal.model.User;
import com.rmtuition.portal.repository.UserRepository;
import com.rmtuition.portal.security.JwtUtils;
import com.rmtuition.portal.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private NotificationService notificationService;

    private static final String ADMIN_EMAIL = "madhavanraja.tution@gmail.com";

    public User registerUser(String email, String password, String name, String phone, String standard) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        Role role = email.equalsIgnoreCase(ADMIN_EMAIL) ? Role.ROLE_ADMIN : Role.ROLE_STUDENT;

        User user = User.builder()
                .email(email.toLowerCase())
                .password(passwordEncoder.encode(password))
                .name(name)
                .phone(phone)
                .standard(role == Role.ROLE_ADMIN ? null : standard) // Admins don't have standard classes
                .role(role)
                .createdAt(new Date())
                .build();

        return userRepository.save(user);
    }

    public Map<String, Object> authenticateUser(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email.toLowerCase(), password));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("id", userPrincipal.getId());
        response.put("email", userPrincipal.getEmail());
        response.put("name", userPrincipal.getName());
        response.put("role", userPrincipal.getRole());

        // Find standard if they are student
        userRepository.findByEmail(userPrincipal.getEmail()).ifPresent(user -> {
            response.put("standard", user.getStandard());
            response.put("phone", user.getPhone());
            response.put("avatarUrl", user.getAvatarUrl());
        });

        return response;
    }

    public void processForgotPassword(String email, String requestSchemeAndHost) {
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase());
        if (userOpt.isEmpty()) {
            return; // Return silently to prevent user enumeration attacks
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        // Expire in 30 minutes
        user.setResetTokenExpiry(new Date(System.currentTimeMillis() + 30 * 60 * 1000));
        userRepository.save(user);

        String resetLink = requestSchemeAndHost + "/reset-password.html?token=" + token;
        notificationService.sendPasswordResetLink(user.getEmail(), resetLink);
    }

    public void resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findAll().stream()
                .filter(u -> token.equals(u.getResetToken()) && u.getResetTokenExpiry() != null && u.getResetTokenExpiry().after(new Date()))
                .findFirst();

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired password reset token!");
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public User adminCreateStudent(String email, String password, String name, String phone, String standard) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        User user = User.builder()
                .email(email.toLowerCase())
                .password(passwordEncoder.encode(password))
                .name(name)
                .phone(phone)
                .standard(standard)
                .role(Role.ROLE_STUDENT)
                .createdAt(new Date())
                .build();

        return userRepository.save(user);
    }
}
