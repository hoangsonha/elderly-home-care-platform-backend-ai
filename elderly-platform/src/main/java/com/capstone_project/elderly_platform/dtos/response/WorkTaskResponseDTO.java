package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for WorkTask
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTaskResponseDTO {
    String workTaskId;
    String name;
    String description;
    String status;
    String completedAt;
}


