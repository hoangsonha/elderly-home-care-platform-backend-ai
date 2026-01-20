package com.capstone_project.elderly_platform.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageResponse {
    String messageId;
    String chatId;
    UUID senderId;
    String senderEmail;
    String senderName; // Full name từ CareSeekerProfile hoặc CaregiverProfile
    String senderAvatar; // Avatar URL từ Account
    UUID receiverId;
    String receiverEmail;
    String receiverName; // Full name từ CareSeekerProfile hoặc CaregiverProfile
    String receiverAvatar; // Avatar URL từ Account
    String content;
    LocalDateTime timestamp;
    Boolean read;
    LocalDateTime readAt;
}
