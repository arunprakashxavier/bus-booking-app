// src/main/java/com/guvi/busapp/dto/SeatLockRequestDto.java
package com.guvi.busapp.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SeatLockRequestDto {

    @NotNull(message = "Trip ID cannot be null")
    private Long tripId;

    @NotEmpty(message = "Seat numbers list cannot be empty")
    private List<String> seatNumbers;
}