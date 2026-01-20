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
public class ChatConversationResponse {
    String chatId;
    UUID participantId;
    String participantEmail;
    String participantName;
    String participantAvatar; // Avatar URL từ Account
    String lastMessage;
    LocalDateTime lastMessageTime;
    Integer unreadCount;
    LocalDateTime createdAt;
}
