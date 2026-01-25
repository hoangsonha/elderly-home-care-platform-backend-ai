package com.capstone_project.elderly_platform.services.externals.firebase;

import com.capstone_project.elderly_platform.dtos.request.ChatMessageRequest;
import com.capstone_project.elderly_platform.dtos.response.ChatMessageResponse;
import com.capstone_project.elderly_platform.dtos.response.ChatConversationResponse;
import com.capstone_project.elderly_platform.exceptions.ElementNotFoundException;
import com.capstone_project.elderly_platform.repositories.AccountRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FirestoreServiceImpl implements FirestoreService {
    
    private final AccountRepository accountRepository;
    private final Firestore firestore; // Injected from FirebaseChatConfiguration
    
    private static final String MESSAGES_COLLECTION = "messages";
    private static final String CHATS_COLLECTION = "chats";
    
    @Override
    public ChatMessageResponse sendMessage(ChatMessageRequest request, UUID senderId) {
        try {
            log.info("SendMessage called - senderId: {}, receiverId: {}", senderId, request.getReceiverId());
            
            // Get sender and receiver info (chỉ lấy account chưa bị xóa)
            var senderOpt = accountRepository.findByAccountIdAndDeletedIsFalse(senderId);
            if (senderOpt.isEmpty()) {
                log.error("Sender account not found or deleted: {}", senderId);
                throw new ElementNotFoundException("Sender account not found");
            }
            var sender = senderOpt.get();
            
            var receiverOpt = accountRepository.findByAccountIdAndDeletedIsFalse(request.getReceiverId());
            if (receiverOpt.isEmpty()) {
                log.error("Receiver account not found or deleted: {}", request.getReceiverId());
                throw new ElementNotFoundException("Receiver account not found");
            }
            var receiver = receiverOpt.get();
            
            // Get or create chat ID
            log.info("Send Message - senderId: {} (toString: {}), receiverId: {} (toString: {})", 
                    senderId, senderId.toString(), request.getReceiverId(), request.getReceiverId().toString());
            String chatId = getOrCreateChatId(senderId, request.getReceiverId());
            log.info("Send Message - Generated chatId: {}", chatId);
            
            // Create message document
            String messageId = UUID.randomUUID().toString();
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("messageId", messageId);
            messageData.put("chatId", chatId);
            messageData.put("senderId", senderId.toString());
            messageData.put("senderEmail", sender.getEmail());
            messageData.put("receiverId", request.getReceiverId().toString());
            messageData.put("receiverEmail", receiver.getEmail());
            messageData.put("content", request.getContent());
            messageData.put("timestamp", FieldValue.serverTimestamp());
            messageData.put("read", false);
            
            // Write to Firestore
            DocumentReference messageRef = firestore.collection(MESSAGES_COLLECTION).document(messageId);
            ApiFuture<WriteResult> future = messageRef.set(messageData);
            future.get(); // Wait for write to complete
            
            log.info("Message sent to Firestore: messageId={}, chatId={}, senderId={}, receiverId={}", 
                    messageId, chatId, senderId, request.getReceiverId());
            
            // Update chat's last message
            updateChatLastMessage(chatId, messageId, request.getContent());
            
            // Get sender and receiver info (name, avatar)
            UserInfo senderInfo = getUserInfo(senderId);
            UserInfo receiverInfo = getUserInfo(request.getReceiverId());
            
            // Build response
            return ChatMessageResponse.builder()
                    .messageId(messageId)
                    .chatId(chatId)
                    .senderId(senderId)
                    .senderEmail(sender.getEmail())
                    .senderName(senderInfo.name)
                    .senderAvatar(senderInfo.avatar)
                    .receiverId(request.getReceiverId())
                    .receiverEmail(receiver.getEmail())
                    .receiverName(receiverInfo.name)
                    .receiverAvatar(receiverInfo.avatar)
                    .content(request.getContent())
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error sending message to Firestore: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message", e);
        }
    }
    
    @Override
    public List<ChatMessageResponse> getMessages(String chatId, int limit) {
        try {
            log.info("Getting messages for chatId: {}, limit: {}", chatId, limit);
            
            Query query = firestore.collection(MESSAGES_COLLECTION)
                    .whereEqualTo("chatId", chatId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot snapshot = future.get();
            
            log.info("Query executed. Snapshot size: {}, empty: {}", 
                    snapshot.size(), snapshot.isEmpty());
            
            // Debug: Log all documents found
            if (snapshot.isEmpty()) {
                log.warn("No messages found for chatId: {}. Checking if messages exist in collection...", chatId);
                
                // Try to get all messages without filter to debug
                Query allMessagesQuery = firestore.collection(MESSAGES_COLLECTION).limit(10);
                QuerySnapshot allSnapshot = allMessagesQuery.get().get();
                log.info("Total messages in collection (first 10): {}", allSnapshot.size());
                allSnapshot.forEach(doc -> {
                    Map<String, Object> data = doc.getData();
                    log.info("Sample message - docId: {}, chatId: {}, content: {}", 
                            doc.getId(), data.get("chatId"), data.get("content"));
                });
            } else {
                snapshot.forEach(doc -> {
                    Map<String, Object> data = doc.getData();
                    log.info("Found message - docId: {}, chatId: {}, content: {}, timestamp: {}", 
                            doc.getId(), data.get("chatId"), data.get("content"), data.get("timestamp"));
                });
            }
            
            return snapshot.getDocuments().stream()
                    .map(this::documentToMessageResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting messages from Firestore: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get messages", e);
        }
    }
    
    @Override
    public List<ChatConversationResponse> getConversations(UUID userId) {
        try {
            // Get all chats where user is participant
            Query query = firestore.collection(CHATS_COLLECTION)
                    .whereArrayContains("participants", userId.toString());
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot snapshot = future.get();
            
            return snapshot.getDocuments().stream()
                    .map(doc -> documentToConversationResponse(doc, userId))
                    .sorted(Comparator.comparing(ChatConversationResponse::getLastMessageTime, 
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting conversations from Firestore: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get conversations", e);
        }
    }
    
    @Override
    public void markAsRead(String messageId, UUID userId) {
        try {
            // Get the message document first to get chatId, timestamp, and receiverId
            DocumentReference messageRef = firestore.collection(MESSAGES_COLLECTION).document(messageId);
            ApiFuture<DocumentSnapshot> messageFuture = messageRef.get();
            DocumentSnapshot messageDoc = messageFuture.get();
            
            if (!messageDoc.exists()) {
                throw new RuntimeException("Message not found: " + messageId);
            }
            
            Map<String, Object> messageData = messageDoc.getData();
            if (messageData == null) {
                throw new RuntimeException("Message data is null: " + messageId);
            }
            
            String chatId = (String) messageData.get("chatId");
            String receiverId = (String) messageData.get("receiverId");
            Timestamp messageTimestamp = (Timestamp) messageData.get("timestamp");
            
            if (chatId == null || receiverId == null) {
                throw new RuntimeException("Message missing required fields (chatId or receiverId): " + messageId);
            }
            
            // Verify that the userId matches the receiverId
            if (!receiverId.equals(userId.toString())) {
                throw new RuntimeException("User is not the receiver of this message");
            }
            
            // Mark the current message as read
            Map<String, Object> updates = new HashMap<>();
            updates.put("read", true);
            updates.put("readAt", FieldValue.serverTimestamp());
            
            ApiFuture<WriteResult> future = messageRef.update(updates);
            future.get();
            
            log.info("Message marked as read: messageId={}, userId={}", messageId, userId);
            
            // Find and mark all older unread messages in the same chat as read
            if (messageTimestamp != null) {
                Query olderMessagesQuery = firestore.collection(MESSAGES_COLLECTION)
                        .whereEqualTo("chatId", chatId)
                        .whereEqualTo("receiverId", receiverId)
                        .whereEqualTo("read", false)
                        .whereLessThan("timestamp", messageTimestamp);
                
                ApiFuture<QuerySnapshot> olderMessagesFuture = olderMessagesQuery.get();
                QuerySnapshot olderMessagesSnapshot = olderMessagesFuture.get();
                
                if (!olderMessagesSnapshot.isEmpty()) {
                    // Use batch write to update all older messages at once
                    WriteBatch batch = firestore.batch();
                    int count = 0;
                    
                    for (QueryDocumentSnapshot doc : olderMessagesSnapshot.getDocuments()) {
                        Map<String, Object> olderUpdates = new HashMap<>();
                        olderUpdates.put("read", true);
                        olderUpdates.put("readAt", FieldValue.serverTimestamp());
                        batch.update(doc.getReference(), olderUpdates);
                        count++;
                    }
                    
                    // Commit the batch
                    ApiFuture<List<WriteResult>> batchFuture = batch.commit();
                    batchFuture.get();
                    
                    log.info("Automatically marked {} older messages as read in chatId={}", count, chatId);
                }
            }
            
        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to mark message as read", e);
        }
    }
    
    @Override
    public String getOrCreateChatId(UUID userId1, UUID userId2) {
        try {
            // Log input để debug
            log.info("getOrCreateChatId called - userId1: {} (type: {}), userId2: {} (type: {})", 
                    userId1, userId1.getClass().getSimpleName(), 
                    userId2, userId2.getClass().getSimpleName());
            
            // Normalize UUIDs để đảm bảo format nhất quán (lowercase, trim)
            String id1 = userId1.toString().toLowerCase().trim();
            String id2 = userId2.toString().toLowerCase().trim();
            
            log.info("Normalized - id1: {}, id2: {}", id1, id2);
            
            // Create consistent chat ID (sorted UUIDs)
            String[] userIds = {id1, id2};
            log.info("Before sort - userIds[0]: {}, userIds[1]: {}", userIds[0], userIds[1]);
            
            Arrays.sort(userIds);
            log.info("After sort - userIds[0]: {}, userIds[1]: {}", userIds[0], userIds[1]);
            
            String chatId = userIds[0] + "_" + userIds[1];
            log.info("Generated chatId: {}", chatId);
            
            // Check if chat exists
            DocumentReference chatRef = firestore.collection(CHATS_COLLECTION).document(chatId);
            ApiFuture<DocumentSnapshot> future = chatRef.get();
            DocumentSnapshot document = future.get();
            
            if (!document.exists()) {
                // Create new chat
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("participants", Arrays.asList(userId1.toString(), userId2.toString()));
                chatData.put("createdAt", FieldValue.serverTimestamp());
                chatData.put("updatedAt", FieldValue.serverTimestamp());
                
                ApiFuture<WriteResult> writeFuture = chatRef.set(chatData);
                writeFuture.get();
                
                log.info("✅ Created new chat: chatId={}, participants=[{}, {}]", chatId, userId1, userId2);
            } else {
                log.info("✅ Chat already exists: chatId={}", chatId);
            }
            
            log.info("Returning chatId: {}", chatId);
            return chatId;
            
        } catch (Exception e) {
            log.error("❌ Error getting or creating chat: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get or create chat", e);
        }
    }
    
    private void updateChatLastMessage(String chatId, String messageId, String content) {
        try {
            DocumentReference chatRef = firestore.collection(CHATS_COLLECTION).document(chatId);
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", content);
            updates.put("lastMessageId", messageId);
            updates.put("lastMessageTime", FieldValue.serverTimestamp());
            updates.put("updatedAt", FieldValue.serverTimestamp());
            
            ApiFuture<WriteResult> future = chatRef.update(updates);
            future.get();
            
        } catch (Exception e) {
            log.error("Error updating chat last message: {}", e.getMessage(), e);
        }
    }
    
    private ChatMessageResponse documentToMessageResponse(DocumentSnapshot doc) {
        Map<String, Object> data = doc.getData();
        if (data == null) return null;
        
        Timestamp timestamp = (Timestamp) data.get("timestamp");
        LocalDateTime localDateTime = timestamp != null 
                ? LocalDateTime.ofInstant(timestamp.toDate().toInstant(), ZoneId.systemDefault())
                : LocalDateTime.now();
        
        Timestamp readAt = (Timestamp) data.get("readAt");
        LocalDateTime readAtLocal = readAt != null
                ? LocalDateTime.ofInstant(readAt.toDate().toInstant(), ZoneId.systemDefault())
                : null;
        
        // Get sender info
        UUID senderId = UUID.fromString((String) data.get("senderId"));
        UserInfo senderInfo = getUserInfo(senderId);
        
        // Get receiver info
        UUID receiverId = UUID.fromString((String) data.get("receiverId"));
        UserInfo receiverInfo = getUserInfo(receiverId);
        
        return ChatMessageResponse.builder()
                .messageId((String) data.get("messageId"))
                .chatId((String) data.get("chatId"))
                .senderId(senderId)
                .senderEmail((String) data.get("senderEmail"))
                .senderName(senderInfo.name)
                .senderAvatar(senderInfo.avatar)
                .receiverId(receiverId)
                .receiverEmail((String) data.get("receiverEmail"))
                .receiverName(receiverInfo.name)
                .receiverAvatar(receiverInfo.avatar)
                .content((String) data.get("content"))
                .timestamp(localDateTime)
                .read((Boolean) data.getOrDefault("read", false))
                .readAt(readAtLocal)
                .build();
    }
    
    private ChatConversationResponse documentToConversationResponse(DocumentSnapshot doc, UUID currentUserId) {
        Map<String, Object> data = doc.getData();
        if (data == null) return null;
        
        @SuppressWarnings("unchecked")
        List<String> participants = (List<String>) data.get("participants");
        String otherParticipantId = participants.stream()
                .filter(id -> !id.equals(currentUserId.toString()))
                .findFirst()
                .orElse(null);
        
        // Get other participant info
        UUID otherId = otherParticipantId != null ? UUID.fromString(otherParticipantId) : null;
        UserInfo otherInfo = getUserInfo(otherId);
        
        Timestamp lastMessageTime = (Timestamp) data.get("lastMessageTime");
        LocalDateTime lastMessageTimeLocal = lastMessageTime != null
                ? LocalDateTime.ofInstant(lastMessageTime.toDate().toInstant(), ZoneId.systemDefault())
                : LocalDateTime.now();
        
        Timestamp createdAt = (Timestamp) data.get("createdAt");
        LocalDateTime createdAtLocal = createdAt != null
                ? LocalDateTime.ofInstant(createdAt.toDate().toInstant(), ZoneId.systemDefault())
                : LocalDateTime.now();
        
        // Calculate unread count (simplified - can be optimized)
        int unreadCount = 0;
        try {
            Query unreadQuery = firestore.collection(MESSAGES_COLLECTION)
                    .whereEqualTo("chatId", doc.getId())
                    .whereEqualTo("receiverId", currentUserId.toString())
                    .whereEqualTo("read", false);
            QuerySnapshot unreadSnapshot = unreadQuery.get().get();
            unreadCount = unreadSnapshot.size();
        } catch (Exception e) {
            log.warn("Could not calculate unread count: {}", e.getMessage());
        }
        
        return ChatConversationResponse.builder()
                .chatId(doc.getId())
                .participantId(otherId)
                .participantEmail(otherInfo.email)
                .participantName(otherInfo.name)
                .participantAvatar(otherInfo.avatar)
                .lastMessage((String) data.get("lastMessage"))
                .lastMessageTime(lastMessageTimeLocal)
                .unreadCount(unreadCount)
                .createdAt(createdAtLocal)
                .build();
    }
    
    /**
     * Helper class để lưu user info (name, avatar, email)
     */
    private static class UserInfo {
        String name;
        String avatar;
        String email;
        
        UserInfo(String name, String avatar, String email) {
            this.name = name;
            this.avatar = avatar;
            this.email = email;
        }
    }
    
    /**
     * Helper method để lấy user info (name, avatar, email) từ Account
     */
    private UserInfo getUserInfo(UUID userId) {
        if (userId == null) {
            return new UserInfo(null, null, null);
        }
        
        try {
            // Chỉ lấy account chưa bị xóa
            var accountOpt = accountRepository.findByAccountIdAndDeletedIsFalse(userId);
            if (accountOpt.isEmpty()) {
                return new UserInfo(null, null, null);
            }
            
            var account = accountOpt.get();
            String email = account.getEmail();
            String avatar = account.getAvatarUrl();
            String name = null;
            
            // Get name from profile if available
            if (account.getCareSeekerProfile() != null && !account.getCareSeekerProfile().isDeleted()) {
                name = account.getCareSeekerProfile().getFullName();
            } else if (account.getCaregiverProfile() != null && !account.getCaregiverProfile().isDeleted()) {
                name = account.getCaregiverProfile().getFullName();
            }
            
            return new UserInfo(name, avatar, email);
        } catch (Exception e) {
            log.warn("Could not fetch user info for userId={}: {}", userId, e.getMessage());
            return new UserInfo(null, null, null);
        }
    }
}
