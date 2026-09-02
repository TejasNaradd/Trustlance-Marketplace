package com.trustlance.reviewservice.controller;

import com.trustlance.reviewservice.config.UserPrincipal;
import com.trustlance.reviewservice.dto.CreateReviewRequest;
import com.trustlance.reviewservice.dto.ReviewResponse;
import com.trustlance.reviewservice.service.ReviewService;
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
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request) {
        return new ResponseEntity<>(reviewService.createReview(principal.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/seller/{sellerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReviewResponse>> getSellerReviews(
            @PathVariable UUID sellerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(sellerId, pageable));
    }

    @GetMapping("/buyer/{buyerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReviewResponse>> getBuyerReviews(
            @PathVariable UUID buyerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(buyerId, pageable));
    }

    @GetMapping("/contract/{contractId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReviewResponse>> getContractReviews(
            @PathVariable UUID contractId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByContract(contractId, pageable));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByMe(principal.getId(), pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable String id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
