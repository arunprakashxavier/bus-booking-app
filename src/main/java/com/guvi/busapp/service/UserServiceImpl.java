// src/main/java/com/guvi/busapp/service/UserServiceImpl.java
package com.guvi.busapp.service;

import com.guvi.busapp.dto.ChangePasswordDto; // Import needed DTO
import com.guvi.busapp.dto.RegisterDto;
import com.guvi.busapp.dto.UserProfileDto; // Import needed DTO
import com.guvi.busapp.exception.ResourceNotFoundException; // Import exception
import com.guvi.busapp.model.User;
import com.guvi.busapp.repository.UserRepository;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class); // Add logger

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- Helper Mapping Method ---
    private UserProfileDto mapUserToProfileDto(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail()); // Include email for display
        dto.setAge(user.getAge());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setGender(user.getGender());
        dto.setPhoneNumber(user.getPhoneNumber());
        return dto;
    }

    @Override
    @Transactional
    public User registerUser(RegisterDto registerDto) throws IllegalArgumentException {
        logger.info("Attempting to register user with email: {}", registerDto.getEmail());
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            logger.warn("Registration failed: Email {} already in use.", registerDto.getEmail());
            throw new IllegalArgumentException("Error: Email is already in use!");
        }
        if (!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            logger.warn("Registration failed: Passwords do not match for email {}.", registerDto.getEmail());
            throw new IllegalArgumentException("Error: Passwords do not match!");
        }

        User user = new User();
        user.setFirstName(registerDto.getFirstName());
        user.setLastName(registerDto.getLastName());
        user.setAge(registerDto.getAge());
        user.setDateOfBirth(registerDto.getDateOfBirth());
        user.setGender(registerDto.getGender());
        user.setPhoneNumber(registerDto.getPhoneNumber());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setRole(User.Role.ROLE_USER);

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully with ID: {} and Email: {}", savedUser.getId(), savedUser.getEmail());
        return savedUser;
    }

    // --- Profile Management Implementation ---

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String email) throws ResourceNotFoundException {
        logger.debug("Fetching profile for user: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Profile fetch failed: User not found with email {}", email);
                    return new ResourceNotFoundException("User", "email", email);
                });
        return mapUserToProfileDto(user);
    }

    @Override
    @Transactional
    public UserProfileDto updateUserProfile(String email, UserProfileDto userProfileDto) throws ResourceNotFoundException {
        logger.info("Attempting to update profile for user: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Profile update failed: User not found with email {}", email);
                    return new ResourceNotFoundException("User", "email", email);
                });

        // Update editable fields
        user.setFirstName(userProfileDto.getFirstName());
        user.setLastName(userProfileDto.getLastName());
        user.setAge(userProfileDto.getAge());
        user.setDateOfBirth(userProfileDto.getDateOfBirth());
        user.setGender(userProfileDto.getGender());
        user.setPhoneNumber(userProfileDto.getPhoneNumber());
        // Email is generally not updated via profile edit

        User updatedUser = userRepository.save(user);
        logger.info("Profile updated successfully for user: {}", email);
        return mapUserToProfileDto(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordDto changePasswordDto) throws ResourceNotFoundException, IllegalArgumentException {
        logger.info("Attempting to change password for user: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Password change failed: User not found with email {}", email);
                    return new ResourceNotFoundException("User", "email", email);
                });

        // 1. Verify current password
        if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
            logger.warn("Password change failed: Incorrect current password for user {}", email);
            throw new IllegalArgumentException("Incorrect current password.");
        }

        // 2. Check if new passwords match
        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmNewPassword())) {
            logger.warn("Password change failed: New passwords do not match for user {}", email);
            throw new IllegalArgumentException("New passwords do not match.");
        }

        // 3. Check if new password is same as old password (optional, but good practice)
        if (passwordEncoder.matches(changePasswordDto.getNewPassword(), user.getPassword())) {
            logger.warn("Password change failed: New password cannot be the same as the old password for user {}", email);
            throw new IllegalArgumentException("New password cannot be the same as the old password.");
        }


        // 4. Encode and update the password
        user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        userRepository.save(user);
        logger.info("Password changed successfully for user: {}", email);
    }
}