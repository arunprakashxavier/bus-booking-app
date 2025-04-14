// src/main/java/com/guvi/busapp/service/ScheduledTripService.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.ScheduledTripRequestDto;
import com.guvi.busapp.dto.ScheduledTripResponseDto;
import com.guvi.busapp.dto.SeatLayoutDto;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.exception.SeatUnavailableException; // **** ADDED Import ****

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing ScheduledTrip entities.
 */
public interface ScheduledTripService {

    // --- Existing Method Signatures ---
    ScheduledTripResponseDto scheduleTrip(ScheduledTripRequestDto requestDto);
    List<ScheduledTripResponseDto> getAllScheduledTrips();
    ScheduledTripResponseDto getScheduledTripById(Long id);
    ScheduledTripResponseDto updateScheduledTrip(Long id, ScheduledTripRequestDto requestDto);
    void deleteScheduledTrip(Long id);
    List<ScheduledTripResponseDto> findAvailableTrips(String origin, String destination, LocalDate date);
    SeatLayoutDto getSeatLayoutForTrip(Long tripId);

    //  Method Signature for Seat Locking
    /**
     * Attempts to lock the specified seats for a given trip for a specific user.
     * Uses pessimistic locking to ensure atomicity.
     *
     * @param tripId      The ID of the scheduled trip.
     * @param seatNumbers The list of seat numbers (e.g., "1", "A5") to lock.
     * @param userId      The ID of the user requesting the lock.
     * @return true if seats were successfully locked.
     * @throws ResourceNotFoundException if the trip is not found.
     * @throws SeatUnavailableException if one or more requested seats are not available (booked, locked, or invalid).
     */
    boolean lockSeats(Long tripId, List<String> seatNumbers, Long userId) throws SeatUnavailableException;

}