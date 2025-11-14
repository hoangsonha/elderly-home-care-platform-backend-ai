package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.pojos.CareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    // AccountRepository can be used later to get FCM tokens or other user info
    // private final AccountRepository accountRepository;

    @Override
    public void sendPushNotification(UUID userId, String title, String body, Map<String, String> data) {
        try {
            // TODO: Implement actual push notification using Firebase Admin SDK
            // For now, just log the notification
            log.info("Sending push notification to user {}: Title: {}, Body: {}, Data: {}", userId, title, body, data);

            // Example Firebase implementation (uncomment when ready):
            // FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
            // Message message = Message.builder()
            //     .setToken(getUserFcmToken(userId))
            //     .setNotification(Notification.builder()
            //         .setTitle(title)
            //         .setBody(body)
            //         .build())
            //     .putAllData(data != null ? data : new HashMap<>())
            //     .build();
            // firebaseMessaging.send(message);

        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}", userId, e.getMessage(), e);
            // Don't throw exception to avoid breaking the main flow
        }
    }

    @Override
    public void sendExpiredCareServiceNotification(CareService careService) {
        if (careService == null) {
            log.warn("Cannot send expired notification: careService is null");
            return;
        }

        try {
            // Get account IDs
            UUID caregiverAccountId = careService.getCaregiverProfile() != null
                    && careService.getCaregiverProfile().getAccount() != null
                    ? careService.getCaregiverProfile().getAccount().getAccountId()
                    : null;

            UUID seekerAccountId = careService.getCareSeekerProfile() != null
                    && careService.getCareSeekerProfile().getAccount() != null
                    ? careService.getCareSeekerProfile().getAccount().getAccountId()
                    : null;

            // Prepare notification data
            Map<String, String> data = new HashMap<>();
            data.put("careServiceId", careService.getCareServiceId().toString());
            data.put("bookingCode", careService.getBookingCode());
            data.put("type", "CARE_SERVICE_EXPIRED");

            // Notify caregiver
            if (caregiverAccountId != null) {
                sendPushNotification(
                        caregiverAccountId,
                        "Yêu cầu chăm sóc đã hết hạn",
                        String.format("Yêu cầu chăm sóc #%s đã hết hạn do không phản hồi kịp thời.",
                                careService.getBookingCode()),
                        data);
            }

            // Notify care seeker
            if (seekerAccountId != null) {
                sendPushNotification(
                        seekerAccountId,
                        "Yêu cầu chăm sóc đã hết hạn",
                        String.format("Yêu cầu chăm sóc #%s đã hết hạn. Vui lòng đặt lại dịch vụ mới.",
                                careService.getBookingCode()),
                        data);
            }

            log.info("Expired notifications sent for care service: {}", careService.getCareServiceId());

        } catch (Exception e) {
            log.error("Failed to send expired care service notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendCareServiceStatusChangeNotification(CareService careService, String newStatus) {
        if (careService == null) {
            log.warn("Cannot send status change notification: careService is null");
            return;
        }

        try {
            // Get account IDs
            UUID caregiverAccountId = careService.getCaregiverProfile() != null
                    && careService.getCaregiverProfile().getAccount() != null
                    ? careService.getCaregiverProfile().getAccount().getAccountId()
                    : null;

            UUID seekerAccountId = careService.getCareSeekerProfile() != null
                    && careService.getCareSeekerProfile().getAccount() != null
                    ? careService.getCareSeekerProfile().getAccount().getAccountId()
                    : null;

            // Prepare notification data
            Map<String, String> data = new HashMap<>();
            data.put("careServiceId", careService.getCareServiceId().toString());
            data.put("bookingCode", careService.getBookingCode());
            data.put("newStatus", newStatus);
            data.put("type", "CARE_SERVICE_STATUS_CHANGED");

            String title = "";
            String caregiverMessage = "";
            String seekerMessage = "";

            // Customize message based on status
            switch (EnumCareServiceStatusType.valueOf(newStatus)) {
                case CAREGIVER_APPROVED:
                    title = "Yêu cầu chăm sóc đã được chấp nhận";
                    caregiverMessage = String.format("Bạn đã chấp nhận yêu cầu chăm sóc #%s",
                            careService.getBookingCode());
                    seekerMessage = String.format("Yêu cầu chăm sóc #%s đã được caregiver chấp nhận",
                            careService.getBookingCode());
                    break;
                case CANCELLED:
                    title = "Yêu cầu chăm sóc đã bị hủy";
                    caregiverMessage = String.format("Yêu cầu chăm sóc #%s đã bị hủy",
                            careService.getBookingCode());
                    seekerMessage = String.format("Yêu cầu chăm sóc #%s đã bị hủy",
                            careService.getBookingCode());
                    break;
                case IN_PROGRESS:
                    title = "Dịch vụ chăm sóc đã bắt đầu";
                    caregiverMessage = String.format("Dịch vụ chăm sóc #%s đã bắt đầu",
                            careService.getBookingCode());
                    seekerMessage = String.format("Dịch vụ chăm sóc #%s đã bắt đầu",
                            careService.getBookingCode());
                    break;
                case COMPLETED:
                    title = "Dịch vụ chăm sóc đã hoàn thành";
                    caregiverMessage = String.format("Dịch vụ chăm sóc #%s đã hoàn thành",
                            careService.getBookingCode());
                    seekerMessage = String.format("Dịch vụ chăm sóc #%s đã hoàn thành. Vui lòng đánh giá dịch vụ.",
                            careService.getBookingCode());
                    break;
                default:
                    title = "Trạng thái yêu cầu chăm sóc đã thay đổi";
                    caregiverMessage = String.format("Trạng thái yêu cầu chăm sóc #%s đã thay đổi thành %s",
                            careService.getBookingCode(), newStatus);
                    seekerMessage = String.format("Trạng thái yêu cầu chăm sóc #%s đã thay đổi thành %s",
                            careService.getBookingCode(), newStatus);
            }

            // Notify caregiver
            if (caregiverAccountId != null && !caregiverMessage.isEmpty()) {
                sendPushNotification(caregiverAccountId, title, caregiverMessage, data);
            }

            // Notify care seeker
            if (seekerAccountId != null && !seekerMessage.isEmpty()) {
                sendPushNotification(seekerAccountId, title, seekerMessage, data);
            }

            log.info("Status change notifications sent for care service: {} with new status: {}",
                    careService.getCareServiceId(), newStatus);

        } catch (Exception e) {
            log.error("Failed to send status change notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendReminderNotification(CareService careService, int hoursBefore) {
        if (careService == null) {
            log.warn("Cannot send reminder notification: careService is null");
            return;
        }

        try {
            UUID caregiverAccountId = careService.getCaregiverProfile() != null
                    && careService.getCaregiverProfile().getAccount() != null
                    ? careService.getCaregiverProfile().getAccount().getAccountId()
                    : null;

            if (caregiverAccountId == null) {
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("careServiceId", careService.getCareServiceId().toString());
            data.put("bookingCode", careService.getBookingCode());
            data.put("type", "CARE_SERVICE_REMINDER");
            data.put("hoursBefore", String.valueOf(hoursBefore));

            sendPushNotification(
                    caregiverAccountId,
                    "Nhắc nhở phản hồi yêu cầu chăm sóc",
                    String.format("Bạn còn %d giờ để phản hồi yêu cầu chăm sóc #%s",
                            hoursBefore, careService.getBookingCode()),
                    data);

            log.info("Reminder notification sent for care service: {} ({} hours before deadline)",
                    careService.getCareServiceId(), hoursBefore);

        } catch (Exception e) {
            log.error("Failed to send reminder notification: {}", e.getMessage(), e);
        }
    }
}

