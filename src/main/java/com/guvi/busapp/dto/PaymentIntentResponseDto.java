// src/main/java/com/guvi/busapp/dto/PaymentIntentResponseDto.java
package com.guvi.busapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponseDto {
    private String clientSecret; // The Stripe PaymentIntent client secret
    // You could also include publishableKey here if needed by frontend consistently
    // private String publishableKey;
}