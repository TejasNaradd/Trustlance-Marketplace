package com.trustlance.contractservice.controller;

import com.trustlance.contractservice.config.UserPrincipal;
import com.trustlance.contractservice.dto.*;
import com.trustlance.contractservice.service.ContractService;
import com.trustlance.contractservice.service.MilestoneService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final MilestoneService milestoneService;

    // ─── Create Contract ─────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ContractResponse> createContract(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateContractRequest request) {
        // buyerId always from JWT — never from request body
        return new ResponseEntity<>(contractService.createContract(principal.getId(), request), HttpStatus.CREATED);
    }

    // ─── List My Contracts ───────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ContractResponse>> listMyContracts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(contractService.listMyContracts(principal.getId(), pageable));
    }

    // ─── Get Contract ────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContractResponse> getContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(contractService.getContract(id, principal));
    }

    // ─── Accept Contract (SELLER only) ───────────────────────────────────────────

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ContractResponse> acceptContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Object-level check inside service: must be the designated seller
        return ResponseEntity.ok(contractService.acceptContract(id, principal.getId()));
    }

    // ─── Cancel Contract ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContractResponse> cancelContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Service enforces participant or ADMIN check
        return ResponseEntity.ok(contractService.cancelContract(id, principal));
    }

    // ─── Add Milestone to Contract ───────────────────────────────────────────────

    @PostMapping("/{id}/milestones")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<MilestoneResponse> addMilestone(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMilestoneRequest request) {
        return new ResponseEntity<>(milestoneService.addMilestone(id, principal.getId(), request), HttpStatus.CREATED);
    }

    // ─── List Contract Milestones ────────────────────────────────────────────────

    @GetMapping("/{id}/milestones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MilestoneResponse>> listMilestones(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(milestoneService.listMilestones(id, principal));
    }
}
