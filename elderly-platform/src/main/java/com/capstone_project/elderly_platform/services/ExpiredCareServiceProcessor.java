package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.enums.EnumActorType;
import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.pojos.CareServiceStatusLog;
import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import com.capstone_project.elderly_platform.repositories.CareServiceStatusLogRepository;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.utils.CaregiverScheduleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service to process expired care services.
 * Centralizes the expiration logic to avoid code duplication.
 * 
 * Also implements MessageListener to handle Redis Keyspace Notifications
 * for real-time expiration events (when Redis keys expire).
 * 
 * Redis configuration required:
 * - CONFIG SET notify-keyspace-events Ex
 * - This enables expiration events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiredCareServiceProcessor implements MessageListener {

    private static final String KEY_PREFIX = "care_service:expire:";

    private final CareServiceRepository careServiceRepository;
    private final CareServiceStatusLogRepository careServiceStatusLogRepository;
    private final NotificationService notificationService;
    private final ExpiredCareServiceQueueService expiredCareServiceQueueService;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final CaregiverScheduleUtils caregiverScheduleUtils;

    /**
     * Processes a single expired care service.
     * Updates status to EXPIRED and creates status log.
     *
     * @param careService The care service to expire
     */
    @Transactional
    public void expireCareService(CareService careService) {
        // Double-check: Only expire if still in PENDING_CAREGIVER status
        if (careService.getStatus() != EnumCareServiceStatusType.PENDING_CAREGIVER) {
            log.info("Care service {} is no longer PENDING_CAREGIVER (current status: {}), skipping expiration",
                    careService.getCareServiceId(), careService.getStatus());
            return;
        }

        // Cancel any scheduled expiration in Redis (if exists)
        try {
            expiredCareServiceQueueService.cancelExpiration(careService.getCareServiceId());
        } catch (Exception e) {
            log.warn("Failed to cancel expiration for care service {}: {}",
                    careService.getCareServiceId(), e.getMessage());
            // Continue with expiration even if cancel fails
        }

        // Create status log
        CareServiceStatusLog statusLog = CareServiceStatusLog.builder()
                .changedBy(EnumActorType.SYSTEM)
                .careService(careService)
                .oldStatus(careService.getStatus())
                .newStatus(EnumCareServiceStatusType.EXPIRED)
                .note("Automatically expired by system due to caregiver response deadline passed")
                .build();

        careServiceStatusLogRepository.save(statusLog);

        // Update status
        careService.setStatus(EnumCareServiceStatusType.EXPIRED);
        careServiceRepository.save(careService);

        log.info("Care service {} has been expired successfully", careService.getCareServiceId());

        // Restore caregiver free schedule (đã set lịch bận khi tạo booking)
        try {
            CaregiverProfile caregiverProfile = careService.getCaregiverProfile();
            if (caregiverProfile != null) {
                String currentProfileData = caregiverProfile.getProfileData();
                String updatedProfileData = caregiverScheduleUtils.removeBookedTime(
                        currentProfileData,
                        careService.getWorkDate(),
                        careService.getStartTime(),
                        careService.getEndTime());
                caregiverProfile.setProfileData(updatedProfileData);
                caregiverProfileRepository.save(caregiverProfile);
                log.info("Restored free schedule for caregiver profile {} after expiring care service {}",
                        caregiverProfile.getCaregiverProfileId(), careService.getCareServiceId());
            }
        } catch (Exception e) {
            log.error("Failed to restore caregiver free schedule for expired care service {}: {}",
                    careService.getCareServiceId(), e.getMessage(), e);
            // Don't throw exception - expiration is already processed
        }

        // Send notification
        try {
            notificationService.sendExpiredCareServiceNotification(careService);
        } catch (Exception e) {
            log.error("Failed to send expiration notification for care service {}: {}",
                    careService.getCareServiceId(), e.getMessage(), e);
            // Don't throw exception - expiration is already processed
        }
    }

    /**
     * Processes multiple expired care services.
     *
     * @param expiredCareServices List of care services to expire
     * @return Number of successfully expired care services
     */
    @Transactional
    public int expireCareServices(List<CareService> expiredCareServices) {
        if (expiredCareServices == null || expiredCareServices.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        int failedCount = 0;

        for (CareService careService : expiredCareServices) {
            try {
                expireCareService(careService);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                log.error("Failed to expire care service {}: {}",
                        careService.getCareServiceId(), e.getMessage(), e);
                // Continue processing other services even if one fails
            }
        }

        log.info("Expired care services processing completed: {} successful, {} failed",
                successCount, failedCount);

        return successCount;
    }

    // ==================== Redis Keyspace Notifications Listener
    // ====================

    /**
     * Handles Redis Keyspace Notifications when a key expires.
     * Automatically receives events when Redis keys expire (real-time, no polling).
     * 
     * @param message The Redis message containing the expired key
     * @param pattern The pattern that matched (not used)
     */
    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        try {
            String expiredKey = new String(message.getBody());

            // Only process keys with our prefix
            if (!expiredKey.startsWith(KEY_PREFIX)) {
                return;
            }

            // Extract careServiceId from key: "care_service:expire:{uuid}"
            String careServiceIdStr = expiredKey.substring(KEY_PREFIX.length());
            UUID careServiceId;

            try {
                careServiceId = UUID.fromString(careServiceIdStr);
            } catch (IllegalArgumentException e) {
                log.error("Invalid care service ID format in expired key: {}", expiredKey);
                return;
            }

            log.info("Received expiration event for care service: {} (real-time via Redis Keyspace Notification)",
                    careServiceId);

            // Process expiration
            CareService careService = careServiceRepository
                    .findByCareServiceIdAndDeletedIsFalse(careServiceId);

            if (careService == null) {
                log.warn("Care service {} not found for expiration", careServiceId);
                return;
            }

            // Use centralized expiration logic
            expireCareService(careService);
            log.info("Care service {} has been expired successfully (real-time via Redis Keyspace Notification)",
                    careServiceId);

        } catch (Exception e) {
            log.error("Error processing expired care service event: {}", e.getMessage(), e);
        }
    }
}
