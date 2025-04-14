// src/main/java/com/guvi/busapp/controller/AdminBusController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.BusDto;
import com.guvi.busapp.service.BusService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/buses") // Base path for admin bus operations
@PreAuthorize("hasRole('ADMIN')")   // Secure all endpoints in this controller for ADMIN role
public class AdminBusController {

    private static final Logger logger = LoggerFactory.getLogger(AdminBusController.class);

    private final BusService busService;

    @Autowired
    public AdminBusController(BusService busService) {
        this.busService = busService;
    }

    // POST: Create a new Bus
    @PostMapping
    public ResponseEntity<BusDto> createBus(@Valid @RequestBody BusDto busDto) {
        logger.info("Admin request received to create bus: {}", busDto.getBusNumber());
        BusDto createdBus = busService.createBus(busDto);
        logger.info("Admin successfully created bus with ID: {}", createdBus.getId());
        // Return 201 Created status with the created bus DTO in the body
        return new ResponseEntity<>(createdBus, HttpStatus.CREATED);
    }

    // GET: Retrieve all Buses
    @GetMapping
    public ResponseEntity<List<BusDto>> getAllBuses() {
        logger.info("Admin request received to get all buses.");
        List<BusDto> buses = busService.getAllBuses();
        logger.info("Returning {} buses.", buses.size());
        return ResponseEntity.ok(buses); // Return 200 OK with the list
    }

    // GET: Retrieve a single Bus by ID
    @GetMapping("/{id}")
    public ResponseEntity<BusDto> getBusById(@PathVariable Long id) {
        logger.info("Admin request received to get bus with ID: {}", id);
        BusDto busDto = busService.getBusById(id); // Service handles NotFoundException
        logger.info("Returning bus details for ID: {}", id);
        return ResponseEntity.ok(busDto); // Return 200 OK with the bus DTO
    }

    // PUT: Update an existing Bus by ID
    @PutMapping("/{id}")
    public ResponseEntity<BusDto> updateBus(@PathVariable Long id, @Valid @RequestBody BusDto busDto) {
        logger.info("Admin request received to update bus with ID: {}", id);
        BusDto updatedBus = busService.updateBus(id, busDto); // Service handles NotFoundException
        logger.info("Admin successfully updated bus with ID: {}", id);
        return ResponseEntity.ok(updatedBus); // Return 200 OK with updated bus DTO
    }

    // DELETE: Delete a Bus by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBus(@PathVariable Long id) {
        logger.info("Admin request received to delete bus with ID: {}", id);
        busService.deleteBus(id); // Service handles NotFoundException
        logger.info("Admin successfully deleted bus with ID: {}", id);
        // Return 200 OK or 204 No Content
        // return ResponseEntity.noContent().build(); // Standard for DELETE success with no body
        return ResponseEntity.ok("Bus with ID " + id + " deleted successfully."); // Or return a confirmation message
    }

}