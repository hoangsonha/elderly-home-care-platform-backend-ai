package com.capstone_project.elderly_platform.schedulers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainSchedule {

    private final Scheduler scheduler;
    private final CommonUtils commonUtils;
    
    @Value("${job.delete-old-free-schedules.hour:0}")
    private int deleteOldSchedulesHour;
    
    @Value("${job.delete-old-free-schedules.minute:0}")
    private int deleteOldSchedulesMinute;

    @PostConstruct
    public void startSchedule() throws SchedulerException {
        scheduler.start();
        log.info("Quartz Scheduler started");
        
        // Schedule job to delete old free schedules
        try {
            scheduler(DeleteOldFreeSchedulesJob.class, deleteOldSchedulesHour, deleteOldSchedulesMinute);
            log.info("Scheduled DeleteOldFreeSchedulesJob to run daily at {}:{}", 
                    deleteOldSchedulesHour, deleteOldSchedulesMinute);
        } catch (Exception e) {
            log.error("Failed to schedule DeleteOldFreeSchedulesJob", e);
        }
    }

    public void scheduler(Class<? extends Job> jobClass, int hour, int minute) throws SchedulerException {
        JobDetail jobDetail = commonUtils.getJobDetail(jobClass);
        Trigger triggerDetail = commonUtils.getTriggerByCronExpression(jobClass, hour, minute);
        scheduler.scheduleJob(jobDetail, triggerDetail);
        log.info("Scheduled job {} to run daily at {}:{}", jobClass.getSimpleName(), hour, minute);
    }

    public void scheduler(Class<? extends Job> jobClass, int minute) throws SchedulerException {
        JobDetail jobDetail = commonUtils.getJobDetail(jobClass);
        Trigger triggerDetail = commonUtils.getTriggerByCronExpression(jobClass, minute);
        scheduler.scheduleJob(jobDetail, triggerDetail);
        log.info("Scheduled job {} to run every {} minutes", jobClass.getSimpleName(), minute);
    }

    @PreDestroy
    public void stopSchedule() throws SchedulerException {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.info("Quartz Scheduler stopped");
        }
    }
}
