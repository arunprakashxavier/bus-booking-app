package com.guvi.busapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

// DTO for viewing and updating user profile (excluding password and roles)
@Data
public class UserProfileDto {

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    // Email is usually not editable or handled separately
    @Email(message = "Email should be valid")
    @Size(max = 100)
    private String email; // Display only

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be positive")
    private Integer age;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;
}