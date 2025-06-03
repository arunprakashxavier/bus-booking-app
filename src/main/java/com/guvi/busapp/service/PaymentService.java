
// src/main/java/com/guvi/busapp/service/PaymentService.java
package com.guvi.busapp.service;

import com.guvi.busapp.exception.ResourceNotFoundException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.security.access.AccessDeniedException;

/**
 * Service interface for handling payment gateway interactions.
 */
public interface PaymentService {

    /**
     * Creates a Stripe PaymentIntent for a given booking.
     * Verifies booking ownership and status.
     *
     * @param bookingId The ID of the booking.
     * @param userEmailForVerification The email of the authenticated user, for verification.
     * @return The created Stripe PaymentIntent.
     * @throws StripeException If there's an error with the Stripe API.
     * @throws ResourceNotFoundException If the booking is not found.
     * @throws AccessDeniedException If the user does not own the booking.
     * @throws IllegalStateException If the booking is not in a PENDING state.
     */
    PaymentIntent createPaymentIntent(Long bookingId, String userEmailForVerification)
            throws StripeException, ResourceNotFoundException, AccessDeniedException, IllegalStateException;


    boolean verifyWebhookSignature(String payload, String sigHeader);

    void handlePaymentSuccess(String paymentIntentId, Long bookingId, Long amount, String currency);

    void handlePaymentFailure(String paymentIntentId, Long bookingId);
}