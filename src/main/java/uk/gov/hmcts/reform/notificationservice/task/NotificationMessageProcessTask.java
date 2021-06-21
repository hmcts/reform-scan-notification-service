package uk.gov.hmcts.reform.notificationservice.task;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@ConditionalOnProperty(value = "scheduling.task.notifications-consume.enabled", matchIfMissing = true)
public class NotificationMessageProcessTask {

    private final ServiceBusProcessorClient serviceBusProcessorClient;
    private static final Logger log = LoggerFactory.getLogger(NotificationMessageProcessTask.class);

    public NotificationMessageProcessTask(
        ServiceBusProcessorClient serviceBusProcessorClient
    ) {
        this.serviceBusProcessorClient = serviceBusProcessorClient;
    }

    @PostConstruct
    void startProcessor() {
        serviceBusProcessorClient.start();
    }

    @Scheduled(fixedDelayString = "${scheduling.task.notifications-check.delay}")
    public void checkServiceBusProcessorClient() {
        if (!serviceBusProcessorClient.isRunning()) {
            log.error("Notification queue consume listener is NOT running!!!");
        } else {
            log.info("Notification queue consume listener is working.");
        }
    }
}
