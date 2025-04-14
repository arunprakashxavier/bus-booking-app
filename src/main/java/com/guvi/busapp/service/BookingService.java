// src/main/java/com/guvi/busapp/service/BookingService.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.BookingRequestDto;
import com.guvi.busapp.dto.BookingResponseDto;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.exception.SeatUnavailableException;

import java.util.List;

/**
 * Service interface for managing Booking entities.
 */
public interface BookingService {

    BookingResponseDto createBooking(BookingRequestDto bookingRequest, String userEmail)
            throws ResourceNotFoundException, SeatUnavailableException, IllegalArgumentException;

    List<BookingResponseDto> getBookingsByUser(String userEmail)
            throws ResourceNotFoundException;

    // **** ADDED: Method for Admin to get all bookings ****
    /**
     * Retrieves all bookings in the system, typically ordered by booking time descending.
     * Intended for Admin use.
     *
     * @return A list of all BookingResponseDto objects.
     */
    List<BookingResponseDto> getAllBookings();

}