package uk.gov.hmcts.reform.notificationservice.task;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.notificationservice.service.NotificationService;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class PendingNotificationsTask {

    private static final Logger log = getLogger(PendingNotificationsTask.class);
    private static final String TASK_NAME = "pending-notifications";

    private final NotificationService notificationService;

    public PendingNotificationsTask(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void run() {
        log.info("Started {} task", TASK_NAME);

        notificationService.processPendingNotifications();

        log.info("Finished {} task", TASK_NAME);
    }
}
