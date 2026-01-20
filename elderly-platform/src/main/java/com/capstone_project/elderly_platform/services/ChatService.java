package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.ChatMessageRequest;
import com.capstone_project.elderly_platform.dtos.response.ChatMessageResponse;
import com.capstone_project.elderly_platform.dtos.response.ChatConversationResponse;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    ChatMessageResponse sendMessage(UUID senderId, ChatMessageRequest request);
    List<ChatMessageResponse> getMessages(String chatId, int limit);
    List<ChatConversationResponse> getConversations(UUID userId);
    void markAsRead(String messageId, UUID userId);
    String getOrCreateChatId(UUID senderId, UUID receiverId);
}
