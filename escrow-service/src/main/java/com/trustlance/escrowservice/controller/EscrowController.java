package com.trustlance.escrowservice.controller;

import com.trustlance.escrowservice.config.UserPrincipal;
import com.trustlance.escrowservice.dto.EscrowResponse;
import com.trustlance.escrowservice.service.EscrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/escrow")
@RequiredArgsConstructor
public class EscrowController {

    private final EscrowService escrowService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EscrowResponse> getEscrow(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(escrowService.getEscrow(id, principal));
    }

    @GetMapping("/contract/{contractId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EscrowResponse>> getContractEscrows(
            @PathVariable UUID contractId) {
        return ResponseEntity.ok(escrowService.getEscrowsByContract(contractId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<EscrowResponse>> listMyEscrows(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(escrowService.listMyEscrows(principal.getId(), pageable));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
    public ResponseEntity<EscrowResponse> releaseFunds(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(escrowService.releaseFunds(id, principal));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EscrowResponse> refundFunds(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(escrowService.refundFunds(id, principal));
    }

    @PostMapping("/{id}/freeze")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EscrowResponse> freezeFunds(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(escrowService.freezeFunds(id, principal.getId()));
    }
}
