// src/main/java/com/guvi/busapp/service/UserService.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.ChangePasswordDto; // Import needed DTO
import com.guvi.busapp.dto.RegisterDto;
import com.guvi.busapp.dto.UserProfileDto; // Import needed DTO
import com.guvi.busapp.exception.ResourceNotFoundException; // Import needed exception
import com.guvi.busapp.model.User;

public interface UserService {
    /**
     * Registers a new user in the system.
     *
     * @param registerDto DTO containing registration details.
     * @return The newly created User object.
     * @throws IllegalArgumentException if username or email already exists or passwords don't match.
     */
    User registerUser(RegisterDto registerDto) throws IllegalArgumentException;

    /**
     * Retrieves the profile information for a user.
     *
     * @param email The email of the user whose profile is to be retrieved.
     * @return UserProfileDto containing the user's details (excluding password).
     * @throws ResourceNotFoundException if the user with the given email is not found.
     */
    UserProfileDto getUserProfile(String email) throws ResourceNotFoundException;

    /**
     * Updates the profile information for a user.
     *
     * @param email          The email of the user whose profile is to be updated.
     * @param userProfileDto DTO containing the updated profile information.
     * @return The updated UserProfileDto.
     * @throws ResourceNotFoundException if the user with the given email is not found.
     */
    UserProfileDto updateUserProfile(String email, UserProfileDto userProfileDto) throws ResourceNotFoundException;

    /**
     * Changes the password for a user.
     *
     * @param email             The email of the user changing the password.
     * @param changePasswordDto DTO containing current and new password information.
     * @throws ResourceNotFoundException if the user with the given email is not found.
     * @throws IllegalArgumentException  if the current password is incorrect or new passwords don't match.
     */
    void changePassword(String email, ChangePasswordDto changePasswordDto) throws ResourceNotFoundException, IllegalArgumentException;

}