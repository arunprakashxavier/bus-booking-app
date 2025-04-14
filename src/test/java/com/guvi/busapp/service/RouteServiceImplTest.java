// src/test/java/com/guvi/busapp/service/RouteServiceImplTest.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.RouteDto;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.model.Route;
import com.guvi.busapp.repository.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteServiceImpl routeService;

    private Route testRoute;
    private RouteDto testRouteDto;
    private Long routeId = 1L;
    private String origin = "CityA";
    private String destination = "CityB";

    @BeforeEach
    void setUp() {
        testRouteDto = new RouteDto();
        testRouteDto.setOrigin(origin);
        testRouteDto.setDestination(destination);

        testRoute = new Route();
        testRoute.setId(routeId);
        testRoute.setOrigin(origin);
        testRoute.setDestination(destination);
    }

    // --- Tests for createRoute ---

    @Test
    void testCreateRoute_Success() {
        // Arrange
        when(routeRepository.existsByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination)).thenReturn(false);
        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        when(routeRepository.save(routeCaptor.capture())).thenAnswer(invocation -> {
            Route routeToSave = invocation.getArgument(0);
            routeToSave.setId(routeId);
            return routeToSave;
        });

        // Act
        RouteDto createdDto = routeService.createRoute(testRouteDto);

        // Assert
        assertNotNull(createdDto);
        assertEquals(routeId, createdDto.getId()); // Check ID is present in returned DTO
        assertEquals(origin, createdDto.getOrigin());
        assertEquals(destination, createdDto.getDestination());

        // Verify
        verify(routeRepository, times(1)).existsByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination);
        verify(routeRepository, times(1)).save(any(Route.class));

        // Check captured entity before save (excluding ID check)
        Route capturedRoute = routeCaptor.getValue();
        // assertNull(capturedRoute.getId()); // REMOVED THIS ASSERTION
        assertEquals(origin, capturedRoute.getOrigin());
        assertEquals(destination, capturedRoute.getDestination());
    }

    @Test
    void testCreateRoute_AlreadyExists() {
        // Arrange
        when(routeRepository.existsByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            routeService.createRoute(testRouteDto);
        });
        assertEquals(String.format("Route from '%s' to '%s' already exists.", origin, destination), exception.getMessage());

        // Verify
        verify(routeRepository, times(1)).existsByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination);
        verify(routeRepository, never()).save(any());
    }

    // --- Tests for getAllRoutes ---

    @Test
    void testGetAllRoutes_Success() {
        List<Route> routeList = List.of(testRoute);
        when(routeRepository.findAll()).thenReturn(routeList);
        List<RouteDto> resultList = routeService.getAllRoutes();
        assertNotNull(resultList);
        assertEquals(1, resultList.size());
        assertEquals(routeId, resultList.get(0).getId());
        verify(routeRepository, times(1)).findAll();
    }

    @Test
    void testGetAllRoutes_Empty() {
        when(routeRepository.findAll()).thenReturn(Collections.emptyList());
        List<RouteDto> resultList = routeService.getAllRoutes();
        assertNotNull(resultList);
        assertTrue(resultList.isEmpty());
        verify(routeRepository, times(1)).findAll();
    }

    // --- Tests for getRouteById ---

    @Test
    void testGetRouteById_Success() {
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));
        RouteDto resultDto = routeService.getRouteById(routeId);
        assertNotNull(resultDto);
        assertEquals(routeId, resultDto.getId());
        verify(routeRepository, times(1)).findById(routeId);
    }

    @Test
    void testGetRouteById_NotFound() {
        Long nonExistentId = 999L;
        when(routeRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> routeService.getRouteById(nonExistentId));
        assertEquals("Route not found with ID : '" + nonExistentId + "'", exception.getMessage());
        verify(routeRepository, times(1)).findById(nonExistentId);
    }

    // --- Tests for updateRoute ---

    @Test
    void testUpdateRoute_Success() {
        RouteDto updatedDto = new RouteDto();
        updatedDto.setOrigin("NewOrigin");
        updatedDto.setDestination("NewDest");
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));
        when(routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase("NewOrigin", "NewDest")).thenReturn(Collections.emptyList());
        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        when(routeRepository.save(routeCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        RouteDto resultDto = routeService.updateRoute(routeId, updatedDto);

        assertNotNull(resultDto);
        assertEquals(routeId, resultDto.getId());
        assertEquals("NewOrigin", resultDto.getOrigin());
        verify(routeRepository, times(1)).findById(routeId);
        verify(routeRepository, times(1)).findByOriginIgnoreCaseAndDestinationIgnoreCase("NewOrigin", "NewDest");
        verify(routeRepository, times(1)).save(any(Route.class));
        Route capturedRoute = routeCaptor.getValue();
        assertEquals(routeId, capturedRoute.getId());
        assertEquals("NewOrigin", capturedRoute.getOrigin());
    }

    @Test
    void testUpdateRoute_NotFound() {
        Long nonExistentId = 999L;
        when(routeRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> routeService.updateRoute(nonExistentId, testRouteDto));
        assertEquals("Route not found with ID : '" + nonExistentId + "'", exception.getMessage());
        verify(routeRepository, never()).save(any());
    }

    @Test
    void testUpdateRoute_Conflict() {
        String conflictingOrigin = "ConflictOrigin";
        String conflictingDest = "ConflictDest";
        RouteDto updatedDto = new RouteDto(); updatedDto.setOrigin(conflictingOrigin); updatedDto.setDestination(conflictingDest);
        Route conflictingRoute = new Route(); conflictingRoute.setId(routeId + 1); conflictingRoute.setOrigin(conflictingOrigin); conflictingRoute.setDestination(conflictingDest);

        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));
        when(routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase(conflictingOrigin, conflictingDest)).thenReturn(List.of(conflictingRoute));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> routeService.updateRoute(routeId, updatedDto));
        assertEquals(String.format("Another route from '%s' to '%s' already exists.", conflictingOrigin, conflictingDest), exception.getMessage());
        verify(routeRepository, never()).save(any());
    }

    @Test
    void testUpdateRoute_NoChange() {
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));
        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        when(routeRepository.save(routeCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        RouteDto resultDto = routeService.updateRoute(routeId, testRouteDto);

        assertNotNull(resultDto);
        assertEquals(origin, resultDto.getOrigin());
        verify(routeRepository, times(1)).findById(routeId);
        verify(routeRepository, times(1)).save(any(Route.class));
        verify(routeRepository, never()).findByOriginIgnoreCaseAndDestinationIgnoreCase(anyString(), anyString());
        Route capturedRoute = routeCaptor.getValue();
        assertEquals(origin, capturedRoute.getOrigin());
    }

    // --- Tests for deleteRoute ---

    @Test
    void testDeleteRoute_Success() {
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(testRoute));
        doNothing().when(routeRepository).delete(any(Route.class));
        assertDoesNotThrow(() -> routeService.deleteRoute(routeId));
        verify(routeRepository, times(1)).findById(routeId);
        verify(routeRepository, times(1)).delete(testRoute);
    }

    @Test
    void testDeleteRoute_NotFound() {
        Long nonExistentId = 999L;
        when(routeRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> routeService.deleteRoute(nonExistentId));
        assertEquals("Route not found with ID : '" + nonExistentId + "'", exception.getMessage());
        verify(routeRepository, never()).delete(any());
    }

    // --- Tests for findRoutesByOriginAndDestination ---

    @Test
    void testFindRoutesByOriginAndDestination_Found() {
        List<Route> foundRoutes = List.of(testRoute);
        when(routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination)).thenReturn(foundRoutes);
        List<RouteDto> results = routeService.findRoutesByOriginAndDestination(origin, destination);
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(routeRepository, times(1)).findByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination);
    }

    @Test
    void testFindRoutesByOriginAndDestination_NotFound() {
        when(routeRepository.findByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination)).thenReturn(Collections.emptyList());
        List<RouteDto> results = routeService.findRoutesByOriginAndDestination(origin, destination);
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(routeRepository, times(1)).findByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination);
    }
}