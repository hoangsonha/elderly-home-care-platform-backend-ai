package com.capstone_project.elderly_platform.listeners;

import com.capstone_project.elderly_platform.enums.EnumNotificationType;
import com.capstone_project.elderly_platform.events.CareServiceCreatedEvent;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.services.externals.firebase.PushNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CareServiceEventListener {

    private final PushNotificationService pushNotificationService;
    private final ObjectMapper objectMapper;

    @EventListener
    @Async
    public void handleCareServiceCreated(CareServiceCreatedEvent event) {
        CareService careService = event.getCareService();

        log.info("Handling CareServiceCreatedEvent for service: {}",
                careService.getCareServiceId());

        try {
            // 1. Kiểm tra caregiver đã được assign chưa
            CaregiverProfile assignedCaregiver = careService.getCaregiverProfile();

            if (assignedCaregiver == null) {
                log.warn("Care service {} has no assigned caregiver, skipping notification",
                        careService.getCareServiceId());
                return;
            }

            log.info("Sending notification to assigned caregiver: {} for care service: {}",
                    assignedCaregiver.getCaregiverProfileId(),
                    careService.getCareServiceId());

            // 2. Gửi notification cho caregiver được assign
            try {
                // Lấy account_id từ profile
                UUID caregiverAccountId = assignedCaregiver.getAccount().getAccountId();

                // Tạo notification data
                Map<String, Object> data = new HashMap<>();
                data.put("careServiceId", careService.getCareServiceId().toString());
                data.put("bookingCode", careService.getBookingCode());

                // Elderly info
                if (careService.getElderlyProfile() != null) {
                    data.put("elderlyName", careService.getElderlyProfile().getFullName());
                }

                // Service package info
                if (careService.getServicePackage() != null) {
                    data.put("servicePackageName", careService.getServicePackage().getPackageName());
                }

                // Work schedule info
                if (careService.getWorkDate() != null) {
                    data.put("workDate", careService.getWorkDate().toString());
                }

                if (careService.getStartTime() != null && careService.getEndTime() != null) {
                    data.put("startTime", careService.getStartTime().toString());
                    data.put("endTime", careService.getEndTime().toString());
                    data.put("workTime", careService.getStartTime() + " - " + careService.getEndTime());
                }

                // Price info
                if (careService.getTotalPrice() != null) {
                    data.put("totalPrice", careService.getTotalPrice());
                }

                // Parse location từ JSON
                try {
                    String locationJson = careService.getElderlyProfile().getLocation();
                    if (locationJson != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> locationMap = objectMapper.readValue(locationJson, Map.class);
                        data.put("location", locationMap);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse location: {}", e.getMessage());
                }

                // Build notification message
                String elderlyName = careService.getElderlyProfile() != null
                        ? careService.getElderlyProfile().getFullName()
                        : "người cao tuổi";

                String workInfo = "";
                if (careService.getWorkDate() != null) {
                    workInfo = String.format("vào ngày %s", careService.getWorkDate());

                    if (careService.getStartTime() != null && careService.getEndTime() != null) {
                        workInfo += String.format(" từ %s đến %s",
                                careService.getStartTime(),
                                careService.getEndTime());
                    }
                }

                String notificationBody = String.format(
                        "Yêu cầu chăm sóc cho %s %s. Nhấn để xem chi tiết.",
                        elderlyName,
                        workInfo);

                // Gửi notification
                pushNotificationService.sendNotification(
                        caregiverAccountId, // recipient
                        careService.getCareSeekerProfile().getAccount().getAccountId(), // sender
                        "Có yêu cầu chăm sóc mới!",
                        notificationBody,
                        EnumNotificationType.NEW_CARE_SERVICE_REQUEST,
                        "CARE_SERVICE",
                        careService.getCareServiceId(),
                        data,
                        null);

                log.info("Notification sent to caregiver: {}", caregiverAccountId);

            } catch (Exception e) {
                log.error("Failed to send notification to caregiver {}: {}",
                        assignedCaregiver.getCaregiverProfileId(), e.getMessage());
            }

            log.info("Completed sending notifications for care service: {}",
                    careService.getCareServiceId());

        } catch (Exception e) {
            log.error("Error handling CareServiceCreatedEvent: {}", e.getMessage(), e);
        }
    }
}
