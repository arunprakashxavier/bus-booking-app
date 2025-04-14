// src/main/java/com/guvi/busapp/service/PaymentService.java
package com.guvi.busapp.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

/**
 * Service interface for handling payment gateway interactions.
 */
public interface PaymentService {

    PaymentIntent createPaymentIntent(Long bookingId, Long amount, String currency, String userEmail) throws StripeException;

    boolean verifyWebhookSignature(String payload, String sigHeader);

    // --- MODIFIED Webhook Handler Method Signatures ---

    /**
     * Handles the logic after a Stripe PaymentIntent succeeds.
     * Updates Booking status to CONFIRMED and ScheduledTrip seats to BOOKED.
     *
     * @param paymentIntentId The ID of the successful Stripe PaymentIntent (pi_...).
     * @param bookingId       The corresponding booking ID extracted from metadata.
     * @param amount          (Optional) The amount received, for verification.
     * @param currency        (Optional) The currency received, for verification.
     */
    void handlePaymentSuccess(String paymentIntentId, Long bookingId, Long amount, String currency);

    /**
     * Handles the logic after a Stripe PaymentIntent fails.
     * Updates Booking status to FAILED and reverts ScheduledTrip seats to AVAILABLE.
     *
     * @param paymentIntentId The ID of the failed Stripe PaymentIntent (pi_...).
     * @param bookingId       The corresponding booking ID extracted from metadata.
     */
    void handlePaymentFailure(String paymentIntentId, Long bookingId);
}