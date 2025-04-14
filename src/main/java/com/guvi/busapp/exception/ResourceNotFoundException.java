// src/main/java/com/guvi/busapp/exception/ResourceNotFoundException.java
package com.guvi.busapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for cases where a requested resource is not found.
 * Annotated with @ResponseStatus(HttpStatus.NOT_FOUND) so Spring Boot
 * automatically returns a 404 status code when this exception is thrown
 * and not caught elsewhere (e.g., by a @ControllerAdvice).
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND) // Sets the HTTP status code
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L; // Recommended for Serializable classes

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        // Construct the error message
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}