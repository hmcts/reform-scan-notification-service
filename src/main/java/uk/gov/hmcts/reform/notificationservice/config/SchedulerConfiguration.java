package uk.gov.hmcts.reform.notificationservice.config;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

//TODO: FACT-2026 - whole class can go
@Configuration
@EnableScheduling
public class SchedulerConfiguration implements SchedulingConfigurer {

    private static final int POOL_SIZE = 10;
    private static AtomicInteger errorCount = new AtomicInteger(0);
    private static final Logger log = getLogger(SchedulerConfiguration.class);

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(notificationTaskScheduler());
    }

    @Bean
    public TaskScheduler notificationTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(POOL_SIZE);
        scheduler.setThreadNamePrefix("NOTIFICATION-");
        scheduler.setErrorHandler(t -> {
            log.error("Unhandled exception during task. {}: {}", t.getClass(), t.getMessage(), t);
            errorCount.incrementAndGet();
        });
        scheduler.initialize();

        return scheduler;
    }
}
