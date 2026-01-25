package com.capstone_project.elderly_platform.schedulers;

import com.capstone_project.elderly_platform.services.OldFreeScheduleCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteOldFreeSchedulesJob implements Job {

    private final OldFreeScheduleCleanupService cleanupService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Starting DeleteOldFreeSchedulesJob at {}", java.time.LocalDateTime.now());
            cleanupService.deleteOldFreeSchedules();
            log.info("Completed DeleteOldFreeSchedulesJob successfully");
        } catch (Exception e) {
            log.error("Error executing DeleteOldFreeSchedulesJob", e);
            throw new JobExecutionException("Failed to delete old free schedules", e);
        }
    }
}
