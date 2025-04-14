// src/main/java/com/guvi/busapp/dto/BookingRequestDto.java
package com.guvi.busapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BookingRequestDto {

    @NotNull(message = "Trip ID cannot be null")
    private Long tripId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<String> selectedSeats;

    // List of passengers for this booking
    @Valid // Triggers validation on nested PassengerDto objects
    @NotEmpty(message = "Passenger details must be provided")
    private List<PassengerDto> passengers;

}