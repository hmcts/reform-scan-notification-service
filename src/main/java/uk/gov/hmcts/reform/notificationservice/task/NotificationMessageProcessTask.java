package uk.gov.hmcts.reform.notificationservice.task;

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

    public NotificationMessageProcessTask(
        NotificationMessageProcessor notificationMessageProcessor
    ) {
        this.notificationMessageProcessor = notificationMessageProcessor;
    }

    @Scheduled(fixedDelayString = "${scheduling.task.notifications-consume.delay}")
    public void consumeMessages() {
        log.info("Started the job consuming notification messages");

        try {
            boolean queueMayHaveMessages;

            do {
                queueMayHaveMessages = notificationMessageProcessor.processNextMessage();
            } while (queueMayHaveMessages);

            log.info("Finished the job consuming notification messages");
        } catch (InterruptedException exception) {
            logTaskError(exception);
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            logTaskError(exception);
        }
    }

    private void logTaskError(Exception exception) {
        log.error("An error occurred when running the 'consume notification messages' task", exception);
    }
}
