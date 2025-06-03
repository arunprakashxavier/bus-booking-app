// src/main/java/com/guvi/busapp/controller/BookingController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.BookingRequestDto;
import com.guvi.busapp.dto.BookingResponseDto;
import com.guvi.busapp.dto.SeatLockRequestDto;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.exception.SeatUnavailableException;
import com.guvi.busapp.model.CustomUserDetails;
import com.guvi.busapp.service.BookingService;
import com.guvi.busapp.service.ScheduledTripService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails; // Keep this for @AuthenticationPrincipal type
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    private final ScheduledTripService scheduledTripService;
    private final BookingService bookingService;

    @Autowired
    public BookingController(ScheduledTripService scheduledTripService,
                             BookingService bookingService) {
        this.scheduledTripService = scheduledTripService;
        this.bookingService = bookingService;
    }

    // POST: Attempt to lock seats
    @PostMapping("/lock-seats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> lockSeatsForBooking(
            @Valid @RequestBody SeatLockRequestDto lockRequest,
            @AuthenticationPrincipal UserDetails userDetails) { // Keep UserDetails here

        if (userDetails == null) {
            // This should ideally be caught by Spring Security earlier
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required.");
        }

        // Get userId from CustomUserDetails
        Long userId;
        String userEmailForLog; // For logging purposes

        if (userDetails instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
            userId = customUserDetails.getId();
            userEmailForLog = customUserDetails.getUsername(); // Email
        } else {
            // Fallback or error if not CustomUserDetails - this shouldn't happen if UserDetailsServiceImpl is correct
            logger.error("Principal is not an instance of CustomUserDetails. Cannot retrieve custom user attributes.");
            // You might still be able to get the email if it's a standard UserDetails
            userEmailForLog = userDetails.getUsername();
            // If userId is absolutely critical and cannot be obtained, return an error.
            // However, ScheduledTripService.lockSeats expects userId.
            // This situation indicates a misconfiguration if it occurs.
            // For now, let's assume it won't happen with the CustomUserDetails setup.
            // If it did, we'd need a way to get the ID from the email via UserService.
            // But since lockSeats needs userId, we must get it.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("User details misconfiguration.");
        }
        // **** END OF MODIFIED PART ****

        logger.info("Received seat lock request from user ID {} (Email: {}) for trip ID {} seats {}",
                userId, userEmailForLog, lockRequest.getTripId(), lockRequest.getSeatNumbers());

        try {
            // Pass the obtained userId to the service method
            scheduledTripService.lockSeats(lockRequest.getTripId(), lockRequest.getSeatNumbers(), userId);
            logger.info("Seats locked successfully for user ID {} on trip ID {}", userId, lockRequest.getTripId());
            return ResponseEntity.ok().body("Seats locked successfully.");
        } catch (SeatUnavailableException e) {
            logger.warn("Seat locking failed for user ID {} on trip ID {}: {}", userId, lockRequest.getTripId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            logger.warn("Seat locking failed for user ID {} on trip ID {}: {}", userId, lockRequest.getTripId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) { // Catch if seat map is not initialized
            logger.error("Seat locking failed for user ID {} on trip ID {}: {}", userId, lockRequest.getTripId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        catch (Exception e) {
            logger.error("Unexpected error during seat locking for user ID {} on trip ID {}: {}", userId, lockRequest.getTripId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred while locking seats.");
        }
    }

    // POST: Create the actual booking
    @PostMapping // Maps to POST /api/booking/
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequestDto bookingRequest,
            @AuthenticationPrincipal UserDetails userDetails) { // userDetails will be CustomUserDetails

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required.");
        }
        // We need the email for BookingService.createBooking
        String userEmail = userDetails.getUsername();

        logger.info("Received booking request from user {} for trip ID {} seats {}",
                userEmail, bookingRequest.getTripId(), bookingRequest.getSelectedSeats());

        try {
            BookingResponseDto createdBooking = bookingService.createBooking(bookingRequest, userEmail);
            logger.info("Booking created successfully with ID: {}", createdBooking.getBookingId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);
        } catch (SeatUnavailableException e) {
            logger.warn("Booking failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            logger.warn("Booking failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("Booking failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) { // Catch if seat map is not initialized from BookingService
            logger.error("Booking failed for user {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        catch (Exception e) {
            logger.error("Unexpected error during booking creation for user {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred during booking.");
        }
    }
}
