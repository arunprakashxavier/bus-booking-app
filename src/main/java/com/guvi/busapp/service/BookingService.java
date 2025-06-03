// src/main/java/com/guvi/busapp/service/BookingService.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.BookingRequestDto;
import com.guvi.busapp.dto.BookingResponseDto;
import com.guvi.busapp.exception.ResourceNotFoundException;
import com.guvi.busapp.exception.SeatUnavailableException;
import com.guvi.busapp.model.Booking; // Import Booking model

import java.util.List;

/**
 * Service interface for managing Booking entities.
 */
public interface BookingService {

    BookingResponseDto createBooking(BookingRequestDto bookingRequest, String userEmail)
            throws ResourceNotFoundException, SeatUnavailableException, IllegalArgumentException;

    List<BookingResponseDto> getBookingsByUser(String userEmail)
            throws ResourceNotFoundException;

    List<BookingResponseDto> getAllBookings();

    /**
     * Retrieves a booking entity by its ID.
     * This method is intended for internal service use, for example, by PaymentService.
     *
     * @param bookingId The ID of the booking to retrieve.
     * @return The Booking entity.
     * @throws ResourceNotFoundException if the booking with the given ID is not found.
     */
    Booking getBookingEntityById(Long bookingId) throws ResourceNotFoundException;
}


