package com.trustlance.contractservice.controller;

import com.trustlance.contractservice.config.UserPrincipal;
import com.trustlance.contractservice.dto.*;
import com.trustlance.contractservice.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;

    // ─── Submit Work (SELLER only) ────────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<MilestoneResponse> submitWork(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) SubmitMilestoneRequest request) {
        SubmitMilestoneRequest req = request != null ? request : new SubmitMilestoneRequest();
        // Object-level check inside service: must be the contract's seller
        return ResponseEntity.ok(milestoneService.submitWork(id, principal.getId(), req));
    }

    // ─── Approve Milestone (BUYER only) ──────────────────────────────────────────

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<MilestoneResponse> approveMilestone(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Object-level check inside service: must be the contract's buyer
        return ResponseEntity.ok(milestoneService.approveMilestone(id, principal.getId()));
    }

    // ─── Request Revision (BUYER only) ───────────────────────────────────────────

    @PostMapping("/{id}/revision")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<MilestoneResponse> requestRevision(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) RevisionRequest request) {
        RevisionRequest req = request != null ? request : new RevisionRequest();
        // Object-level check inside service: must be the contract's buyer
        return ResponseEntity.ok(milestoneService.requestRevision(id, principal.getId(), req));
    }
}
