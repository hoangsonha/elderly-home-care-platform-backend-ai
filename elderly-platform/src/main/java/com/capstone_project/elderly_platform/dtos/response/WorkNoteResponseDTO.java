package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for WorkNote
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkNoteResponseDTO {
    String workNoteId;
    String content;
    String createdByAccountId;
    String createdByFullName;
    String createdAt;
    String updatedAt;
}
