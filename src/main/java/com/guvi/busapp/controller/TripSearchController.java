// src/main/java/com/guvi/busapp/controller/TripSearchController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.ScheduledTripResponseDto;
import com.guvi.busapp.dto.SeatLayoutDto;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.service.ScheduledTripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trips") // Base path for public/user trip-related APIs
public class TripSearchController {

    private static final Logger logger = LoggerFactory.getLogger(TripSearchController.class);

    private final ScheduledTripService scheduledTripService;

    @Autowired
    public TripSearchController(ScheduledTripService scheduledTripService) {
        this.scheduledTripService = scheduledTripService;
    }

    // GET: Search for available trips (Publicly accessible)
    @GetMapping("/search")
    public ResponseEntity<List<ScheduledTripResponseDto>> searchAvailableTrips(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        logger.info("Received trip search request for origin={}, destination={}, date={}", origin, destination, date);
        if (origin == null || origin.isBlank() || destination == null || destination.isBlank() || date == null) {
            logger.warn("Search request received with missing parameters.");
            return ResponseEntity.badRequest().build();
        }
        if (origin.equalsIgnoreCase(destination)) {
            logger.warn("Search request received with same origin and destination.");
            return ResponseEntity.badRequest().body(List.of());
        }
        try {
            List<ScheduledTripResponseDto> availableTrips = scheduledTripService.findAvailableTrips(origin, destination, date);
            logger.info("Found {} available trips for the search criteria.", availableTrips.size());
            return ResponseEntity.ok(availableTrips);
        } catch (Exception e) {
            logger.error("Error during trip search for origin={}, destination={}, date={}: {}", origin, destination, date, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET: Seat Layout for a specific Trip (Requires authentication)
    @GetMapping("/{tripId}/seats")
    @PreAuthorize("isAuthenticated()") // Ensure user is logged in
    public ResponseEntity<SeatLayoutDto> getSeatsForTrip(@PathVariable Long tripId) {
        logger.info("Received request for seat layout for trip ID: {}", tripId);
        try {
            SeatLayoutDto seatLayout = scheduledTripService.getSeatLayoutForTrip(tripId);
            return ResponseEntity.ok(seatLayout);
        } catch (ResourceNotFoundException e) {
            logger.warn("Seat layout requested for non-existent trip ID: {}", tripId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching seat layout for trip ID {}: {}", tripId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // **** ADDED: GET Trip Details by ID ****
    // Used by booking confirmation page to display summary
    // Requires authentication
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Ensure user is logged in
    public ResponseEntity<ScheduledTripResponseDto> getTripById(@PathVariable Long id) {
        logger.info("Received request for trip details for ID: {}", id);
        try {
            ScheduledTripResponseDto trip = scheduledTripService.getScheduledTripById(id);
            return ResponseEntity.ok(trip);
        } catch (ResourceNotFoundException e) {
            logger.warn("Trip details requested for non-existent trip ID: {}", id);
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (Exception e) {
            logger.error("Error fetching trip details for trip ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build(); // 500 Internal Server Error
        }
    }
}