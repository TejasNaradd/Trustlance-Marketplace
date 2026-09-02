package com.trustlance.userservice.dto;

import com.trustlance.userservice.model.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull
    private UserStatus status;
}
