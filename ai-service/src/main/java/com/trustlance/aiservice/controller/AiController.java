package com.trustlance.aiservice.controller;

import com.trustlance.aiservice.config.UserPrincipal;
import com.trustlance.aiservice.dto.VerificationRequest;
import com.trustlance.aiservice.model.AiVerification;
import com.trustlance.aiservice.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/verifications")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiVerification> submitVerification(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerificationRequest request) {
        return new ResponseEntity<>(aiService.submitForVerification(principal.getId(), request), HttpStatus.ACCEPTED);
    }
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AiVerification>> getMyVerifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(aiService.getMyVerifications(principal.getId(), pageable));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiVerification> getVerification(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getRole().equals("ADMIN");
        return ResponseEntity.ok(aiService.getVerification(id, principal.getId(), isAdmin));
    }
}
