package com.capstone_project.elderly_platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "application.service-package")
@Getter
@Setter
public class ServicePackageProperties {
    
    private String allowedDurationHours;
    
    public List<Integer> getAllowedDurationHoursList() {
        if (allowedDurationHours == null || allowedDurationHours.isEmpty()) {
            return Arrays.asList(4, 8); // Default values
        }
        return Arrays.stream(allowedDurationHours.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}


