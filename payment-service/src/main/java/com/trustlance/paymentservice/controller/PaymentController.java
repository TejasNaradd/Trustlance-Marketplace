package com.trustlance.paymentservice.controller;

import com.trustlance.paymentservice.config.UserPrincipal;
import com.trustlance.paymentservice.dto.CreatePaymentIntentRequest;
import com.trustlance.paymentservice.dto.PaymentIntentResponse;
import com.trustlance.paymentservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/intents")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<PaymentIntentResponse> createIntent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        return new ResponseEntity<>(paymentService.createIntent(principal.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentIntentResponse> getIntent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.getIntent(id, principal.getId()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentIntentResponse>> listMyPayments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(paymentService.listMyPayments(principal.getId(), pageable));
    }

    /**
     * Webhook endpoint — called directly by payment provider, NOT through normal auth.
     * Security: provider signature verified inside service layer.
     */
    @PostMapping("/webhooks/{provider}")
    public ResponseEntity<Void> webhook(
            @PathVariable String provider,
            @RequestBody String rawPayload,
            HttpServletRequest request) {
        // Signature header varies by provider: X-Razorpay-Signature, Stripe-Signature etc.
        String signature = request.getHeader("X-Payment-Signature");
        paymentService.processWebhook(provider, rawPayload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<PaymentIntentResponse> retryPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.retryPayment(id, principal.getId()));
    }
}
