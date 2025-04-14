// src/main/java/com/guvi/busapp/service/BusService.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.BusDto;
import com.guvi.busapp.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Service interface for managing Bus entities.
 */
public interface BusService {

    /**
     * Creates a new bus.
     *
     * @param busDto DTO containing bus details.
     * @return The created BusDto.
     * @throws IllegalArgumentException if bus number already exists.
     */
    BusDto createBus(BusDto busDto);

    /**
     * Retrieves a list of all buses.
     *
     * @return List of BusDto.
     */
    List<BusDto> getAllBuses();

    /**
     * Retrieves a bus by its ID.
     *
     * @param id The ID of the bus.
     * @return The found BusDto.
     * @throws ResourceNotFoundException if bus with the given ID is not found.
     */
    BusDto getBusById(Long id); // Consider throwing ResourceNotFoundException

    /**
     * Updates an existing bus.
     *
     * @param id     The ID of the bus to update.
     * @param busDto DTO containing updated bus details.
     * @return The updated BusDto.
     * @throws ResourceNotFoundException if bus with the given ID is not found.
     * @throws IllegalArgumentException if updated bus number conflicts with another existing bus.
     */
    BusDto updateBus(Long id, BusDto busDto); // Consider ResourceNotFoundException

    /**
     * Deletes a bus by its ID.
     *
     * @param id The ID of the bus to delete.
     * @throws ResourceNotFoundException if bus with the given ID is not found.
     */
    void deleteBus(Long id); // Consider ResourceNotFoundException
}