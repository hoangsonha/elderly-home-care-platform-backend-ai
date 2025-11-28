package com.capstone_project.elderly_platform.dtos.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenResponse {
    private UUID deviceTokenId;
    private String deviceType;
    private String deviceName;
    private Boolean isActive;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}





