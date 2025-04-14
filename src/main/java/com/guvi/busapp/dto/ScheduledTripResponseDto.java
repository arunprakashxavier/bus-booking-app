// src/main/java/com/guvi/busapp/dto/ScheduledTripResponseDto.java
package com.guvi.busapp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduledTripResponseDto {

    private Long id;
    private BusDto bus; // Nested Bus details
    private RouteDto route; // Nested Route details
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private BigDecimal fare;
    private Integer availableSeats;

}