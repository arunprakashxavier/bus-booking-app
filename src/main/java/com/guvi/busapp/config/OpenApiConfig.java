// src/main/java/com/guvi/busapp/config/OpenApiConfig.java
package com.guvi.busapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth"; // Can be any name

        // Define API Info (Optional but recommended)
        Info apiInfo = new Info()
                .title("Bus Booking Application API")
                .version("1.0.0")
                .description("API documentation for the Bus Booking application.")
                .license(new License().name("Apache 2.0").url("http://springdoc.org"));

        // Define the JWT Bearer security scheme
        SecurityScheme securityScheme = new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP) // Type is HTTP
                .scheme("bearer") // Scheme is bearer
                .bearerFormat("JWT") // Format is JWT
                .description("Enter JWT token **_only_**"); // Optional description

        // Add the security scheme to components
        Components components = new Components()
                .addSecuritySchemes(securitySchemeName, securityScheme);

        // Add a global security requirement (makes all APIs require the scheme unless overridden)
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(securitySchemeName);

        // Build the OpenAPI definition
        return new OpenAPI()
                .info(apiInfo)
                .components(components)
                .addSecurityItem(securityRequirement); // Apply security globally
    }
}