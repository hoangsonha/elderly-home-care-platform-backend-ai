package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.enums.EnumCareServiceStatusType;
import com.capstone_project.elderly_platform.pojos.CareService;
import com.capstone_project.elderly_platform.repositories.CareServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service to handle expired care service recovery and periodic checking.
 * 
 * 1. RECOVERY (on startup):
 *    - Reschedules expired care service keys in Redis after application restart
 *    - Checks and expires any care services that have already passed their deadline
 *    
 *    IMPORTANT: This handles the critical edge case where:
 *    - Server/Redis restarts at the exact moment a care service expires
 *    - Redis keys are lost (no persistence) or expired during restart
 *    - Database still has status = PENDING_CAREGIVER but deadline has passed
 * 
 *    Flow:
 *    1. FIRST: Check and expire all care services with status = PENDING_CAREGIVER 
 *       that have already passed their deadline (caregiverResponseDeadline < now)
 *       → This ensures no expired care services remain in PENDING_CAREGIVER status
 * 
 *    2. THEN: Reschedule all care services with status = PENDING_CAREGIVER 
 *       that still have future deadlines (caregiverResponseDeadline > now)
 *       → This restores Redis keys for pending care services
 * 
 * 2. PERIODIC CHECK (every 5 minutes):
 *    - Backup mechanism to ensure no care services are missed
 *    - Handles edge cases where Redis Keyspace Notifications might have been missed
 *    - Checks for expired care services and automatically expires them
 * 
 * This ensures data consistency and prevents expired care services from being missed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(100) // Run after DatabaseInit (which has default order)
public class ExpiredCareServiceRecoveryService implements CommandLineRunner {

    private final CareServiceRepository careServiceRepository;
    private final ExpiredCareServiceQueueService expiredCareServiceQueueService;
    private final ExpiredCareServiceProcessor expiredCareServiceProcessor;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            log.info("Starting expired care service recovery...");
            LocalDateTime now = LocalDateTime.now();

            // Step 1: CRITICAL - Check and expire care services that have already passed their deadline
            // This handles the edge case where:
            // - Server/Redis restarted at the exact moment expiration should happen
            // - Redis keys are lost but database still has status = PENDING_CAREGIVER
            // - Deadline has passed but status hasn't been updated to EXPIRED
            // 
            // We MUST do this BEFORE rescheduling to ensure data consistency
            log.info("Step 1: Checking for care services with status PENDING_CAREGIVER that have already passed their deadline...");
            List<CareService> expiredCareServices = careServiceRepository.findExpiredCareServices(
                    EnumCareServiceStatusType.PENDING_CAREGIVER, now);

            if (!expiredCareServices.isEmpty()) {
                log.warn("Found {} care service(s) with status PENDING_CAREGIVER that have already passed their deadline. " +
                        "This likely happened during server/Redis restart. Expiring them now...",
                        expiredCareServices.size());
                int expiredCount = expiredCareServiceProcessor.expireCareServices(expiredCareServices);
                log.info("Successfully expired {} care service(s) that were missed during restart", expiredCount);
            } else {
                log.info("No expired care services found (all care services are up-to-date)");
            }

            // Step 2: Reschedule pending care services that still have future deadlines
            // Only care services with deadline > now will be rescheduled
            // (Expired ones were already handled in Step 1)
            log.info("Step 2: Rescheduling pending care services with future deadlines...");
            List<CareService> pendingCareServices = careServiceRepository
                    .findPendingCareServicesForRecovery(EnumCareServiceStatusType.PENDING_CAREGIVER, now);

            if (pendingCareServices.isEmpty()) {
                log.info("No pending care services found to reschedule");
                return;
            }

            log.info("Found {} pending care service(s) to reschedule", pendingCareServices.size());

            int successCount = 0;
            int failedCount = 0;

            for (CareService careService : pendingCareServices) {
                try {
                    // Reschedule expiration
                    expiredCareServiceQueueService.scheduleExpiration(
                            careService.getCareServiceId(),
                            careService.getCaregiverResponseDeadline());
                    successCount++;
                    log.debug("Rescheduled expiration for care service {} (deadline: {})",
                            careService.getCareServiceId(), careService.getCaregiverResponseDeadline());
                } catch (Exception e) {
                    failedCount++;
                    log.error("Failed to reschedule expiration for care service {}: {}",
                            careService.getCareServiceId(), e.getMessage(), e);
                }
            }

            log.info("Recovery completed: {} expired, {} rescheduled successfully, {} reschedule failed",
                    expiredCareServices.size(), successCount, failedCount);

        } catch (Exception e) {
            log.error("Error during expired care service recovery: {}", e.getMessage(), e);
            // Don't throw exception to avoid blocking application startup
        }
    }

    // ==================== Periodic Backup Check ====================

    /**
     * Scheduled task to periodically check and expire care services that have passed their deadline.
     * 
     * This is a backup mechanism to ensure no care services are missed, especially in edge cases:
     * - Redis key expired but listener didn't process it (server restart during expiration)
     * - Redis was down when expiration should have happened
     * - Any other edge cases where Redis Keyspace Notifications might have been missed
     * 
     * Runs every 5 minutes (300000 milliseconds) as a backup mechanism.
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void checkAndExpireCareServices() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Find all care services with PENDING_CAREGIVER status that have passed their deadline
            List<CareService> expiredCareServices = careServiceRepository.findExpiredCareServices(
                    EnumCareServiceStatusType.PENDING_CAREGIVER, now);

            if (expiredCareServices.isEmpty()) {
                log.debug("No expired care services found at {} (periodic check)", now);
                return;
            }

            log.info("Found {} expired care service(s) to process (periodic backup check)", expiredCareServices.size());

            // Process expired care services
            int expiredCount = expiredCareServiceProcessor.expireCareServices(expiredCareServices);

            log.info("Successfully processed {} expired care service(s) (periodic backup check)", expiredCount);

        } catch (Exception e) {
            log.error("Error in expired care service periodic check: {}", e.getMessage(), e);
            // Don't throw exception to avoid breaking the scheduler
        }
    }
}

