package com.trustlance.paymentservice.provider;

import com.trustlance.paymentservice.dto.PaymentIntentResponse;
import com.trustlance.paymentservice.model.PaymentIntent;

import java.util.Map;

/**
 * Provider adapter interface — enables swapping Razorpay/Stripe/etc without
 * changing payment business logic. New providers implement this interface.
 */
public interface PaymentProvider {

    /** Returns the provider name key e.g. "razorpay", "stripe" */
    String getName();

    /**
     * Creates a payment order/session with the external provider.
     * Returns the provider-specific order ID and client secret/key for the frontend.
     */
    ProviderOrderResult createOrder(PaymentIntent intent);

    /**
     * Verifies a webhook signature server-side — authoritative proof of payment.
     * Throws IllegalArgumentException if signature is invalid.
     */
    void verifyWebhookSignature(String rawPayload, String signature, String secret);

    /**
     * Extracts the provider event ID and event type from the raw webhook payload.
     */
    WebhookEventData extractEventData(String rawPayload);

    record ProviderOrderResult(String providerOrderId, Map<String, String> clientMetadata) {}

    record WebhookEventData(String providerEventId, String eventType, String providerPaymentId, boolean isSuccess) {}
}
