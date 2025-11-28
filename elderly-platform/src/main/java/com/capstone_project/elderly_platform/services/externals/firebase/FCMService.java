package com.capstone_project.elderly_platform.services.externals.firebase;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Firebase Cloud Messaging operations
 */
public interface FCMService {

    /**
     * Send notification to a single device token
     *
     * @param fcmToken   Firebase Cloud Messaging token
     * @param title      Notification title
     * @param body       Notification body
     * @param data       Additional data
     * @param deviceType Device type (ANDROID, IOS, WEB)
     * @throws FirebaseMessagingException if sending fails
     */
    void sendToToken(
            String fcmToken,
            String title,
            String body,
            Map<String, Object> data,
            String deviceType) throws FirebaseMessagingException;

    /**
     * Send notification to multiple devices (batch)
     *
     * @param fcmTokens List of Firebase Cloud Messaging tokens
     * @param title     Notification title
     * @param body      Notification body
     * @param data      Additional data
     * @return BatchResponse with send results
     * @throws FirebaseMessagingException if sending fails
     */
    BatchResponse sendToMultipleTokens(
            List<String> fcmTokens,
            String title,
            String body,
            Map<String, Object> data) throws FirebaseMessagingException;
}
