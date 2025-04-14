// src/main/java/com/guvi/busapp/dto/PaymentRequestDto.java
package com.guvi.busapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDto {
    @NotNull(message = "Booking ID is required to initiate payment")
    private Long bookingId;
}