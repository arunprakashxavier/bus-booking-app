// src/main/java/com/guvi/busapp/controller/AdminScheduledTripController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.ScheduledTripRequestDto;
import com.guvi.busapp.dto.ScheduledTripResponseDto;
import com.guvi.busapp.service.ScheduledTripService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/scheduled-trips") // Base path for admin scheduled trip operations
@PreAuthorize("hasRole('ADMIN')") // Secure all endpoints for ADMIN role
public class AdminScheduledTripController {

    private static final Logger logger = LoggerFactory.getLogger(AdminScheduledTripController.class);

    private final ScheduledTripService scheduledTripService;

    @Autowired
    public AdminScheduledTripController(ScheduledTripService scheduledTripService) {
        this.scheduledTripService = scheduledTripService;
    }

    // POST: Schedule a new Trip
    @PostMapping
    public ResponseEntity<ScheduledTripResponseDto> scheduleTrip(@Valid @RequestBody ScheduledTripRequestDto requestDto) {
        logger.info("Admin request received to schedule trip for Route ID: {} and Bus ID: {}", requestDto.getRouteId(), requestDto.getBusId());
        ScheduledTripResponseDto createdTrip = scheduledTripService.scheduleTrip(requestDto);
        logger.info("Admin successfully scheduled trip with ID: {}", createdTrip.getId());
        return new ResponseEntity<>(createdTrip, HttpStatus.CREATED); // 201 Created
    }

    // GET: Retrieve all Scheduled Trips
    @GetMapping
    public ResponseEntity<List<ScheduledTripResponseDto>> getAllScheduledTrips() {
        logger.info("Admin request received to get all scheduled trips.");
        List<ScheduledTripResponseDto> trips = scheduledTripService.getAllScheduledTrips();
        logger.info("Returning {} scheduled trips.", trips.size());
        return ResponseEntity.ok(trips); // 200 OK
    }

    // GET: Retrieve a single Scheduled Trip by ID
    @GetMapping("/{id}")
    public ResponseEntity<ScheduledTripResponseDto> getScheduledTripById(@PathVariable Long id) {
        logger.info("Admin request received to get scheduled trip with ID: {}", id);
        ScheduledTripResponseDto tripDto = scheduledTripService.getScheduledTripById(id);
        logger.info("Returning scheduled trip details for ID: {}", id);
        return ResponseEntity.ok(tripDto); // 200 OK
    }

    // PUT: Update an existing Scheduled Trip by ID
    @PutMapping("/{id}")
    public ResponseEntity<ScheduledTripResponseDto> updateScheduledTrip(@PathVariable Long id, @Valid @RequestBody ScheduledTripRequestDto requestDto) {
        // Note: Service layer currently primarily updates time/fare based on requestDto
        logger.info("Admin request received to update scheduled trip with ID: {}", id);
        ScheduledTripResponseDto updatedTrip = scheduledTripService.updateScheduledTrip(id, requestDto);
        logger.info("Admin successfully updated scheduled trip with ID: {}", id);
        return ResponseEntity.ok(updatedTrip); // 200 OK
    }

    // DELETE: Delete a Scheduled Trip by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteScheduledTrip(@PathVariable Long id) {
        logger.info("Admin request received to delete scheduled trip with ID: {}", id);
        scheduledTripService.deleteScheduledTrip(id);
        logger.info("Admin successfully deleted scheduled trip with ID: {}", id);
        return ResponseEntity.ok("Scheduled trip with ID " + id + " deleted successfully."); // 200 OK with message
    }
}