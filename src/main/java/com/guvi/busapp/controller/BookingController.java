// src/main/java/com/guvi/busapp/controller/BookingController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.BookingRequestDto;
import com.guvi.busapp.dto.BookingResponseDto;
import com.guvi.busapp.dto.SeatLockRequestDto;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.exception.SeatUnavailableException;
import com.guvi.busapp.model.User;
import com.guvi.busapp.repository.UserRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booking")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private ScheduledTripService scheduledTripService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private UserRepository userRepository;

    // POST: Attempt to lock seats
    @PostMapping("/lock-seats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> lockSeatsForBooking(
            @Valid @RequestBody SeatLockRequestDto lockRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required."); }
        User currentUser = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (currentUser == null) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("User data inconsistent."); }
        Long userId = currentUser.getId();
        logger.info("Received seat lock request from user ID {} for trip ID {} seats {}", userId, lockRequest.getTripId(), lockRequest.getSeatNumbers());
        try {
            scheduledTripService.lockSeats(lockRequest.getTripId(), lockRequest.getSeatNumbers(), userId);
            logger.info("Seats locked successfully for user ID {} on trip ID {}", userId, lockRequest.getTripId());
            return ResponseEntity.ok().body("Seats locked successfully.");
        } catch (SeatUnavailableException e) {
            logger.warn("Seat locking failed for user ID {} on trip ID {}: {}", userId, lockRequest.getTripId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            logger.warn("Seat locking failed for user ID {} on trip ID {}: {}", userId, lockRequest.getTripId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during seat locking for user ID {} on trip ID {}: {}", userId, lockRequest.getTripId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred while locking seats.");
        }
    }

    // POST: Create the actual booking
    @PostMapping // Maps to POST /api/booking/
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequestDto bookingRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required.");
        }
        String userEmail = userDetails.getUsername();

        logger.info("Received booking request from user {} for trip ID {} seats {}",
                userEmail, bookingRequest.getTripId(), bookingRequest.getSelectedSeats());

        try {
            BookingResponseDto createdBooking = bookingService.createBooking(bookingRequest, userEmail);
            logger.info("Booking created successfully with ID: {}", createdBooking.getBookingId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking); // 201 Created
        } catch (SeatUnavailableException e) {
            logger.warn("Booking failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409
        } catch (ResourceNotFoundException e) {
            logger.warn("Booking failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); // 404
        } catch (IllegalArgumentException e) {
            logger.warn("Booking failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage()); // 400
        } catch (Exception e) {
            logger.error("Unexpected error during booking creation for user {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred during booking."); // 500
        }
    }
}