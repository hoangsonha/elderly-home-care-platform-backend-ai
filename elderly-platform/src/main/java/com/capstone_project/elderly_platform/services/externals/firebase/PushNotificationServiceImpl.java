package com.capstone_project.elderly_platform.services.externals.firebase;

import com.capstone_project.elderly_platform.dtos.request.NotificationTokenRequest;
import com.capstone_project.elderly_platform.dtos.response.DeviceTokenResponse;
import com.capstone_project.elderly_platform.dtos.response.NotificationResponse;
import com.capstone_project.elderly_platform.enums.EnumNotificationType;
import com.capstone_project.elderly_platform.pojos.Account;
import com.capstone_project.elderly_platform.pojos.DeviceToken;
import com.capstone_project.elderly_platform.pojos.Notification;
import com.capstone_project.elderly_platform.repositories.DeviceTokenRepository;
import com.capstone_project.elderly_platform.repositories.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationServiceImpl implements PushNotificationService {
    
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;
    private final FCMService fcmService;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public DeviceTokenResponse registerDeviceToken(UUID accountId, NotificationTokenRequest request) {
        log.info("Registering device token for account {} from device {}", 
            accountId, request.getDeviceType());
        
        // SINGLE DEVICE MODE: Deactivate tất cả token cũ của account này
        deviceTokenRepository.deactivateAllTokensByAccount(accountId);
        log.info("Deactivated all existing device tokens for account {}", accountId);
        
        // Tạo token mới
        DeviceToken deviceToken = DeviceToken.builder()
            .account(Account.builder().accountId(accountId).build())
            .fcmToken(request.getFcmToken())
            .deviceType(request.getDeviceType())
            .deviceName(request.getDeviceName())
            .isActive(true)
            .lastUsedAt(LocalDateTime.now())
            .build();
        
        deviceToken = deviceTokenRepository.save(deviceToken);
        
        log.info("Device token registered successfully: {} (device: {})", 
            deviceToken.getDeviceTokenId(), 
            deviceToken.getDeviceType());
        
        return convertToDeviceTokenResponse(deviceToken);
    }
    
    @Override
    @Transactional
    public void sendNotification(
        UUID recipientId,
        String title,
        String body,
        EnumNotificationType type,
        String relatedEntityType,
        UUID relatedEntityId,
        Map<String, Object> data
    ) {
        sendNotification(recipientId, null, title, body, type, 
            relatedEntityType, relatedEntityId, data, null);
    }
    
    @Override
    @Transactional
    public void sendNotification(
        UUID recipientId,
        UUID senderId,
        String title,
        String body,
        EnumNotificationType type,
        String relatedEntityType,
        UUID relatedEntityId,
        Map<String, Object> data,
        String imageUrl
    ) {
        log.info("Sending notification to user {}: {}", recipientId, title);
        
        try {
            // 1. Lưu notification vào DB
            Notification notification = Notification.builder()
                .recipient(Account.builder().accountId(recipientId).build())
                .sender(senderId != null ? Account.builder().accountId(senderId).build() : null)
                .title(title)
                .body(body)
                .notificationType(type)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .data(data != null ? objectMapper.writeValueAsString(data) : null)
                .imageUrl(imageUrl)
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
            
            notification = notificationRepository.save(notification);
            log.info("Notification saved to DB: {}", notification.getNotificationId());
            
            // 2. Lấy device tokens của user
            List<DeviceToken> deviceTokens = deviceTokenRepository
                .findByAccount_AccountIdAndIsActiveTrue(recipientId);
            
            if (deviceTokens.isEmpty()) {
                log.warn("No active device tokens for user {}", recipientId);
                return;
            }
            
            log.info("Found {} active device(s) for user {}", 
                deviceTokens.size(), recipientId);
            
            // 3. Thêm notificationId vào data
            if (data == null) {
                data = new HashMap<>();
            }
            data.put("notificationId", notification.getNotificationId().toString());
            data.put("notificationType", type.name());
            if (relatedEntityType != null) {
                data.put("relatedEntityType", relatedEntityType);
            }
            // Only add relatedEntityId if it's not null
            if (relatedEntityId != null) {
                data.put("relatedEntityId", relatedEntityId.toString());
            }
            
            // 4. Gửi FCM notification đến từng device
            for (DeviceToken token : deviceTokens) {
                try {
                    fcmService.sendToToken(
                        token.getFcmToken(),
                        title,
                        body,
                        data,
                        token.getDeviceType()
                    );
                    
                    // Update last_used_at
                    token.setLastUsedAt(LocalDateTime.now());
                    deviceTokenRepository.save(token);
                    
                    log.info("Notification sent successfully to device {}", 
                        token.getDeviceType());
                    
                } catch (FirebaseMessagingException e) {
                    log.error("Failed to send notification to device {}: {}", 
                        token.getDeviceType(), e.getMessage());
                    
                    // Handle invalid token
                    if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                        e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                        log.warn("Deactivating invalid token for user {}", recipientId);
                        token.setIsActive(false);
                        deviceTokenRepository.save(token);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public Page<NotificationResponse> getNotifications(UUID accountId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
            .findByRecipient_AccountIdAndDeletedFalseOrderByCreatedAtDesc(accountId, pageable);
        
        return notifications.map(this::convertToNotificationResponse);
    }
    
    @Override
    public Long countUnread(UUID accountId) {
        return notificationRepository.countUnreadByRecipient(accountId);
    }
    
    @Override
    @Transactional
    public void markAsRead(UUID accountId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getRecipient().getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }
    
    @Override
    @Transactional
    public void markAllAsRead(UUID accountId) {
        Page<Notification> unreadNotifications = notificationRepository
            .findUnreadByRecipient(accountId, Pageable.unpaged());
        
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        });
        
        notificationRepository.saveAll(unreadNotifications.getContent());
        log.info("Marked {} notifications as read for user {}", 
            unreadNotifications.getTotalElements(), accountId);
    }
    
    @Override
    @Transactional
    public void removeDeviceToken(UUID accountId, String fcmToken) {
        deviceTokenRepository.deactivateToken(accountId, fcmToken);
        log.info("Device token deactivated for account {}", accountId);
    }
    
    // Private helper methods
    
    private DeviceTokenResponse convertToDeviceTokenResponse(DeviceToken token) {
        return DeviceTokenResponse.builder()
            .deviceTokenId(token.getDeviceTokenId())
            .deviceType(token.getDeviceType())
            .deviceName(token.getDeviceName())
            .isActive(token.getIsActive())
            .lastUsedAt(token.getLastUsedAt())
            .createdAt(token.getCreatedAt())
            .build();
    }
    
    @SuppressWarnings("unchecked")
    private NotificationResponse convertToNotificationResponse(Notification notification) {
        try {
            Map<String, Object> dataMap = null;
            if (notification.getData() != null) {
                dataMap = objectMapper.readValue(notification.getData(), Map.class);
            }
            
            return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .notificationType(notification.getNotificationType().name())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .data(dataMap)
                .imageUrl(notification.getImageUrl())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
        } catch (Exception e) {
            log.error("Error converting notification: {}", e.getMessage());
            return null;
        }
    }
}

