// src/main/java/com/guvi/busapp/controller/AuthController.java
package com.guvi.busapp.controller;

import com.guvi.busapp.dto.JwtResponseDto;
import com.guvi.busapp.dto.LoginDto;
import com.guvi.busapp.dto.RegisterDto;
import com.guvi.busapp.model.CustomUserDetails;
import com.guvi.busapp.model.User; // Your User entity for registration response
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Autowired
    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<?> processRegistrationApi(@Valid @RequestBody RegisterDto registerDto) {
        logger.info("API attempting to register user with email: {}", registerDto.getEmail());

        if (registerDto.getPassword() != null && !registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Passwords do not match!");
        }

        try {
            // UserService handles registration and checks for duplicates
            User registeredUser = userService.registerUser(registerDto);
            logger.info("API User registered successfully: {}", registerDto.getEmail());
            // Consider returning a DTO instead of the full User entity, or just a success message.
            // For now, returning a simple message.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("User registered successfully!");

        } catch (IllegalArgumentException e) {
            logger.error("API Registration failed for email {}: {}", registerDto.getEmail(), e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during API registration for email {}: {}", registerDto.getEmail(), e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during registration.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginDto loginDto) {
        logger.info("Attempting to authenticate user with email: {}", loginDto.getEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);


            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

            List<String> roles = customUserDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Get details from CustomUserDetails
            Long userId = customUserDetails.getId();
            String email = customUserDetails.getUsername(); // getUsername() from UserDetails returns the email
            String firstName = customUserDetails.getFirstName();
            String lastName = customUserDetails.getLastName();

            JwtResponseDto jwtResponse = new JwtResponseDto(jwt, userId, email, firstName, lastName, roles);

            logger.info("User {} authenticated successfully. JWT issued.", loginDto.getEmail());
            return ResponseEntity.ok(jwtResponse);

        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user {}: {}", loginDto.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid credentials!");
        } catch (ClassCastException e) {
            logger.error("Error casting Principal to CustomUserDetails. Check UserDetailsService implementation.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: Authentication configuration issue.");
        }
        catch (Exception e) {
            logger.error("Error during authentication for user {}: {}", loginDto.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: An internal error occurred during login.");
        }
    }
}
