package com.trustlance.userservice.service;

import com.trustlance.userservice.dto.*;
import com.trustlance.userservice.exception.AppException;
import com.trustlance.userservice.model.User;
import com.trustlance.userservice.model.UserRole;
import com.trustlance.userservice.model.UserStatus;
import com.trustlance.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = findUserOrThrow(userId);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException("Current password does not match", HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        return mapToResponse(findUserOrThrow(userId));
    }

    @Transactional
    public UserResponse updateStatus(UUID userId, UpdateStatusRequest request) {
        User user = findUserOrThrow(userId);
        user.setStatus(request.getStatus());
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateRole(UUID userId, UpdateRoleRequest request) {
        User user = findUserOrThrow(userId);
        user.setRole(request.getRole());
        return mapToResponse(userRepository.save(user));
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found with id: " + userId, HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
