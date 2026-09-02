package com.trustlance.userservice.service;

import com.trustlance.userservice.config.JwtUtils;
import com.trustlance.userservice.dto.*;
import com.trustlance.userservice.exception.AppException;
import com.trustlance.userservice.model.RefreshToken;
import com.trustlance.userservice.model.User;
import com.trustlance.userservice.model.UserRole;
import com.trustlance.userservice.model.UserStatus;
import com.trustlance.userservice.repository.RefreshTokenRepository;
import com.trustlance.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final GoogleAuthService googleAuthService;

    @Value("${trustlance.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == UserRole.ADMIN) {
            throw new AppException("Cannot register as ADMIN via public registration", HttpStatus.FORBIDDEN, "ADMIN_REGISTRATION_FORBIDDEN");
        }
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new AppException("Email is already in use", HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
        }
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new AppException("Username is already in use", HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS");
        }

        User user = User.builder()
                .userName(request.getUserName())
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByEmail(identifier.toLowerCase())
                .or(() -> userRepository.findByUserName(identifier))
                .orElseThrow(() -> new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException("Account is " + user.getStatus(), HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleAuthService.GoogleUserInfo googleUser = googleAuthService.verifyToken(request.getIdToken());
        User user = userRepository.findByEmail(googleUser.email().toLowerCase())
                .orElseGet(() -> {
                    UserRole role = request.getRequestedRole() != null && request.getRequestedRole() != UserRole.ADMIN
                            ? request.getRequestedRole()
                            : UserRole.BUYER;
                    User newUser = User.builder()
                            .userName("g_" + UUID.randomUUID().toString().substring(0, 8))
                            .email(googleUser.email().toLowerCase())
                            .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .firstName(googleUser.firstName())
                            .lastName(googleUser.lastName())
                            .role(role)
                            .status(UserStatus.ACTIVE)
                            .build();
                    return userRepository.save(newUser);
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException("Account is " + user.getStatus(), HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"));

        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            throw new AppException("Refresh token is expired or revoked", HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN");
        }

        User user = token.getUser();
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String userId) {
        try {
            UUID id = UUID.fromString(userId);
            userRepository.findById(id).ifPresent(refreshTokenRepository::revokeAllByUser);
        } catch (Exception ignored) {}
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getRole().name(), user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(jwtUtils.getAccessTokenExpirationMs())
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
