package com.capstone_project.elderly_platform.schedulers;

import org.quartz.*;
import org.springframework.stereotype.Service;

@Service
public class CommonUtils {

    public JobDetail getJobDetail(Class<? extends Job> jobClass) {
        return JobBuilder.newJob(jobClass)
                .withIdentity(jobClass.getSimpleName(), "grp1")
                .storeDurably(false)
                .build();
    }

    public Trigger getTriggerByCronExpression(Class<? extends Job> jobClass, int hour, int minute) {
        return TriggerBuilder
                .newTrigger()
                .withIdentity(jobClass.getSimpleName() + "_trigger", "grp1")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(hour, minute))
                .build();
    }

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
