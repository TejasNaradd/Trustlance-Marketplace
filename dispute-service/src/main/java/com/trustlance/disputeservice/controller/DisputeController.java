package com.trustlance.disputeservice.controller;

import com.trustlance.disputeservice.config.UserPrincipal;
import com.trustlance.disputeservice.dto.CreateDisputeRequest;
import com.trustlance.disputeservice.dto.DisputeResponse;
import com.trustlance.disputeservice.dto.ResolveDisputeRequest;
import com.trustlance.disputeservice.dto.SubmitEvidenceRequest;
import com.trustlance.disputeservice.model.Dispute;
import com.trustlance.disputeservice.service.DisputeService;
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

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DisputeResponse> createDispute(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateDisputeRequest request) {
        return new ResponseEntity<>(disputeService.createDispute(principal.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DisputeResponse> getDispute(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = "ADMIN".equals(principal.getRole());
        return ResponseEntity.ok(disputeService.getDispute(id, principal.getId(), isAdmin));
    }

    @PostMapping("/{id}/evidence")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DisputeResponse> submitEvidence(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitEvidenceRequest request) {
        return ResponseEntity.ok(disputeService.submitEvidence(id, principal.getId(), request));
    }

    @GetMapping("/{id}/evidence")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Dispute.Evidence>> getEvidence(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = "ADMIN".equals(principal.getRole());
        return ResponseEntity.ok(disputeService.getEvidence(id, principal.getId(), isAdmin));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DisputeResponse>> listMyDisputes(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(disputeService.listMyDisputes(principal.getId(), pageable));
    }
}

@RestController
@RequestMapping("/api/v1/admin/disputes")
@RequiredArgsConstructor
class AdminDisputeController {

    private final DisputeService disputeService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DisputeResponse>> listAllDisputes(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(disputeService.listAllDisputes(pageable));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ResolveDisputeRequest request) {
        return ResponseEntity.ok(disputeService.resolveDispute(id, principal.getId(), request));
    }
}
