package com.capstone_project.elderly_platform.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID accountId;
    private String email;
    private Boolean enabled;
    private Boolean nonLocked;
    private String avatarUrl;
    private String roleName;
    private String fullName; // From CareSeekerProfile or CaregiverProfile
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



