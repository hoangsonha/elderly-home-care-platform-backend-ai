package com.capstone_project.elderly_platform.configurations;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * Quartz Scheduler Configuration
 * Configures Quartz to work with Spring dependency injection
 */
@Configuration
public class QuartzConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Create JobFactory that allows Spring to inject dependencies into Quartz Jobs
     */
    @Bean
    public JobFactory jobFactory() {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    /**
     * Create and configure SchedulerFactoryBean
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() throws SchedulerException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobFactory(jobFactory());
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setOverwriteExistingJobs(true);
        return factory;
    }

    /**
     * Create Scheduler bean
     */
    @Bean
    public Scheduler scheduler() throws SchedulerException {
        return schedulerFactoryBean().getScheduler();
    }
}
