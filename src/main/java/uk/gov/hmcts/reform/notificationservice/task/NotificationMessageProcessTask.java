package uk.gov.hmcts.reform.notificationservice.task;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

//TODO: FACT-2026 - whole class can go
@Service
@ConditionalOnProperty(value = "scheduling.task.notifications-consume.enabled", matchIfMissing = true)
@ConditionalOnExpression("!${jms.enabled}")
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

    @Scheduled(fixedDelayString = "${scheduling.task.notifications-consume.check.delay}")
    public void checkServiceBusProcessorClient() {
        if (!serviceBusProcessorClient.isRunning()) {
            log.error("Notification queue consume listener is NOT running!!!");
        } else {
            log.info("Notification queue consume listener is working.");
        }
    }
}
