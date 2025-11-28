package com.capstone_project.elderly_platform.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTokenRequest {
    
    @NotBlank(message = "FCM token is required")
    private String fcmToken;
    
    @NotBlank(message = "Device type is required")
    private String deviceType; // ANDROID, IOS, WEB
    
    private String deviceName;
}





