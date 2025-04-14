// src/main/java/com/guvi/busapp/dto/BusDto.java
package com.guvi.busapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
// import jakarta.validation.constraints.NotEmpty; // Optional validation for amenities
import lombok.Data;

import java.util.Set; // Import Set

@Data // Includes getters, setters, etc.
public class BusDto {

    private Long id; // Include ID for updates/display

    @NotBlank(message = "Bus number cannot be blank")
    private String busNumber;

    @NotBlank(message = "Operator name cannot be blank")
    private String operatorName;

    @NotBlank(message = "Bus type cannot be blank")
    private String busType;

    @NotNull(message = "Total seats cannot be null")
    @Min(value = 1, message = "Bus must have at least 1 seat")
    private Integer totalSeats;

    private Set<String> amenities; // Allow null or empty set

    private String seatLayout; // e.g., "A1,A2,_|B1,B2,_|..." or JSON representation
}