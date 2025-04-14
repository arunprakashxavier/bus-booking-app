// src/main/java/com/guvi/busapp/config/security/AuthEntryPointJwt.java
package com.guvi.busapp.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component // Make it a bean
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        logger.error("Unauthorized error: {}", authException.getMessage());
        // Send 401 Unauthorized response
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error: Unauthorized");
        // You could customize the response body here if needed (e.g., send JSON)
        // response.setContentType("application/json");
        // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // response.getOutputStream().println("{ \"error\": \"" + authException.getMessage() + "\" }");
    }
}