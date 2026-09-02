package com.trustlance.marketplaceservice.controller;

import com.trustlance.marketplaceservice.config.UserPrincipal;
import com.trustlance.marketplaceservice.dto.*;
import com.trustlance.marketplaceservice.exception.AppException;
import com.trustlance.marketplaceservice.service.MarketplaceService;
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
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceController {

    private final MarketplaceService marketplaceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ServiceResponse> createService(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateServiceRequest request) {
        if (principal == null) {
            throw new AppException("Unauthorized", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        return new ResponseEntity<>(marketplaceService.createService(principal.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable UUID id) {
        return ResponseEntity.ok(marketplaceService.getServiceById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ServiceResponse>> searchServices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(marketplaceService.searchServices(search, categoryId, pageable));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<ServiceResponse>> getServicesBySeller(
            @PathVariable UUID sellerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(marketplaceService.getServicesBySeller(sellerId, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(marketplaceService.updateService(id, principal.getId(), principal.getRole(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteService(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        marketplaceService.deleteService(id, principal.getId(), principal.getRole());
        return ResponseEntity.noContent().build();
    }
}
