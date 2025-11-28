package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.pojos.CareService;

import java.util.Map;
import java.util.UUID;

/**
 * Service for handling business logic notifications
 * (Care Service notifications, reminders, etc.)
 */
public interface NotificationService {

    /**
     * Send a push notification to a user
     * 
     * @param userId User's account ID
     * @param title  Notification title
     * @param body   Notification body
     * @param data   Additional data map
     */
    void sendPushNotification(UUID userId, String title, String body, Map<String, String> data);

    /**
     * Send notification when a care service expires
     * 
     * @param careService The expired care service
     */
    void sendExpiredCareServiceNotification(CareService careService);

    /**
     * Send notification when a care service status changes
     * 
     * @param careService The care service
     * @param newStatus   The new status
     */
    void sendCareServiceStatusChangeNotification(CareService careService, String newStatus);

    /**
     * Send reminder notification to caregiver before care service expires
     * 
     * @param careService The care service
     * @param hoursBefore Hours before expiration
     */
    void sendReminderNotification(CareService careService, int hoursBefore);
}
