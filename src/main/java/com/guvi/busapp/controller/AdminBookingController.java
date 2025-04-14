package com.guvi.busapp.controller;

import com.guvi.busapp.dto.BookingResponseDto;
import com.guvi.busapp.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings") // Base path for admin booking operations
@PreAuthorize("hasRole('ADMIN')") // Secure all methods in this controller for ADMIN role
public class AdminBookingController {

    private static final Logger logger = LoggerFactory.getLogger(AdminBookingController.class);

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public ResponseEntity<?> getAllBookings() {
        logger.info("Admin request received to fetch all bookings.");
        try {
            List<BookingResponseDto> bookings = bookingService.getAllBookings();
            logger.info("Returning {} total bookings.", bookings.size());
            if (bookings.isEmpty()) {
                return ResponseEntity.noContent().build(); // Return 204 if no bookings exist
            }
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            logger.error("Unexpected error fetching all bookings for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching all bookings.");
        }
    }

}