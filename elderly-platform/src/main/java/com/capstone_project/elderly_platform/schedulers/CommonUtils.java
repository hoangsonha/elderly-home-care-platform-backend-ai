package com.capstone_project.elderly_platform.schedulers;

import org.quartz.*;
import org.springframework.stereotype.Service;

@Service
public class CommonUtils {

    /**
     * Create JobDetail for a job class
     * 
     * @param jobClass Job class that implements Job interface
     * @return JobDetail
     */
    public JobDetail getJobDetail(Class<? extends Job> jobClass) {
        return JobBuilder.newJob(jobClass)
                .withIdentity(jobClass.getSimpleName(), "grp1")
                .storeDurably(false)
                .build();
    }

    /**
     * Create Trigger for daily schedule at specific hour and minute
     * 
     * @param jobClass Job class
     * @param hour Hour (0-23)
     * @param minute Minute (0-59)
     * @return Trigger
     */
    public Trigger getTriggerByCronExpression(Class<? extends Job> jobClass, int hour, int minute) {
        return TriggerBuilder
                .newTrigger()
                .withIdentity(jobClass.getSimpleName() + "_trigger", "grp1")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(hour, minute))
                .build();
    }

    /**
     * Create Trigger for interval schedule (every N minutes)
     * 
     * @param jobClass Job class
     * @param minute Interval in minutes
     * @return Trigger
     */
    public Trigger getTriggerByCronExpression(Class<? extends Job> jobClass, int minute) {
        return TriggerBuilder
                .newTrigger()
                .withIdentity(jobClass.getSimpleName() + "_trigger", "grp1")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(minute)
                        .repeatForever())
                .build();
    }

}
