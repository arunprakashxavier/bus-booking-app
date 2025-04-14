// src/main/java/com/guvi/busapp/service/RouteService.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.RouteDto;
import com.guvi.busapp.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Service interface for managing Route entities.
 */
public interface RouteService {

    /**
     * Creates a new route.
     *
     * @param routeDto DTO containing route details (origin, destination).
     * @return The created RouteDto.
     * @throws IllegalArgumentException if a route with the same origin and destination already exists.
     */
    RouteDto createRoute(RouteDto routeDto);

    /**
     * Retrieves a list of all routes.
     *
     * @return List of RouteDto.
     */
    List<RouteDto> getAllRoutes();

    /**
     * Retrieves a route by its ID.
     *
     * @param id The ID of the route.
     * @return The found RouteDto.
     * @throws ResourceNotFoundException if route with the given ID is not found.
     */
    RouteDto getRouteById(Long id);

    /**
     * Updates an existing route.
     *
     * @param id       The ID of the route to update.
     * @param routeDto DTO containing updated route details.
     * @return The updated RouteDto.
     * @throws ResourceNotFoundException if route with the given ID is not found.
     * @throws IllegalArgumentException if the updated origin/destination conflicts with another existing route.
     */
    RouteDto updateRoute(Long id, RouteDto routeDto);

    /**
     * Deletes a route by its ID.
     * Note: Consider implications if ScheduledTrips are using this route.
     * Should deletion be allowed, or should it cascade or be prevented?
     * For now, we allow deletion, but this might need refinement.
     *
     * @param id The ID of the route to delete.
     * @throws ResourceNotFoundException if route with the given ID is not found.
     */
    void deleteRoute(Long id);

    /**
     * Finds routes based on origin and destination.
     * (This might be useful for user search later).
     *
     * @param origin      The origin city/location.
     * @param destination The destination city/location.
     * @return A list of RouteDto matching the criteria.
     */
    List<RouteDto> findRoutesByOriginAndDestination(String origin, String destination);

}