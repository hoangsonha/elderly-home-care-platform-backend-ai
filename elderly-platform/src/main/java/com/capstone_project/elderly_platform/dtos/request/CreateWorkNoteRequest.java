package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a work note
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkNoteRequest {
    
    @NotNull(message = "Work schedule ID is required")
    private UUID workScheduleId;
    
    @NotBlank(message = "Content is required")
    private String content;
}
