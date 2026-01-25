package com.capstone_project.elderly_platform.services;

import com.capstone_project.elderly_platform.pojos.CaregiverProfile;
import com.capstone_project.elderly_platform.repositories.CaregiverProfileRepository;
import com.capstone_project.elderly_platform.utils.CaregiverScheduleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OldFreeScheduleCleanupService {

    private final CaregiverProfileRepository caregiverProfileRepository;
    private final CaregiverScheduleUtils scheduleUtils;

    /**
     * Delete booked slots from yesterday for all caregivers
     * This method is called daily at 0:00 AM by DeleteOldFreeSchedulesJob
     */
    @Transactional
    public void deleteOldFreeSchedules() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting cleanup of old free schedules for date: {}", yesterday);

        // Get all active caregiver profiles
        List<CaregiverProfile> caregiverProfiles = caregiverProfileRepository.findAll();

        if (caregiverProfiles.isEmpty()) {
            log.info("No caregiver profiles found, skipping cleanup");
            return;
        }

        int updatedCount = 0;
        int errorCount = 0;

        for (CaregiverProfile profile : caregiverProfiles) {
            try {
                String currentProfileData = profile.getProfileData();

                if (currentProfileData == null || currentProfileData.isEmpty()) {
                    continue; // Skip profiles without profileData
                }

                // Remove booked slots for yesterday
                String updatedProfileData = scheduleUtils.removeBookedSlotsByDate(
                        currentProfileData, yesterday);

                // Only update if profileData changed
                if (!updatedProfileData.equals(currentProfileData)) {
                    profile.setProfileData(updatedProfileData);
                    caregiverProfileRepository.save(profile);
                    updatedCount++;
                    log.debug("Removed old booked slots for caregiver profile ID: {}",
                            profile.getCaregiverProfileId());
                }
            } catch (Exception e) {
                errorCount++;
                log.error("Error cleaning up free schedule for caregiver profile ID: {}",
                        profile.getCaregiverProfileId(), e);
                // Continue processing other profiles even if one fails
            }
        }

        log.info("Completed cleanup: {} profiles updated, {} errors, {} total profiles processed",
                updatedCount, errorCount, caregiverProfiles.size());
    }
}
