package com.capstone_project.elderly_platform.dtos.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID notificationId;
    private String title;
    private String body;
    private String notificationType;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private Map<String, Object> data;
    private String imageUrl;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}





