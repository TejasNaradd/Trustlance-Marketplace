package com.trustlance.userservice.dto;

import com.trustlance.userservice.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    @NotNull
    private UserRole role;
}
