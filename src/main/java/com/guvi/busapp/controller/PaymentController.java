// src/main/java/com/guvi/busapp/controller/PaymentController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.PaymentIntentResponseDto;
import com.guvi.busapp.dto.PaymentRequestDto;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.model.Booking;
import com.guvi.busapp.repository.BookingRepository; // Need BookingRepository
import com.guvi.busapp.repository.UserRepository;
import com.guvi.busapp.service.PaymentService;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // For publishable key
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // For access check
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payment") // Base path for payment related APIs
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BookingRepository bookingRepository; // Inject to fetch booking details
    @Autowired
    private UserRepository userRepository; // Inject to verify user

    @Value("${stripe.publishable.key}") // Inject publishable key if needed in response
    private String stripePublishableKey;


    @PostMapping("/create-intent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createPaymentIntent(
            @Valid @RequestBody PaymentRequestDto paymentRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required.");
        }
        String userEmail = userDetails.getUsername();
        logger.info("Received request to create payment intent for booking ID {} from user {}",
                paymentRequest.getBookingId(), userEmail);

        try {
            // 1. Fetch the Booking
            Booking booking = bookingRepository.findById(paymentRequest.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", "ID", paymentRequest.getBookingId()));

            // 2. Verify Booking belongs to the authenticated user
            if (!booking.getUser().getEmail().equals(userEmail)) {
                logger.warn("User {} attempted to create payment intent for booking ID {} which does not belong to them.", userEmail, booking.getId());
                throw new AccessDeniedException("Access denied to this booking.");
            }

            // 3. Verify Booking status is PENDING
            if (booking.getStatus() != Booking.BookingStatus.PENDING) {
                logger.warn("Attempted to create payment intent for booking ID {} with status {}, expected PENDING.", booking.getId(), booking.getStatus());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment cannot be initiated for booking with status: " + booking.getStatus());
            }

            // 4. Calculate amount in smallest currency unit (e.g., paise for INR)
            // Assuming INR for now, adjust if supporting multiple currencies
            String currency = "inr";
            // Multiply by 100 and convert to Long for Stripe
            long amountInPaise = booking.getTotalFare().multiply(new BigDecimal("100")).longValueExact();

            // 5. Call PaymentService to create Stripe Payment Intent
            PaymentIntent paymentIntent = paymentService.createPaymentIntent(
                    booking.getId(),
                    amountInPaise,
                    currency,
                    userEmail
            );

            // 6. Return the client secret to the frontend
            PaymentIntentResponseDto responseDto = new PaymentIntentResponseDto(paymentIntent.getClientSecret());
            // Optionally add publishable key: responseDto.setPublishableKey(stripePublishableKey);

            logger.info("Successfully created Payment Intent {} for booking ID {}", paymentIntent.getId(), booking.getId());
            return ResponseEntity.ok(responseDto);

        } catch (ResourceNotFoundException e) {
            logger.warn("Payment intent creation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            logger.error("Payment intent creation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) { // Catch StripeException or other errors
            logger.error("Error creating Stripe Payment Intent for booking ID {}: {}", paymentRequest.getBookingId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create payment intent.");
        }
    }
}