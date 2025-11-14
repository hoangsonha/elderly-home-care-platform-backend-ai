package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.pojos.CareService;

import java.util.UUID;

/**
 * Service for sending notifications (push notifications, in-app notifications,
 * etc.)
 */
public interface NotificationService {

    /**
     * Sends a push notification to a specific user.
     *
     * @param userId The account ID of the user to notify
     * @param title  The notification title
     * @param body   The notification body/message
     * @param data   Additional data payload (can be null)
     */
    void sendPushNotification(UUID userId, String title, String body, java.util.Map<String, String> data);

    /**
     * Sends notification to both caregiver and care seeker when a care service
     * expires.
     *
     * @param careService The expired care service
     */
    void sendExpiredCareServiceNotification(CareService careService);

    /**
     * Sends notification when care service status changes.
     *
     * @param careService The care service with changed status
     * @param newStatus   The new status
     */
    void sendCareServiceStatusChangeNotification(CareService careService, String newStatus);

    /**
     * Sends reminder notification before caregiver response deadline.
     *
     * @param careService The care service that needs reminder
     * @param hoursBefore Hours before deadline to send reminder
     */
    void sendReminderNotification(CareService careService, int hoursBefore);
}
