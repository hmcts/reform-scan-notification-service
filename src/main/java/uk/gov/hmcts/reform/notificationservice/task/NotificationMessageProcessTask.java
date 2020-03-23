package uk.gov.hmcts.reform.notificationservice.task;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.service.NotificationMessageProcessor;

@Service
@ConditionalOnProperty(value = "scheduling.task.notifications-consume.enabled", matchIfMissing = true)
public class NotificationMessageProcessTask {

    private static final Logger log = LoggerFactory.getLogger(NotificationMessageProcessTask.class);

    private final NotificationMessageProcessor notificationMessageProcessor;
    private static final String TASK_NAME = "notification consume task";

    public NotificationMessageProcessTask(
        NotificationMessageProcessor notificationMessageProcessor
    ) {
        this.notificationMessageProcessor = notificationMessageProcessor;
    }

    @Scheduled(fixedDelayString = "${scheduling.task.notifications-consume.delay}")
    public void consumeMessages() {
        log.info("Start {}", TASK_NAME);

        try {
            boolean queueMayHaveMessages;

            do {
                queueMayHaveMessages = notificationMessageProcessor.processNextMessage();
            } while (queueMayHaveMessages);

            log.info("Finish {}", TASK_NAME);
        } catch (InterruptedException exception) {
            logTaskError(exception);
            Thread.currentThread().interrupt();
        } catch (ServiceBusException exception) {
            logTaskError(exception);
        }
    }

    private void logTaskError(Exception exception) {
        log.error("Error occurred during {} task", TASK_NAME, exception);
    }
}
