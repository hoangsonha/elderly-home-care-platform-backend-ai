package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.dtos.request.ChatMessageRequest;
import com.capstone_project.elderly_platform.dtos.response.ChatMessageResponse;
import com.capstone_project.elderly_platform.dtos.response.ChatConversationResponse;
import com.capstone_project.elderly_platform.enums.EnumNotificationType;
import com.capstone_project.elderly_platform.services.externals.firebase.FirestoreService;
import com.capstone_project.elderly_platform.services.externals.firebase.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    
    private final FirestoreService firestoreService;
    private final PushNotificationService pushNotificationService;
    
    @Override
    public ChatMessageResponse sendMessage(UUID senderId, ChatMessageRequest request) {
        // Send message to Firestore (real-time)
        ChatMessageResponse response = firestoreService.sendMessage(request, senderId);
        
        // Send push notification to receiver
        try {
            String senderName = response.getSenderName() != null 
                    ? response.getSenderName() 
                    : "Ai đó";
            
            String notificationTitle = "Tin nhắn mới";
            String notificationBody = senderName + " đã gửi cho bạn 1 tin nhắn";
            
            // Prepare notification data
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("chatId", response.getChatId());
            notificationData.put("messageId", response.getMessageId());
            notificationData.put("senderId", response.getSenderId().toString());
            notificationData.put("senderName", senderName);
            notificationData.put("senderAvatar", response.getSenderAvatar());
            notificationData.put("content", response.getContent());
            
            // Send notification
            // Note: Không cần relatedEntityType và relatedEntityId vì tất cả thông tin đã có trong data (chatId, messageId, etc)
            pushNotificationService.sendNotification(
                    response.getReceiverId(),
                    response.getSenderId(),
                    notificationTitle,
                    notificationBody,
                    EnumNotificationType.CHAT_MESSAGE,
                    null, // relatedEntityType - không cần
                    null, // relatedEntityId - không cần
                    notificationData,
                    response.getSenderAvatar() // Image URL (sender avatar)
            );
            
            log.info("Push notification sent to receiver: {} - {}", 
                    response.getReceiverId(), notificationBody);
                    
        } catch (Exception e) {
            // Non-critical: notification failure should not break message sending
            log.warn("Failed to send push notification (non-critical): {}", e.getMessage());
        }
        
        return response;
    }
    
    @Override
    public List<ChatMessageResponse> getMessages(String chatId, int limit) {
        return firestoreService.getMessages(chatId, limit);
    }
    
    @Override
    public List<ChatConversationResponse> getConversations(UUID userId) {
        return firestoreService.getConversations(userId);
    }
    
    @Override
    public void markAsRead(String messageId, UUID userId) {
        firestoreService.markAsRead(messageId, userId);
    }
    
    @Override
    public String getOrCreateChatId(UUID senderId, UUID receiverId) {
        return firestoreService.getOrCreateChatId(senderId, receiverId);
    }
}
