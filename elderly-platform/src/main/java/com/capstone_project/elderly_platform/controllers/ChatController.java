package com.capstone_project.elderly_platform.controllers;

import com.capstone_project.elderly_platform.configurations.CustomAccountDetail;
import com.capstone_project.elderly_platform.dtos.request.ChatMessageRequest;
import com.capstone_project.elderly_platform.dtos.response.ChatConversationResponse;
import com.capstone_project.elderly_platform.dtos.response.ChatMessageResponse;
import com.capstone_project.elderly_platform.dtos.response.ObjectResponse;
import com.capstone_project.elderly_platform.services.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("/api/v1/chat")
@RestController
@RefreshScope
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Real-time chat operations using Firestore")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Send message", description = "Send a message to another user. Message is stored in Firestore for real-time delivery.")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @PostMapping("/send")
    public ResponseEntity<ObjectResponse> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            Authentication authentication) {
        try {
            UUID senderId = getUserIdFromAuthentication(authentication);
            ChatMessageResponse response = chatService.sendMessage(senderId, request);
            
            // Log response với avatar và name
            log.info("Send Message Response - messageId: {}, chatId: {}, senderId: {}, senderName: {}, senderAvatar: {}, receiverId: {}, receiverName: {}, receiverAvatar: {}", 
                    response.getMessageId(),
                    response.getChatId(),
                    response.getSenderId(),
                    response.getSenderName(),
                    response.getSenderAvatar(),
                    response.getReceiverId(),
                    response.getReceiverName(),
                    response.getReceiverAvatar());
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Message sent successfully", response));
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to send message: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get conversations", description = "Get all conversations for the current user")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @GetMapping("/conversations")
    public ResponseEntity<ObjectResponse> getConversations(Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            List<ChatConversationResponse> conversations = chatService.getConversations(userId);
            
            // Log response với avatar và name
            log.info("Get Conversations Response - Count: {}", conversations.size());
            conversations.forEach(conv -> {
                log.info("Conversation - chatId: {}, participantId: {}, participantName: {}, participantAvatar: {}", 
                        conv.getChatId(), 
                        conv.getParticipantId(), 
                        conv.getParticipantName(), 
                        conv.getParticipantAvatar());
            });
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Conversations retrieved successfully", conversations));
        } catch (Exception e) {
            log.error("Error getting conversations", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get conversations: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get messages", description = "Get messages for a specific chat conversation")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @GetMapping("/messages/{chatId}")
    public ResponseEntity<ObjectResponse> getMessages(
            @Parameter(description = "Chat ID") @PathVariable String chatId,
            @Parameter(description = "Limit number of messages (default: 50)") @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        try {
            List<ChatMessageResponse> messages = chatService.getMessages(chatId, limit);
            
            // Log response với avatar và name
            log.info("Get Messages Response - chatId: {}, Count: {}", chatId, messages.size());
            messages.forEach(msg -> {
                log.info("Message - messageId: {}, senderId: {}, senderName: {}, senderAvatar: {}, receiverId: {}, receiverName: {}, receiverAvatar: {}", 
                        msg.getMessageId(),
                        msg.getSenderId(),
                        msg.getSenderName(),
                        msg.getSenderAvatar(),
                        msg.getReceiverId(),
                        msg.getReceiverName(),
                        msg.getReceiverAvatar());
            });
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Messages retrieved successfully", messages));
        } catch (Exception e) {
            log.error("Error getting messages", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get messages: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Mark message as read", description = "Mark a message as read")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<ObjectResponse> markAsRead(
            @Parameter(description = "Message ID") @PathVariable String messageId,
            Authentication authentication) {
        try {
            UUID userId = getUserIdFromAuthentication(authentication);
            chatService.markAsRead(messageId, userId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Message marked as read", null));
        } catch (Exception e) {
            log.error("Error marking message as read", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to mark message as read: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Get or create chat ID", description = "Get or create chat ID between current user and receiver. Returns chatId for navigation to chat screen.")
    @PreAuthorize("hasRole('CAREGIVER') or hasRole('CARE_SEEKER')")
    @GetMapping("/chat-id/{receiverId}")
    public ResponseEntity<ObjectResponse> getOrCreateChatId(
            @Parameter(description = "Receiver user ID") @PathVariable UUID receiverId,
            Authentication authentication) {
        try {
            UUID senderId = getUserIdFromAuthentication(authentication);
            
            // Log để debug
            log.info("Get Chat ID API called - senderId: {} (toString: {}), receiverId: {} (toString: {})", 
                    senderId, senderId.toString(), receiverId, receiverId.toString());
            
            String chatId = chatService.getOrCreateChatId(senderId, receiverId);
            
            log.info("Get Chat ID Response - senderId: {}, receiverId: {}, chatId: {}", senderId, receiverId, chatId);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("chatId", chatId);
            responseData.put("senderId", senderId);
            responseData.put("receiverId", receiverId);
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ObjectResponse("Success", "Chat ID retrieved successfully", responseData));
        } catch (Exception e) {
            log.error("Error getting chat ID", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ObjectResponse("Failed", "Failed to get chat ID: " + e.getMessage(), null));
        }
    }

    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomAccountDetail) {
            CustomAccountDetail accountDetail = (CustomAccountDetail) authentication.getPrincipal();
            return accountDetail.getId();
        }
        throw new IllegalStateException("User not authenticated");
    }
}
