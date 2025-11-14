package com.capstone_project.elderly_platform.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service to manage expired care service queue using Redis TTL with Keyspace
 * Notifications.
 * Uses Redis key expiration to trigger real-time expiration events.
 * When key expires, Redis automatically publishes event → Listener processes
 * immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiredCareServiceQueueService {

    private static final String KEY_PREFIX = "care_service:expire:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Schedules expiration by setting a Redis key with TTL.
     * When TTL expires, Redis automatically publishes an expiration event.
     *
     * @param careServiceId The ID of the care service
     * @param deadline      The deadline when the care service should expire
     */
    public void scheduleExpiration(UUID careServiceId, LocalDateTime deadline) {
        try {
            String key = KEY_PREFIX + careServiceId.toString();

            // Calculate TTL in seconds from now to deadline
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(now, deadline);
            long ttlSeconds = duration.getSeconds();

            // Only set if deadline is in the future
            if (ttlSeconds > 0) {
                // Set key with value = careServiceId (for listener to extract)
                // TTL = seconds until deadline
                redisTemplate.opsForValue().set(key, careServiceId.toString(),
                        Duration.ofSeconds(ttlSeconds));

                log.info("Scheduled expiration for care service {} at {} (TTL: {} seconds)",
                        careServiceId, deadline, ttlSeconds);
            } else {
                log.warn("Deadline {} for care service {} is in the past, skipping schedule",
                        deadline, careServiceId);
            }
        } catch (Exception e) {
            log.error("Failed to schedule expiration for care service {}: {}",
                    careServiceId, e.getMessage(), e);
            throw new RuntimeException("Failed to schedule care service expiration", e);
        }
    }

    /**
     * Cancels the scheduled expiration for a care service.
     * Called when caregiver accepts/declines or seeker cancels.
     * Deletes the Redis key to prevent expiration event.
     *
     * @param careServiceId The ID of the care service to cancel
     */
    public void cancelExpiration(UUID careServiceId) {
        try {
            String key = KEY_PREFIX + careServiceId.toString();
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("Cancelled expiration for care service {}", careServiceId);
            } else {
                log.debug("No expiration found to cancel for care service {}", careServiceId);
            }
        } catch (Exception e) {
            log.error("Failed to cancel expiration for care service {}: {}",
                    careServiceId, e.getMessage(), e);
            // Don't throw exception to avoid breaking the main flow
        }
    }
}
