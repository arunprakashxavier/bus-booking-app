// src/main/java/com/guvi/busapp/dto/PassengerDto.java
package com.guvi.busapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PassengerDto {

    @NotBlank(message = "Passenger name cannot be blank")
    @Size(min = 2, max = 100, message = "Passenger name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Passenger age cannot be null")
    @Min(value = 1, message = "Passenger age must be at least 1")
    private Integer age;

    @NotBlank(message = "Passenger gender cannot be blank")
    private String gender;

    private String seatNumber;
}