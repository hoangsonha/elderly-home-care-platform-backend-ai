package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for starting work
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartWorkResponse {
    private UUID careServiceId;
    private String status;
    private String checkInImageUrl;
    private String message;
}


