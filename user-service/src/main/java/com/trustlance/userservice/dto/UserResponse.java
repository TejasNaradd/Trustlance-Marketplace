package com.trustlance.userservice.dto;

import com.trustlance.userservice.model.UserRole;
import com.trustlance.userservice.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID userId;
    private String userName;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
