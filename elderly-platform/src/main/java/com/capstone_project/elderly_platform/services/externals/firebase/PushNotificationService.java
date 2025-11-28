package com.capstone_project.elderly_platform.services.externals.firebase;

import com.capstone_project.elderly_platform.dtos.request.NotificationTokenRequest;
import com.capstone_project.elderly_platform.dtos.response.DeviceTokenResponse;
import com.capstone_project.elderly_platform.dtos.response.NotificationResponse;
import com.capstone_project.elderly_platform.enums.EnumNotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing Push Notifications with Firebase Cloud
 * Messaging (FCM)
 * Handles device token registration, notification sending, and notification
 * history
 */
public interface PushNotificationService {

    /**
     * Register device token for push notifications
     *
     * @param accountId Account ID
     * @param request   Device token request
     * @return Device token response
     */
    DeviceTokenResponse registerDeviceToken(UUID accountId, NotificationTokenRequest request);

    /**
     * Send notification to a user
     *
     * @param recipientId       Recipient account ID
     * @param title             Notification title
     * @param body              Notification body
     * @param type              Notification type
     * @param relatedEntityType Related entity type (e.g. CARE_SERVICE)
     * @param relatedEntityId   Related entity ID
     * @param data              Additional data
     */
    void sendNotification(
            UUID recipientId,
            String title,
            String body,
            EnumNotificationType type,
            String relatedEntityType,
            UUID relatedEntityId,
            Map<String, Object> data);

    /**
     * Send notification to a user (full parameters)
     *
     * @param recipientId       Recipient account ID
     * @param senderId          Sender account ID (optional)
     * @param title             Notification title
     * @param body              Notification body
     * @param type              Notification type
     * @param relatedEntityType Related entity type
     * @param relatedEntityId   Related entity ID
     * @param data              Additional data
     * @param imageUrl          Image URL (optional)
     */
    void sendNotification(
            UUID recipientId,
            UUID senderId,
            String title,
            String body,
            EnumNotificationType type,
            String relatedEntityType,
            UUID relatedEntityId,
            Map<String, Object> data,
            String imageUrl);

    /**
     * Get paginated notifications for an account
     *
     * @param accountId Account ID
     * @param pageable  Pagination parameters
     * @return Page of notifications
     */
    Page<NotificationResponse> getNotifications(UUID accountId, Pageable pageable);

    /**
     * Count unread notifications for an account
     *
     * @param accountId Account ID
     * @return Number of unread notifications
     */
    Long countUnread(UUID accountId);

    /**
     * Mark a notification as read
     *
     * @param accountId      Account ID
     * @param notificationId Notification ID
     */
    void markAsRead(UUID accountId, UUID notificationId);

    /**
     * Mark all notifications as read for an account
     *
     * @param accountId Account ID
     */
    void markAllAsRead(UUID accountId);

    /**
     * Remove (deactivate) device token
     *
     * @param accountId Account ID
     * @param fcmToken  FCM token to remove
     */
    void removeDeviceToken(UUID accountId, String fcmToken);
}
