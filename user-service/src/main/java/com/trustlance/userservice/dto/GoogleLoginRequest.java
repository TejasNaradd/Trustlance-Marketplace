package com.trustlance.userservice.dto;

import com.trustlance.userservice.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken;

    private UserRole requestedRole;
}
