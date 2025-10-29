package com.capstone_project.elderly_platform.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfiguration {

    @Value("${cors.allowed-origins:}")
    private String allowedOriginsConfig;

    @Value("${cors.allow-all-origins:false}")
    private boolean allowAllOrigins;

    @Bean
    public CorsFilter corsFilter() {
        org.springframework.web.cors.CorsConfiguration corsConfiguration = new org.springframework.web.cors.CorsConfiguration();

        // Configure allowed origins
        if (allowAllOrigins) {
            // Allow all origins (for development/public APIs)
            // Note: Cannot use setAllowCredentials(true) with allowedOriginPattern("*")
            corsConfiguration.addAllowedOriginPattern("*");
            corsConfiguration.setAllowCredentials(false);
        } else {
            // Default allowed origins
            List<String> allowedOrigins = new ArrayList<>(Arrays.asList(
                    "http://localhost",
                    "http://localhost:5173",
                    "http://localhost:3000",
                    "http://localhost:8080",
                    "http://127.0.0.1",
                    "http://127.0.0.1:5173",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:8080",
                    "http://35.239.101.150:8080",
                    "https://toireview.net"));

            // Add origins from environment variable if provided
            if (allowedOriginsConfig != null && !allowedOriginsConfig.trim().isEmpty()) {
                String[] additionalOrigins = allowedOriginsConfig.split(",");
                for (String origin : additionalOrigins) {
                    String trimmed = origin.trim();
                    if (!trimmed.isEmpty()) {
                        allowedOrigins.add(trimmed);
                    }
                }
            }

            corsConfiguration.setAllowedOrigins(allowedOrigins);
            // Allow credentials only when using specific origins
            corsConfiguration.setAllowCredentials(true);
        }

        // Configure allowed methods
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

        // Configure allowed headers
        corsConfiguration.setAllowedHeaders(List.of("*"));

        // Configure exposed headers
        corsConfiguration.setExposedHeaders(Arrays.asList(
                "Content-Disposition",
                "Authorization",
                "X-Total-Count",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"));

        // Cache preflight response for 1 hour
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(source);
    }
}