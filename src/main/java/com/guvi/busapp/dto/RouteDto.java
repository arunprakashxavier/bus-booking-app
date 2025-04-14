// src/main/java/com/guvi/busapp/dto/RouteDto.java
package com.guvi.busapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data // Lombok annotation for getters, setters, toString, etc.
public class RouteDto {

    private Long id; // Include ID for responses and potential updates

    @NotBlank(message = "Origin cannot be blank")
    @Size(min = 2, max = 100, message = "Origin must be between 2 and 100 characters")
    private String origin;

    @NotBlank(message = "Destination cannot be blank")
    @Size(min = 2, max = 100, message = "Destination must be between 2 and 100 characters")
    private String destination;

}