package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for starting work (Check In)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartWorkRequest {
    
    @NotNull(message = "Care service ID is required")
    private UUID careServiceId;
}


