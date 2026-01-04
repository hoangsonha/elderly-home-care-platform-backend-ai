package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for ending work (Check Out)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndWorkRequest {
    
    @NotNull(message = "Care service ID is required")
    private UUID careServiceId;
}


