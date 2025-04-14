// src/main/java/com/guvi/busapp/service/RouteServiceImpl.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.RouteDto;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.model.Route;
import com.guvi.busapp.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;

    @Autowired
    public RouteServiceImpl(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    // --- Helper Mapping Methods ---

    private RouteDto mapToDto(Route route) {
        RouteDto dto = new RouteDto();
        dto.setId(route.getId());
        dto.setOrigin(route.getOrigin());
        dto.setDestination(route.getDestination());

        return dto;
    }

    private Route mapToEntity(RouteDto dto) {
        Route route = new Route();
        route.setOrigin(dto.getOrigin());
        route.setDestination(dto.getDestination());
        return route;
    }

    // --- Service Method Implementations ---

    @Override
    @Transactional
    public RouteDto createRoute(RouteDto routeDto) {
        // Validate: Check if a route with the same origin/destination already exists (ignore case)
        if (routeRepository.existsByOriginIgnoreCaseAndDestinationIgnoreCase(routeDto.getOrigin(), routeDto.getDestination())) {
            throw new IllegalArgumentException(
                    String.format("Route from '%s' to '%s' already exists.", routeDto.getOrigin(), routeDto.getDestination())
            );
        }

        Route route = mapToEntity(routeDto);
        Route savedRoute = routeRepository.save(route);
        return mapToDto(savedRoute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteDto> getAllRoutes() {
        List<Route> routes = routeRepository.findAll();
        return routes.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RouteDto getRouteById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "ID", id));
        return mapToDto(route);
    }

    @Override
    @Transactional
    public RouteDto updateRoute(Long id, RouteDto routeDto) {
        // 1. Find the existing route
        Route existingRoute = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "ID", id));

        // 2. Check if origin/destination is changing and if it conflicts with another route
        boolean originChanged = !existingRoute.getOrigin().equalsIgnoreCase(routeDto.getOrigin());
        boolean destinationChanged = !existingRoute.getDestination().equalsIgnoreCase(routeDto.getDestination());

        if (originChanged || destinationChanged) {
            // Check if the new combination already exists for a *different* route ID
            List<Route> conflictingRoutes = routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase(
                    routeDto.getOrigin(), routeDto.getDestination()
            );
            // If conflicts exist and none of them are the route we are currently updating
            if (!conflictingRoutes.isEmpty() && conflictingRoutes.stream().noneMatch(r -> r.getId().equals(id))) {
                throw new IllegalArgumentException(
                        String.format("Another route from '%s' to '%s' already exists.", routeDto.getOrigin(), routeDto.getDestination())
                );
            }
        }

        // 3. Update fields
        existingRoute.setOrigin(routeDto.getOrigin());
        existingRoute.setDestination(routeDto.getDestination());

        // 4. Save updated route
        Route updatedRoute = routeRepository.save(existingRoute);
        return mapToDto(updatedRoute);
    }

    @Override
    @Transactional
    public void deleteRoute(Long id) {
        // Check if route exists before deleting
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "ID", id));

        routeRepository.delete(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteDto> findRoutesByOriginAndDestination(String origin, String destination) {
        List<Route> routes = routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination);
        return routes.stream().map(this::mapToDto).collect(Collectors.toList());
    }
}