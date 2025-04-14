// src/main/java/com/guvi/busapp/config/SecurityConfig.java
package com.guvi.busapp.config;

import com.guvi.busapp.config.security.AuthEntryPointJwt;
import com.guvi.busapp.config.security.AuthTokenFilter;
import com.guvi.busapp.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Import HttpMethod
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Keep this for @PreAuthorize
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Define authorization rules - Order can matter: Specific rules first
                .authorizeHttpRequests(authz -> authz
                        // --- Publicly Accessible Paths ---
                        // Explicitly permit GET requests to specific pages needed for UI navigation
                        .requestMatchers(HttpMethod.GET,
                                "/", "/error", "/login", "/register", // Basic web pages
                                "/dashboard",                       // User pages
                                "/booking/trip/*/select-seats",
                                "/booking/confirm",
                                "/payment/**",
                                "/booking-history",
                                "/profile",
                                // Admin pages...
                                "/admin/dashboard",
                                "/admin/buses", "/admin/buses/add", "/admin/buses/edit/**",
                                "/admin/routes", "/admin/routes/add", "/admin/routes/edit/**",
                                "/admin/scheduled-trips", "/admin/scheduled-trips/add", "/admin/scheduled-trips/edit/**",
                                "/admin/bookings" // **** CORRECTED: Added /admin/bookings ****
                        ).permitAll() // Permit GET requests to these specific page URLs

                        // --- Allow static resources ---
                        .requestMatchers("/css/**", "/images/**", "/js/**", "/favicon.png", "/favicon.ico").permitAll()

                        // --- Allow specific API endpoints ---
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll() // Auth API (POST)
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll() // Swagger
                        .requestMatchers(HttpMethod.GET, "/api/trips/search").permitAll() // Public Search API
                        .requestMatchers(HttpMethod.POST, "/api/stripe/webhook").permitAll() // Stripe Webhook

                        // --- Protected API Routes (Require Roles/Authentication) ---
                        // Specific API endpoints requiring USER role
                        .requestMatchers(
                                HttpMethod.POST, "/api/payment/create-intent", "/api/booking/lock-seats", "/api/booking", "/api/user/change-password"
                        ).hasRole("USER")
                        .requestMatchers(
                                HttpMethod.GET, "/api/user/bookings", "/api/user/profile"
                        ).hasRole("USER")
                        .requestMatchers(
                                HttpMethod.PUT, "/api/user/profile"
                        ).hasRole("USER")
                        // Specific API endpoints requiring isAuthenticated (role checked by @PreAuthorize on method)
                        .requestMatchers(HttpMethod.GET, "/api/trips/*/seats", "/api/trips/*").authenticated()

                        // Admin APIs (/api/admin/**) secured by @PreAuthorize on methods/controllers
                        // Note: GET /api/admin/bookings is secured via AdminBookingController @PreAuthorize

                        // --- Default Rule ---
                        // Any other request (not matched above) must be authenticated
                        .anyRequest().authenticated()
                );

        // Add custom JWT filter before the standard username/password filter
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}