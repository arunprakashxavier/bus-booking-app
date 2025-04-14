// src/main/java/com/guvi/busapp/controller/AdminRouteController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.RouteDto;
import com.guvi.busapp.service.RouteService;
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
@RequestMapping("/api/admin/routes") // Base path for admin route operations
@PreAuthorize("hasRole('ADMIN')")   // Secure all endpoints for ADMIN role
public class AdminRouteController {

    private static final Logger logger = LoggerFactory.getLogger(AdminRouteController.class);

    private final RouteService routeService;

    @Autowired
    public AdminRouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    // POST: Create a new Route
    @PostMapping
    public ResponseEntity<RouteDto> createRoute(@Valid @RequestBody RouteDto routeDto) {
        logger.info("Admin request received to create route from {} to {}", routeDto.getOrigin(), routeDto.getDestination());
        RouteDto createdRoute = routeService.createRoute(routeDto); // Service handles duplicate check
        logger.info("Admin successfully created route with ID: {}", createdRoute.getId());
        return new ResponseEntity<>(createdRoute, HttpStatus.CREATED); // 201 Created
    }

    // GET: Retrieve all Routes
    @GetMapping
    public ResponseEntity<List<RouteDto>> getAllRoutes() {
        logger.info("Admin request received to get all routes.");
        List<RouteDto> routes = routeService.getAllRoutes();
        logger.info("Returning {} routes.", routes.size());
        return ResponseEntity.ok(routes); // 200 OK
    }

    // GET: Retrieve a single Route by ID
    @GetMapping("/{id}")
    public ResponseEntity<RouteDto> getRouteById(@PathVariable Long id) {
        logger.info("Admin request received to get route with ID: {}", id);
        RouteDto routeDto = routeService.getRouteById(id); // Service handles NotFoundException
        logger.info("Returning route details for ID: {}", id);
        return ResponseEntity.ok(routeDto); // 200 OK
    }

    // PUT: Update an existing Route by ID
    @PutMapping("/{id}")
    public ResponseEntity<RouteDto> updateRoute(@PathVariable Long id, @Valid @RequestBody RouteDto routeDto) {
        logger.info("Admin request received to update route with ID: {}", id);
        RouteDto updatedRoute = routeService.updateRoute(id, routeDto); // Service handles NotFoundException and duplicate check
        logger.info("Admin successfully updated route with ID: {}", id);
        return ResponseEntity.ok(updatedRoute); // 200 OK
    }

    // DELETE: Delete a Route by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRoute(@PathVariable Long id) {
        logger.info("Admin request received to delete route with ID: {}", id);
        routeService.deleteRoute(id); // Service handles NotFoundException and potentially dependency check later
        logger.info("Admin successfully deleted route with ID: {}", id);
        return ResponseEntity.ok("Route with ID " + id + " deleted successfully."); // 200 OK with message
        // Alternatively: return ResponseEntity.noContent().build(); // 204 No Content
    }

}