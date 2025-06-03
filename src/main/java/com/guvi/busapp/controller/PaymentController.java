

// src/main/java/com/guvi/busapp/controller/PaymentController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.PaymentIntentResponseDto;
import com.guvi.busapp.dto.PaymentRequestDto;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }


    @PostMapping("/create-intent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createPaymentIntent(
            @Valid @RequestBody PaymentRequestDto paymentRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            logger.warn("Create payment intent request with null UserDetails principal, though @PreAuthorize('hasRole(\\'USER\\')') is active.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required.");
        }
        String userEmail = userDetails.getUsername();
        logger.info("Received request to create payment intent for booking ID {} from user {}",
                paymentRequest.getBookingId(), userEmail);

        try {
            PaymentIntent paymentIntent = paymentService.createPaymentIntent(
                    paymentRequest.getBookingId(),
                    userEmail
            );

            PaymentIntentResponseDto responseDto = new PaymentIntentResponseDto(paymentIntent.getClientSecret());

            logger.info("Successfully created Payment Intent {} for booking ID {}", paymentIntent.getId(), paymentRequest.getBookingId());
            return ResponseEntity.ok(responseDto);

        } catch (ResourceNotFoundException e) {
            logger.warn("Payment intent creation failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            logger.warn("Payment intent creation failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Payment intent creation failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (StripeException e) {
            logger.error("Stripe API error during payment intent creation for booking ID {} by user {}: {}",
                    paymentRequest.getBookingId(), userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error communicating with payment provider.");
        } catch (Exception e) {
            logger.error("Unexpected error creating Stripe Payment Intent for booking ID {} by user {}: {}",
                    paymentRequest.getBookingId(), userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create payment intent due to an internal error.");
        }
    }
}
