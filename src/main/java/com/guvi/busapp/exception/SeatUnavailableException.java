// src/main/java/com/guvi/busapp/exception/SeatUnavailableException.java
package com.guvi.busapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT) // Return 409 Conflict status code
public class SeatUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SeatUnavailableException(String message) {
        super(message);
    }
}