// src/main/java/com/guvi/busapp/controller/UserController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.BookingResponseDto;
import com.guvi.busapp.dto.ChangePasswordDto; // Import DTO
import com.guvi.busapp.dto.UserProfileDto; // Import DTO
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.service.BookingService;
import com.guvi.busapp.service.UserService; // Import UserService
import jakarta.validation.Valid; // Import Valid
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*; // Import needed annotations

import java.util.List;

@RestController
@RequestMapping("/api/user") // Base path for user-specific API operations
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private BookingService bookingService;

    // **** ADDED UserService Injection ****
    @Autowired
    private UserService userService;

    // --- GET User's Booking History ---
    @GetMapping("/bookings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserBookingHistory(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required.");
        }
        String userEmail = userDetails.getUsername();
        logger.info("Request received to fetch booking history for user: {}", userEmail);

        try {
            List<BookingResponseDto> bookings = bookingService.getBookingsByUser(userEmail);
            logger.info("Returning {} bookings for user {}", bookings.size(), userEmail);
            return ResponseEntity.ok(bookings);
        } catch (ResourceNotFoundException e) {
            logger.error("Error fetching bookings: User {} not found.", userEmail, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching booking history for user {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while fetching booking history.");
        }
    }

    // --- ADDED Profile Management Endpoints ---

    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required.");
        }
        String userEmail = userDetails.getUsername();
        logger.info("Request received to fetch profile for user: {}", userEmail);
        try {
            UserProfileDto profile = userService.getUserProfile(userEmail);
            return ResponseEntity.ok(profile);
        } catch (ResourceNotFoundException e) {
            logger.error("Error fetching profile: User {} not found.", userEmail, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching profile for user {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while fetching the profile.");
        }
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateUserProfile(@AuthenticationPrincipal UserDetails userDetails,
                                               @Valid @RequestBody UserProfileDto profileDto) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required.");
        }
        String userEmail = userDetails.getUsername();
        // Prevent user from changing email via this endpoint if profileDto contains it
        if(profileDto.getEmail() != null && !profileDto.getEmail().equalsIgnoreCase(userEmail)){
            logger.warn("Attempt by user {} to change email via profile update endpoint denied.", userEmail);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email cannot be changed via this endpoint.");
        }
        profileDto.setEmail(userEmail); // Ensure the correct email is used

        logger.info("Request received to update profile for user: {}", userEmail);
        try {
            UserProfileDto updatedProfile = userService.updateUserProfile(userEmail, profileDto);
            return ResponseEntity.ok(updatedProfile);
        } catch (ResourceNotFoundException e) {
            logger.error("Error updating profile: User {} not found.", userEmail, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error updating profile for user {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating the profile.");
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                            @Valid @RequestBody ChangePasswordDto changePasswordDto) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication required.");
        }
        String userEmail = userDetails.getUsername();
        logger.info("Request received to change password for user: {}", userEmail);
        try {
            userService.changePassword(userEmail, changePasswordDto);
            logger.info("Password changed successfully for user: {}", userEmail);
            return ResponseEntity.ok("Password changed successfully.");
        } catch (ResourceNotFoundException e) {
            logger.error("Error changing password: User {} not found.", userEmail, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) { // Catch specific errors from service
            logger.warn("Password change validation failed for user {}: {}", userEmail, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error changing password for user {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while changing the password.");
        }
    }
}