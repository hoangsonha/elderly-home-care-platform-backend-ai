package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for toggling work task status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleWorkTaskRequest {
    
    @NotNull(message = "Work task ID is required")
    private UUID workTaskId;
}


