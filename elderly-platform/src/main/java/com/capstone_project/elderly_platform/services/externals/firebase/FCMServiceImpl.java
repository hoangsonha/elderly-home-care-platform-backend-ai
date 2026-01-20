package com.capstone_project.elderly_platform.services.externals.firebase;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FCM Service Implementation
 * Dùng FirebaseMessaging từ project mới (chatFirebaseMessaging)
 * Để match với tokens từ mobile app (project mới)
 */
@Service
@Slf4j
public class FCMServiceImpl implements FCMService {
    
    private final FirebaseMessaging firebaseMessaging;
    
    /**
     * Constructor injection với @Qualifier để dùng FirebaseMessaging từ project mới
     */
    public FCMServiceImpl(@Qualifier("chatFirebaseMessaging") FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
        log.info("FCMServiceImpl initialized with FirebaseMessaging from project mới (chatFirebaseMessaging)");
    }
    
    @Override
    public void sendToToken(
        String fcmToken,
        String title,
        String body,
        Map<String, Object> data,
        String deviceType
    ) throws FirebaseMessagingException {
        
        // Convert data Map to String Map (FCM requirement)
        Map<String, String> fcmData = new HashMap<>();
        if (data != null) {
            data.forEach((key, value) -> 
                fcmData.put(key, value != null ? value.toString() : "")
            );
        }
        
        // Build notification
        com.google.firebase.messaging.Notification notification = 
            com.google.firebase.messaging.Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
        
        // Build message
        Message.Builder messageBuilder = Message.builder()
            .setToken(fcmToken)
            .setNotification(notification)
            .putAllData(fcmData);
        
        // Android config
        if ("ANDROID".equalsIgnoreCase(deviceType)) {
            AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                    .setSound("default")
                    .setChannelId("care_service_channel")
                    .setColor("#FF6B6B")
                    .setDefaultSound(true)
                    .setDefaultVibrateTimings(true)
                    .setDefaultLightSettings(true)
                    .setVisibility(AndroidNotification.Visibility.PUBLIC)
                    .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                    .build())
                .build();
            messageBuilder.setAndroidConfig(androidConfig);
        }
        
        // iOS config
        if ("IOS".equalsIgnoreCase(deviceType)) {
            ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                    .setSound("default")
                    .setBadge(1)
                    .build())
                .build();
            messageBuilder.setApnsConfig(apnsConfig);
        }
        
        // Send
        Message message = messageBuilder.build();
        String response = firebaseMessaging.send(message);
        
        log.info("Successfully sent FCM message: {}", response);
    }
    
    @Override
    public BatchResponse sendToMultipleTokens(
        List<String> fcmTokens,
        String title,
        String body,
        Map<String, Object> data
    ) throws FirebaseMessagingException {
        
        if (fcmTokens == null || fcmTokens.isEmpty()) {
            log.warn("No FCM tokens provided");
            return null;
        }
        
        // Convert data
        Map<String, String> fcmData = new HashMap<>();
        if (data != null) {
            data.forEach((key, value) -> 
                fcmData.put(key, value != null ? value.toString() : "")
            );
        }
        
        // Build notification
        com.google.firebase.messaging.Notification notification = 
            com.google.firebase.messaging.Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
        
        // Build multicast message
        MulticastMessage message = MulticastMessage.builder()
            .addAllTokens(fcmTokens)
            .setNotification(notification)
            .putAllData(fcmData)
            .build();
        
        // Send
        BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
        
        log.info("Successfully sent {} messages, {} failures", 
            response.getSuccessCount(), 
            response.getFailureCount());
        
        // Handle failures
        if (response.getFailureCount() > 0) {
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    log.error("Failed to send to token {}: {}", 
                        fcmTokens.get(i), 
                        responses.get(i).getException().getMessage());
                }
            }
        }
        
        return response;
    }
}

