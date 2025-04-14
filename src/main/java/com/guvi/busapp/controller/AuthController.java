// src/main/java/com/guvi/busapp/controller/AuthController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.JwtResponseDto;
import com.guvi.busapp.dto.LoginDto;
import com.guvi.busapp.dto.RegisterDto;
import com.guvi.busapp.model.User;
import com.guvi.busapp.repository.UserRepository; // Keep UserRepository import
import com.guvi.busapp.service.UserService;
import com.guvi.busapp.util.JwtUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*; // Import needed annotations

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth") // Base path for API authentication endpoints
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    // == Process Registration API ==
    @PostMapping("/register")
    // Now accepts JSON and returns ResponseEntity
    public ResponseEntity<?> processRegistrationApi(@Valid @RequestBody RegisterDto registerDto) {

        logger.info("API attempting to register user with email: {}", registerDto.getEmail());

        // Password match check (can also be handled by class-level DTO validator)
        if (registerDto.getPassword() != null && !registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            // Return a clear error response
            return ResponseEntity
                    .badRequest() // HTTP 400
                    .body("Error: Passwords do not match!");
        }

        // Try to register the user via the service
        try {
            User registeredUser = userService.registerUser(registerDto); // Service throws IllegalArgumentException for duplicates
            logger.info("API User registered successfully: {}", registerDto.getEmail());

            // Return a success response
            return ResponseEntity
                    .status(HttpStatus.CREATED) // HTTP 201
                    .body("User registered successfully!"); // Or return basic user info (ID, email)

        } catch (IllegalArgumentException e) {
            // Handle known registration errors (like duplicate email)
            logger.error("API Registration failed for email {}: {}", registerDto.getEmail(), e.getMessage());
            return ResponseEntity
                    .badRequest() // HTTP 400
                    .body(e.getMessage()); // Return the specific error message

        } catch (Exception e) {
            // Catch any other unexpected errors during registration
            logger.error("Unexpected error during API registration for email {}: {}", registerDto.getEmail(), e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR) // HTTP 500
                    .body("An unexpected error occurred during registration.");
        }
    }


    // == Process Login API (Returns JSON with JWT) - No Changes Needed Here ==
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginDto loginDto) {
        logger.info("Attempting to authenticate user with email: {}", loginDto.getEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            User userModel = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            Long userId = (userModel != null) ? userModel.getId() : null;
            String firstName = (userModel != null) ? userModel.getFirstName() : null;
            String lastName = (userModel != null) ? userModel.getLastName() : null;

            JwtResponseDto jwtResponse = new JwtResponseDto(jwt, userId, userDetails.getUsername(), firstName, lastName, roles);

            logger.info("User {} authenticated successfully. JWT issued.", loginDto.getEmail());
            return ResponseEntity.ok(jwtResponse);

        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user {}: {}", loginDto.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid credentials!");
        } catch (Exception e) {
            logger.error("Error during authentication for user {}: {}", loginDto.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: An internal error occurred during login.");
        }
    }
}