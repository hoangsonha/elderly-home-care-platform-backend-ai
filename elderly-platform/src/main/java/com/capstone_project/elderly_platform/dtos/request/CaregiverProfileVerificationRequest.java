package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CaregiverProfileVerificationRequest {
    @NotBlank(message = "Action is required. Must be 'APPROVE' or 'REJECT'")
    String action; // "APPROVE" or "REJECT"
    
    String rejectionReason; // Required if action is "REJECT", optional for "APPROVE"
}

