// src/main/java/com/guvi/busapp/dto/SeatLayoutDto.java
package com.guvi.busapp.dto;

import com.guvi.busapp.model.ScheduledTrip; // Import the nested enum
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // **** ADDED Import ****
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutDto {

    private String seatLayout;

    private Map<String, ScheduledTrip.SeatStatus> seatStatus;

    private Integer totalSeats;
    private BigDecimal tripFare;

}