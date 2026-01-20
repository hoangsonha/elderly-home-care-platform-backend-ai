package com.capstone_project.elderly_platform.services.externals.firebase;

import com.capstone_project.elderly_platform.dtos.request.ChatMessageRequest;
import com.capstone_project.elderly_platform.dtos.response.ChatMessageResponse;
import com.capstone_project.elderly_platform.dtos.response.ChatConversationResponse;

import java.util.List;
import java.util.UUID;

public interface FirestoreService {
    /**
     * Gửi tin nhắn vào Firestore
     */
    ChatMessageResponse sendMessage(ChatMessageRequest request, UUID senderId);
    
    /**
     * Lấy danh sách messages của 1 conversation
     */
    List<ChatMessageResponse> getMessages(String chatId, int limit);
    
    /**
     * Lấy danh sách conversations của user
     */
    List<ChatConversationResponse> getConversations(UUID userId);
    
    /**
     * Đánh dấu message đã đọc
     */
    void markAsRead(String messageId, UUID userId);
    
    /**
     * Tạo hoặc lấy chat ID giữa 2 users
     */
    String getOrCreateChatId(UUID userId1, UUID userId2);
}
