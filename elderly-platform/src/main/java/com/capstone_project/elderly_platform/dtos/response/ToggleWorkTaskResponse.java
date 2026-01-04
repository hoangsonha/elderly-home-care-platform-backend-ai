package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for toggling work task status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleWorkTaskResponse {
    private UUID workTaskId;
    private String status;
    private String message;
}


