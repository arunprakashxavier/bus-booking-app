// src/main/java/com/guvi/busapp/dto/ScheduledTripRequestDto.java
package com.guvi.busapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat; // For time parsing

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduledTripRequestDto {

    @NotNull(message = "Bus ID cannot be null")
    private Long busId;

    @NotNull(message = "Route ID cannot be null")
    private Long routeId;

    @NotNull(message = "Departure date cannot be null")
    @FutureOrPresent(message = "Departure date must be today or in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Standard YYYY-MM-DD
    private LocalDate departureDate;

    @NotNull(message = "Departure time cannot be null")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) // Standard HH:mm:ss or HH:mm
    private LocalTime departureTime;

    @NotNull(message = "Arrival time cannot be null")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime arrivalTime;

    @NotNull(message = "Fare cannot be null")
    @DecimalMin(value = "0.01", message = "Fare must be positive") // At least 0.01
    private BigDecimal fare;

    // availableSeats will be calculated based on the Bus totalSeats initially
    // and not directly set via this DTO during creation.

}