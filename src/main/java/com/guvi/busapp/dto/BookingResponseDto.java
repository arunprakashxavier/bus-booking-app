// src/main/java/com/guvi/busapp/dto/BookingResponseDto.java
package com.guvi.busapp.dto;

import com.guvi.busapp.model.Booking; // Import BookingStatus enum
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponseDto {

    private Long bookingId;
    private ScheduledTripResponseDto tripDetails; // Include details of the trip
    private List<PassengerDto> passengers; // Include details of booked passengers (with seat numbers)
    private Booking.BookingStatus status;
    private BigDecimal totalFare;
    private LocalDateTime bookingTime;
    private String userEmail; // Email of the user who booked
    private String userFullName; // Name of the user who booked

    // Add more fields as needed for confirmation/ticket
}